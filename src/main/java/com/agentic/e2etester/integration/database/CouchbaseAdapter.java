package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Couchbase integration adapter for database connectivity and operations.
 * Provides document validation, comparison utilities, and query execution capabilities.
 */
@Component
public class CouchbaseAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CouchbaseAdapter.class);
    
    private final Cluster cluster;
    private final DocumentValidator documentValidator;
    private final QueryExecutor queryExecutor;
    
    @Autowired
    public CouchbaseAdapter(Cluster cluster, DocumentValidator documentValidator, QueryExecutor queryExecutor) {
        this.cluster = cluster;
        this.documentValidator = documentValidator;
        this.queryExecutor = queryExecutor;
    }
    
    /**
     * Retrieves a document by ID from the specified bucket and collection.
     */
    public Optional<JsonObject> getDocument(String bucketName, String collectionName, String documentId) {
        try {
            logger.debug("Retrieving document {} from {}.{}", documentId, bucketName, collectionName);
            
            Bucket bucket = cluster.bucket(bucketName);
            Collection collection = bucket.collection(collectionName);
            
            GetResult result = collection.get(documentId);
            JsonObject document = result.contentAsObject();
            
            logger.debug("Successfully retrieved document {}", documentId);
            return Optional.of(document);
            
        } catch (Exception e) {
            logger.warn("Failed to retrieve document {} from {}.{}: {}", 
                       documentId, bucketName, collectionName, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Stores a document in the specified bucket and collection.
     */
    public CompletableFuture<DocumentOperationResult> storeDocument(String bucketName, String collectionName, 
                                                                   String documentId, JsonObject document) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Storing document {} in {}.{}", documentId, bucketName, collectionName);
                
                Bucket bucket = cluster.bucket(bucketName);
                Collection collection = bucket.collection(collectionName);
                
                MutationResult result = collection.upsert(documentId, document);
                
                logger.debug("Successfully stored document {} with CAS {}", documentId, result.cas());
                return DocumentOperationResult.success(documentId, result.cas());
                
            } catch (Exception e) {
                logger.error("Failed to store document {} in {}.{}: {}", 
                           documentId, bucketName, collectionName, e.getMessage());
                return DocumentOperationResult.failure(documentId, e.getMessage());
            }
        });
    }
    
    /**
     * Validates a document against expected structure and values.
     */
    public DocumentValidationResult validateDocument(String bucketName, String collectionName, 
                                                   String documentId, Map<String, Object> expectedValues) {
        Optional<JsonObject> documentOpt = getDocument(bucketName, collectionName, documentId);
        
        if (documentOpt.isEmpty()) {
            return DocumentValidationResult.failure(documentId, "Document not found");
        }
        
        JsonObject document = documentOpt.get();
        return documentValidator.validate(document, expectedValues);
    }
    
    /**
     * Compares two documents for equality or specific field differences.
     */
    public DocumentComparisonResult compareDocuments(String bucketName, String collectionName,
                                                   String documentId1, String documentId2) {
        Optional<JsonObject> doc1 = getDocument(bucketName, collectionName, documentId1);
        Optional<JsonObject> doc2 = getDocument(bucketName, collectionName, documentId2);
        
        if (doc1.isEmpty() || doc2.isEmpty()) {
            return DocumentComparisonResult.failure("One or both documents not found");
        }
        
        return documentValidator.compare(doc1.get(), doc2.get());
    }
    
    /**
     * Executes a N1QL query and returns the results.
     */
    public CompletableFuture<QueryExecutionResult> executeQuery(String query, Map<String, Object> parameters) {
        return queryExecutor.executeQuery(query, parameters);
    }
    
    /**
     * Verifies query results against expected outcomes.
     */
    public QueryVerificationResult verifyQueryResults(String query, Map<String, Object> parameters,
                                                    QueryExpectation expectation) {
        try {
            QueryExecutionResult result = executeQuery(query, parameters).get();
            
            if (!result.isSuccess()) {
                return QueryVerificationResult.failure("Query execution failed: " + result.getErrorMessage());
            }
            
            return queryExecutor.verifyResults(result.getRows(), expectation);
            
        } catch (Exception e) {
            logger.error("Failed to verify query results: {}", e.getMessage());
            return QueryVerificationResult.failure("Query verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a document exists in the specified location.
     */
    public boolean documentExists(String bucketName, String collectionName, String documentId) {
        try {
            Bucket bucket = cluster.bucket(bucketName);
            Collection collection = bucket.collection(collectionName);
            
            collection.exists(documentId);
            return true;
            
        } catch (Exception e) {
            logger.debug("Document {} does not exist in {}.{}: {}", 
                        documentId, bucketName, collectionName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Deletes a document from the specified location.
     */
    public CompletableFuture<DocumentOperationResult> deleteDocument(String bucketName, String collectionName, 
                                                                   String documentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Deleting document {} from {}.{}", documentId, bucketName, collectionName);
                
                Bucket bucket = cluster.bucket(bucketName);
                Collection collection = bucket.collection(collectionName);
                
                MutationResult result = collection.remove(documentId);
                
                logger.debug("Successfully deleted document {} with CAS {}", documentId, result.cas());
                return DocumentOperationResult.success(documentId, result.cas());
                
            } catch (Exception e) {
                logger.error("Failed to delete document {} from {}.{}: {}", 
                           documentId, bucketName, collectionName, e.getMessage());
                return DocumentOperationResult.failure(documentId, e.getMessage());
            }
        });
    }
    
    /**
     * Performs a health check on the Couchbase cluster.
     */
    public DatabaseHealthResult checkHealth() {
        try {
            // Ping the cluster to check connectivity
            cluster.ping();
            
            // Try to get bucket information
            cluster.buckets().getAllBuckets();
            
            return DatabaseHealthResult.healthy();
            
        } catch (Exception e) {
            logger.error("Couchbase health check failed: {}", e.getMessage());
            return DatabaseHealthResult.unhealthy(e.getMessage());
        }
    }
}