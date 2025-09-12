package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.Cluster;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;

/**
 * Configuration for Couchbase integration components.
 */
@Configuration
@ConditionalOnProperty(name = "spring.couchbase.connection-string")
public class CouchbaseConfiguration extends AbstractCouchbaseConfiguration {
    
    @Override
    public String getConnectionString() {
        return "couchbase://localhost";
    }
    
    @Override
    public String getUserName() {
        return "Administrator";
    }
    
    @Override
    public String getPassword() {
        return "password";
    }
    
    @Override
    public String getBucketName() {
        return "test-bucket";
    }
    
    @Bean
    public DocumentValidator documentValidator() {
        return new DocumentValidator();
    }
    
    @Bean
    public QueryExecutor queryExecutor(Cluster cluster) {
        return new QueryExecutor(cluster);
    }
    
    @Bean
    public CouchbaseAdapter couchbaseAdapter(Cluster cluster, DocumentValidator documentValidator, 
                                           QueryExecutor queryExecutor) {
        return new CouchbaseAdapter(cluster, documentValidator, queryExecutor);
    }
}