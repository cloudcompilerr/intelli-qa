package com.agentic.e2etester.cicd.gitlab;

import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.model.StepResult;
import com.agentic.e2etester.model.TestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Formats test results for GitLab CI/CD consumption.
 */
@Component
public class GitLabReportFormatter {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Formats test results as JUnit XML for GitLab test result publishing.
     */
    public String formatAsJUnitXml(TestResult testResult) {
        StringBuilder xml = new StringBuilder();
        
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite ");
        xml.append("name=\"").append(escapeXml(testResult.getTestId())).append("\" ");
        xml.append("tests=\"").append(testResult.getStepResults().size()).append("\" ");
        xml.append("failures=\"").append(countFailures(testResult)).append("\" ");
        xml.append("errors=\"").append(countErrors(testResult)).append("\" ");
        xml.append("time=\"").append(testResult.getExecutionTime().toSeconds()).append("\" ");
        xml.append("timestamp=\"").append(testResult.getStartTime().toString()).append("\"");
        xml.append(">\n");
        
        // Add GitLab-specific properties
        xml.append("  <properties>\n");
        xml.append("    <property name=\"gitlab.environment\" value=\"").append(escapeXml(getEnvironment(testResult))).append("\"/>\n");
        xml.append("    <property name=\"gitlab.version\" value=\"").append(escapeXml(getVersion(testResult))).append("\"/>\n");
        xml.append("    <property name=\"gitlab.pipeline_id\" value=\"").append(escapeXml(getPipelineId(testResult))).append("\"/>\n");
        xml.append("  </properties>\n");
        
        // Add test cases
        for (StepResult stepResult : testResult.getStepResults()) {
            xml.append("  <testcase ");
            xml.append("classname=\"AgenticE2ETest\" ");
            xml.append("name=\"").append(escapeXml(stepResult.getStepId())).append("\" ");
            xml.append("time=\"").append(stepResult.getExecutionTime().toSeconds()).append("\"");
            
            if (stepResult.getStatus() == TestStatus.FAILED) {
                xml.append(">\n");
                xml.append("    <failure message=\"").append(escapeXml(stepResult.getErrorMessage())).append("\">");
                xml.append("<![CDATA[").append(stepResult.getDetails()).append("]]>");
                xml.append("</failure>\n");
                xml.append("  </testcase>\n");
            } else if (stepResult.getStatus() == TestStatus.SKIPPED) {
                xml.append(">\n");
                xml.append("    <skipped/>\n");
                xml.append("  </testcase>\n");
            } else {
                xml.append("/>\n");
            }
        }
        
        xml.append("</testsuite>\n");
        
        return xml.toString();
    }
    
    /**
     * Formats test results as GitLab-specific report format.
     */
    public String formatAsGitLabReport(TestResult testResult) {
        try {
            Map<String, Object> report = new HashMap<>();
            
            // GitLab test report format
            report.put("version", "1.0");
            report.put("test_suite", testResult.getTestId());
            report.put("status", mapStatusToGitLab(testResult.getStatus()));
            report.put("duration", testResult.getExecutionTime().toSeconds());
            report.put("start_time", testResult.getStartTime().toString());
            report.put("end_time", testResult.getEndTime().toString());
            
            // Test summary
            Map<String, Object> summary = new HashMap<>();
            summary.put("total", testResult.getStepResults().size());
            summary.put("passed", countPassed(testResult));
            summary.put("failed", countFailures(testResult));
            summary.put("errors", countErrors(testResult));
            summary.put("skipped", countSkipped(testResult));
            report.put("summary", summary);
            
            // Test cases
            List<Map<String, Object>> testCases = new ArrayList<>();
            for (StepResult stepResult : testResult.getStepResults()) {
                Map<String, Object> testCase = new HashMap<>();
                testCase.put("name", stepResult.getStepId());
                testCase.put("status", mapStatusToGitLab(stepResult.getStatus()));
                testCase.put("duration", stepResult.getExecutionTime().toSeconds());
                
                if (stepResult.getErrorMessage() != null) {
                    testCase.put("failure_message", stepResult.getErrorMessage());
                }
                
                if (stepResult.getDetails() != null) {
                    testCase.put("details", stepResult.getDetails());
                }
                
                testCases.add(testCase);
            }
            report.put("test_cases", testCases);
            
            // Metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("environment", getEnvironment(testResult));
            metadata.put("version", getVersion(testResult));
            metadata.put("platform", "gitlab");
            if (testResult.getMetadata() != null) {
                metadata.putAll(testResult.getMetadata());
            }
            report.put("metadata", metadata);
            
            return objectMapper.writeValueAsString(report);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to format GitLab report", e);
        }
    }
    
