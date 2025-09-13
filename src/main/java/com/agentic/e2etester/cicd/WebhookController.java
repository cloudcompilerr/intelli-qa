package com.agentic.e2etester.cicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for handling CI/CD webhook events.
 */
@RestController
@RequestMapping("/api/cicd")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    private final DeploymentEventListener deploymentEventListener;
    private final CiCdConfiguration ciCdConfiguration;
    private final ObjectMapper objectMapper;
    
    public WebhookController(DeploymentEventListener deploymentEventListener,
                           CiCdConfiguration ciCdConfiguration,
                           ObjectMapper objectMapper) {
        this.deploymentEventListener = deploymentEventListener;
        this.ciCdConfiguration = ciCdConfiguration;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generic webhook endpoint for deployment events.
     */
    @PostMapping("/webhook/deployment")
    public ResponseEntity<Map<String, Object>> handleDeploymentWebhook(
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Received deployment webhook from IP: {}", request.getRemoteAddr());
            
            // Validate webhook configuration
            if (!ciCdConfiguration.getWebhook().isEnabled()) {
                logger.warn("Webhook is disabled, rejecting request");
                response.put("error", "Webhook is disabled");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            // Validate IP if configured
            if (!isAllowedIp(request.getRemoteAddr())) {
                logger.warn("Request from unauthorized IP: {}", request.getRemoteAddr());
                response.put("error", "Unauthorized IP address");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Validate signature if configured
            if (ciCdConfiguration.getWebhook().isValidateSignature()) {
                if (!validateSignature(payload, headers)) {
                    logger.warn("Invalid webhook signature");
                    response.put("error", "Invalid signature");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            }
            
            // Parse deployment event
            DeploymentEvent deploymentEvent = parseDeploymentEvent(payload, headers);
            
            // Validate event
            if (!deploymentEventListener.validateDeploymentEvent(deploymentEvent)) {
                logger.warn("Invalid deployment event received");
                response.put("error", "Invalid deployment event");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Process event asynchronously
            deploymentEventListener.handleDeploymentEvent(deploymentEvent)
                .exceptionally(throwable -> {
                    logger.error("Failed to process deployment event: {}", deploymentEvent.getEventId(), throwable);
                    return null;
                });
            
            response.put("status", "accepted");
            response.put("eventId", deploymentEvent.getEventId());
            response.put("message", "Deployment event received and queued for processing");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to process webhook request", e);
            response.put("error", "Internal server error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Jenkins-specific webhook endpoint.
     */
    @PostMapping("/webhook/jenkins")
    public ResponseEntity<Map<String, Object>> handleJenkinsWebhook(
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request) {
        
        try {
            logger.info("Received Jenkins webhook");
            
            DeploymentEvent deploymentEvent = parseJenkinsEvent(payload, headers);
            return handleDeploymentWebhook(objectMapper.writeValueAsString(deploymentEvent), headers, request);
            
        } catch (Exception e) {
            logger.error("Failed to process Jenkins webhook", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to process Jenkins webhook");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * GitLab-specific webhook endpoint.
     */
    @PostMapping("/webhook/gitlab")
    public ResponseEntity<Map<String, Object>> handleGitLabWebhook(
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request) {
        
        try {
            logger.info("Received GitLab webhook");
            
            DeploymentEvent deploymentEvent = parseGitLabEvent(payload, headers);
            return handleDeploymentWebhook(objectMapper.writeValueAsString(deploymentEvent), headers, request);
            
        } catch (Exception e) {
            logger.error("Failed to process GitLab webhook", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to process GitLab webhook");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Health check endpoint for webhook service.
     */
    @GetMapping("/webhook/health")
    public ResponseEntity<Map<String, Object>> webhookHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("webhook_enabled", ciCdConfiguration.getWebhook().isEnabled());
        health.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(health);
    }
    
    private boolean isAllowedIp(String remoteAddr) {
        if (ciCdConfiguration.getWebhook().getAllowedIps() == null || 
            ciCdConfiguration.getWebhook().getAllowedIps().isEmpty()) {
            return true; // No IP restrictions configured
        }
        
        return ciCdConfiguration.getWebhook().getAllowedIps().contains(remoteAddr);
    }
    
    private boolean validateSignature(String payload, Map<String, String> headers) {
        String secret = ciCdConfiguration.getWebhook().getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            return true; // No secret configured, skip validation
        }
        
        String signature = headers.get("x-hub-signature-256");
        if (signature == null) {
            signature = headers.get("x-gitlab-token");
        }
        
        if (signature == null) {
            return false;
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(hash);
            
            return expectedSignature.equals(signature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Failed to validate webhook signature", e);
            return false;
        }
    }
    
    private DeploymentEvent parseDeploymentEvent(String payload, Map<String, String> headers) throws Exception {
        // Generic parsing - try to extract common fields
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId(generateEventId());
        event.setTimestamp(Instant.now());
        event.setPlatform(detectPlatform(headers, data));
        
        // Extract common fields
        event.setEnvironment((String) data.get("environment"));
        event.setVersion((String) data.get("version"));
        event.setBranch((String) data.get("branch"));
        event.setCommitHash((String) data.get("commit"));
        event.setProjectId((String) data.get("project_id"));
        
        // Set event type based on data
        event.setEventType(determineEventType(data));
        
        // Store original payload as metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("original_payload", data);
        metadata.put("headers", headers);
        event.setMetadata(metadata);
        
        return event;
    }
    
    private DeploymentEvent parseJenkinsEvent(String payload, Map<String, String> headers) throws Exception {
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId(generateEventId());
        event.setTimestamp(Instant.now());
        event.setPlatform("jenkins");
        
        // Extract Jenkins-specific fields
        event.setProjectId((String) data.get("job_name"));
        event.setEnvironment((String) data.get("environment"));
        event.setVersion((String) data.get("build_number"));
        event.setBranch((String) data.get("branch"));
        
        // Determine event type from Jenkins build status
        String buildStatus = (String) data.get("build_status");
        if ("SUCCESS".equals(buildStatus)) {
            event.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
        } else if ("FAILURE".equals(buildStatus)) {
            event.setEventType(DeploymentEventType.DEPLOYMENT_FAILED);
        } else {
            event.setEventType(DeploymentEventType.DEPLOYMENT_STARTED);
        }
        
        // Store Jenkins metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("BUILD_URL", data.get("build_url"));
        metadata.put("JOB_NAME", data.get("job_name"));
        metadata.put("BUILD_NUMBER", data.get("build_number"));
        metadata.put("original_payload", data);
        event.setMetadata(metadata);
        
        return event;
    }
    
    private DeploymentEvent parseGitLabEvent(String payload, Map<String, String> headers) throws Exception {
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId(generateEventId());
        event.setTimestamp(Instant.now());
        event.setPlatform("gitlab");
        
        // Extract GitLab-specific fields
        event.setProjectId(String.valueOf(data.get("project_id")));
        event.setEnvironment((String) data.get("environment"));
        event.setBranch((String) data.get("ref"));
        
        // Extract from commit or pipeline data
        Map<String, Object> commit = (Map<String, Object>) data.get("commit");
        if (commit != null) {
            event.setCommitHash((String) commit.get("id"));
        }
        
        // Determine event type from GitLab event
        String eventType = headers.get("x-gitlab-event");
        if ("Pipeline Hook".equals(eventType)) {
            String status = (String) data.get("object_attributes.status");
            if ("success".equals(status)) {
                event.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
            } else if ("failed".equals(status)) {
                event.setEventType(DeploymentEventType.DEPLOYMENT_FAILED);
            } else {
                event.setEventType(DeploymentEventType.DEPLOYMENT_STARTED);
            }
        } else {
            event.setEventType(DeploymentEventType.POST_DEPLOYMENT);
        }
        
        // Store GitLab metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("CI_PIPELINE_ID", data.get("pipeline_id"));
        metadata.put("CI_PROJECT_ID", data.get("project_id"));
        metadata.put("CI_MERGE_REQUEST_IID", data.get("merge_request_iid"));
        metadata.put("original_payload", data);
        event.setMetadata(metadata);
        
        return event;
    }
    
    private String detectPlatform(Map<String, String> headers, Map<String, Object> data) {
        if (headers.containsKey("x-gitlab-event")) {
            return "gitlab";
        } else if (headers.containsKey("x-jenkins-event")) {
            return "jenkins";
        } else if (headers.containsKey("x-github-event")) {
            return "github";
        } else {
            return "unknown";
        }
    }
    
    private DeploymentEventType determineEventType(Map<String, Object> data) {
        String status = (String) data.get("status");
        if ("completed".equals(status) || "success".equals(status)) {
            return DeploymentEventType.DEPLOYMENT_COMPLETED;
        } else if ("failed".equals(status) || "failure".equals(status)) {
            return DeploymentEventType.DEPLOYMENT_FAILED;
        } else if ("started".equals(status) || "running".equals(status)) {
            return DeploymentEventType.DEPLOYMENT_STARTED;
        } else {
            return DeploymentEventType.POST_DEPLOYMENT;
        }
    }
    
    private String generateEventId() {
        return "event-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}