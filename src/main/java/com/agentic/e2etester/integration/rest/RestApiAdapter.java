package com.agentic.e2etester.integration.rest;

import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.InteractionType;
import com.agentic.e2etester.model.InteractionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API integration adapter for microservice communication.
 * Handles HTTP interactions with dynamic endpoint discovery and authentication.
 */
@Component
public class RestApiAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(RestApiAdapter.class);
    
    private final WebClient webClient;
    private final EndpointDiscoveryService endpointDiscoveryService;
    private final AuthenticationHandler authenticationHandler;
    
    public RestApiAdapter(WebClient.Builder webClientBuilder,
                         EndpointDiscoveryService endpointDiscoveryService,
                         AuthenticationHandler authenticationHandler) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        this.endpointDiscoveryService = endpointDiscoveryService;
        this.authenticationHandler = authenticationHandler;
    }
    
    /**
     * Execute a GET request to a microservice endpoint
     */
    public CompletableFuture<ServiceInteraction> get(String serviceId, String endpoint, 
                                                   String correlationId, Map<String, String> headers) {
        return executeRequest(serviceId, endpoint, HttpMethod.GET, null, correlationId, headers);
    }
    
    /**
     * Execute a POST request to a microservice endpoint
     */
    public CompletableFuture<ServiceInteraction> post(String serviceId, String endpoint, 
                                                    Object requestBody, String correlationId, 
                                                    Map<String, String> headers) {
        return executeRequest(serviceId, endpoint, HttpMethod.POST, requestBody, correlationId, headers);
    }
    
    /**
     * Execute a PUT request to a microservice endpoint
     */
    public CompletableFuture<ServiceInteraction> put(String serviceId, String endpoint, 
                                                   Object requestBody, String correlationId, 
                                                   Map<String, String> headers) {
        return executeRequest(serviceId, endpoint, HttpMethod.PUT, requestBody, correlationId, headers);
    }
    
    /**
     * Execute a DELETE request to a microservice endpoint
     */
    public CompletableFuture<ServiceInteraction> delete(String serviceId, String endpoint, 
                                                      String correlationId, Map<String, String> headers) {
        return executeRequest(serviceId, endpoint, HttpMethod.DELETE, null, correlationId, headers);
    }
    
    /**
     * Execute a generic HTTP request to a microservice endpoint
     */
    public CompletableFuture<ServiceInteraction> executeRequest(String serviceId, String endpoint, 
                                                              HttpMethod method, Object requestBody, 
                                                              String correlationId, 
                                                              Map<String, String> headers) {
        
        ServiceInteraction interaction = new ServiceInteraction(serviceId, InteractionType.HTTP_REQUEST, InteractionStatus.IN_PROGRESS);
        interaction.setCorrelationId(correlationId);
        interaction.setRequest(requestBody);
        
        Instant startTime = Instant.now();
        
        return endpointDiscoveryService.resolveEndpoint(serviceId, endpoint)
                .thenCompose(fullUrl -> {
                    logger.debug("Executing {} request to {} for service {}", method, fullUrl, serviceId);
                    
                    WebClient.RequestBodySpec requestSpec = webClient
                            .method(method)
                            .uri(fullUrl)
                            .headers(httpHeaders -> {
                                // Add correlation ID header
                                httpHeaders.add("X-Correlation-ID", correlationId);
                                
                                // Add custom headers
                                if (headers != null) {
                                    headers.forEach(httpHeaders::add);
                                }
                                
                                // Set content type for requests with body
                                if (requestBody != null) {
                                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                                }
                            });
                    
                    // Apply authentication
                    requestSpec = authenticationHandler.applyAuthentication(requestSpec, serviceId);
                    
                    // Add request body if present
                    Mono<String> responseMono;
                    if (requestBody != null) {
                        responseMono = requestSpec
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToMono(String.class);
                    } else {
                        responseMono = requestSpec
                                .retrieve()
                                .bodyToMono(String.class);
                    }
                    
                    // Execute with retry logic
                    return responseMono
                            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                    .filter(throwable -> !(throwable instanceof WebClientResponseException) ||
                                            ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()))
                            .doOnSuccess(response -> {
                                Duration responseTime = Duration.between(startTime, Instant.now());
                                interaction.setResponse(response);
                                interaction.setResponseTime(responseTime);
                                interaction.setStatus(InteractionStatus.SUCCESS);
                                logger.debug("Request completed successfully in {}ms for service {}", 
                                           responseTime.toMillis(), serviceId);
                            })
                            .doOnError(error -> {
                                Duration responseTime = Duration.between(startTime, Instant.now());
                                interaction.setResponseTime(responseTime);
                                interaction.setStatus(InteractionStatus.FAILURE);
                                interaction.setErrorMessage(error.getMessage());
                                
                                if (error instanceof WebClientResponseException webClientError) {
                                    interaction.setResponse(webClientError.getResponseBodyAsString());
                                    logger.error("Request failed with status {} for service {}: {}", 
                                               webClientError.getStatusCode(), serviceId, error.getMessage());
                                } else {
                                    logger.error("Request failed for service {}: {}", serviceId, error.getMessage());
                                }
                            })
                            .toFuture()
                            .thenApply(response -> interaction)
                            .exceptionally(throwable -> {
                                // Ensure interaction is properly set even in exceptional cases
                                if (interaction.getStatus() == InteractionStatus.IN_PROGRESS) {
                                    Duration responseTime = Duration.between(startTime, Instant.now());
                                    interaction.setResponseTime(responseTime);
                                    interaction.setStatus(InteractionStatus.FAILURE);
                                    interaction.setErrorMessage(throwable.getMessage());
                                }
                                return interaction;
                            });
                })
                .exceptionally(throwable -> {
                    Duration responseTime = Duration.between(startTime, Instant.now());
                    interaction.setResponseTime(responseTime);
                    interaction.setStatus(InteractionStatus.FAILURE);
                    interaction.setErrorMessage("Failed to resolve endpoint: " + throwable.getMessage());
                    logger.error("Failed to resolve endpoint for service {}: {}", serviceId, throwable.getMessage());
                    return interaction;
                });
    }
    
    /**
     * Check if a service endpoint is available
     */
    public CompletableFuture<Boolean> isEndpointAvailable(String serviceId, String endpoint) {
        return endpointDiscoveryService.resolveEndpoint(serviceId, endpoint)
                .thenCompose(fullUrl -> {
                    return webClient
                            .head()
                            .uri(fullUrl)
                            .retrieve()
                            .toBodilessEntity()
                            .map(response -> response.getStatusCode().is2xxSuccessful())
                            .onErrorReturn(false)
                            .toFuture();
                })
                .exceptionally(throwable -> false);
    }
    
    /**
     * Get service information including available endpoints
     */
    public CompletableFuture<ServiceInfo> getServiceInfo(String serviceId) {
        return endpointDiscoveryService.discoverServiceEndpoints(serviceId);
    }
}