    /**
     * Formats test results as GitLab merge request comment.
     */
    public String formatAsMergeRequestComment(TestResult testResult) {
        StringBuilder comment = new StringBuilder();
        
        comment.append("## 🤖 Agentic E2E Test Results\n\n");
        
        // Status badge
        String statusEmoji = testResult.getStatus() == TestStatus.PASSED ? "✅" : "❌";
        comment.append("**Status:** ").append(statusEmoji).append(" ").append(testResult.getStatus()).append("\n");
        comment.append("**Duration:** ").append(testResult.getExecutionTime().toSeconds()).append(" seconds\n");
        comment.append("**Test ID:** `").append(testResult.getTestId()).append("`\n\n");
        
        // Summary table
        comment.append("### Summary\n\n");
        comment.append("| Metric | Count |\n");
        comment.append("|--------|-------|\n");
        comment.append("| Total Steps | ").append(testResult.getStepResults().size()).append(" |\n");
        comment.append("| Passed | ").append(countPassed(testResult)).append(" |\n");
        comment.append("| Failed | ").append(countFailures(testResult)).append(" |\n");
        comment.append("| Errors | ").append(countErrors(testResult)).append(" |\n");
        comment.append("| Skipped | ").append(countSkipped(testResult)).append(" |\n\n");
        
        // Failed steps details
        long failedCount = countFailures(testResult);
        if (failedCount > 0) {
            comment.append("### ❌ Failed Steps\n\n");
            for (StepResult stepResult : testResult.getStepResults()) {
                if (stepResult.getStatus() == TestStatus.FAILED) {
                    comment.append("- **").append(stepResult.getStepId()).append("**\n");
                    if (stepResult.getErrorMessage() != null) {
                        comment.append("  - Error: `").append(stepResult.getErrorMessage()).append("`\n");
                    }
                }
            }
            comment.append("\n");
        }
        
        // Environment info
        comment.append("### Environment\n\n");
        comment.append("- **Environment:** ").append(getEnvironment(testResult)).append("\n");
        comment.append("- **Version:** ").append(getVersion(testResult)).append("\n");
        comment.append("- **Started:** ").append(testResult.getStartTime()).append("\n");
        comment.append("- **Completed:** ").append(testResult.getEndTime()).append("\n");
        
        return comment.toString();
    }
    
    private String mapStatusToGitLab(TestStatus status) {
        switch (status) {
            case PASSED: return "passed";
            case FAILED: return "failed";
            case SKIPPED: return "skipped";
            case RUNNING: return "running";
            default: return "unknown";
        }
    }
    
    private long countFailures(TestResult testResult) {
        return testResult.getStepResults().stream()
                .mapToLong(step -> step.getStatus() == TestStatus.FAILED ? 1 : 0)
                .sum();
    }
    
    private long countErrors(TestResult testResult) {
        return testResult.getStepResults().stream()
                .mapToLong(step -> step.getErrorMessage() != null ? 1 : 0)
                .sum();
    }
    
    private long countPassed(TestResult testResult) {
        return testResult.getStepResults().stream()
                .mapToLong(step -> step.getStatus() == TestStatus.PASSED ? 1 : 0)
                .sum();
    }
    
    private long countSkipped(TestResult testResult) {
        return testResult.getStepResults().stream()
                .mapToLong(step -> step.getStatus() == TestStatus.SKIPPED ? 1 : 0)
                .sum();
    }
    
    private String getEnvironment(TestResult testResult) {
        if (testResult.getMetadata() != null) {
            Object env = testResult.getMetadata().get("environment");
            return env != null ? env.toString() : "unknown";
        }
        return "unknown";
    }
    
    private String getVersion(TestResult testResult) {
        if (testResult.getMetadata() != null) {
            Object version = testResult.getMetadata().get("version");
            return version != null ? version.toString() : "unknown";
        }
        return "unknown";
    }
    
    private String getPipelineId(TestResult testResult) {
        if (testResult.getMetadata() != null) {
            Object pipelineId = testResult.getMetadata().get("pipelineId");
            return pipelineId != null ? pipelineId.toString() : "unknown";
        }
        return "unknown";
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}