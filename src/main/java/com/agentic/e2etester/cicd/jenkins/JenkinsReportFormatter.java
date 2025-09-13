package com.agentic.e2etester.cicd.jenkins;

import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.model.StepResult;
import com.agentic.e2etester.model.TestStatus;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Formats test results for Jenkins consumption.
 */
@Component
public class JenkinsReportFormatter {
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Formats test results as JUnit XML for Jenkins test result publishing.
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
        
        // Add test properties
        xml.append("  <properties>\n");
        xml.append("    <property name=\"environment\" value=\"").append(escapeXml(getEnvironment(testResult))).append("\"/>\n");
        xml.append("    <property name=\"version\" value=\"").append(escapeXml(getVersion(testResult))).append("\"/>\n");
        xml.append("    <property name=\"platform\" value=\"jenkins\"/>\n");
        xml.append("  </properties>\n");
        
        // Add test cases
        for (StepResult stepResult : testResult.getStepResults()) {
            xml.append("  <testcase ");
            xml.append("classname=\"").append(escapeXml(testResult.getTestId())).append("\" ");
            xml.append("name=\"").append(escapeXml(stepResult.getStepId())).append("\" ");
            xml.append("time=\"").append(stepResult.getExecutionTime().toSeconds()).append("\"");
            
            if (stepResult.getStatus() == TestStatus.FAILED) {
                xml.append(">\n");
                xml.append("    <failure message=\"").append(escapeXml(stepResult.getErrorMessage())).append("\">");
                xml.append(escapeXml(stepResult.getDetails()));
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
     * Formats test results as HTML report for Jenkins artifact publishing.
     */
    public String formatAsHtml(TestResult testResult) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("  <title>Agentic E2E Test Report - ").append(escapeHtml(testResult.getTestId())).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("    .header { background-color: #f5f5f5; padding: 15px; border-radius: 5px; }\n");
        html.append("    .success { color: #28a745; }\n");
        html.append("    .failure { color: #dc3545; }\n");
        html.append("    .warning { color: #ffc107; }\n");
        html.append("    .step { margin: 10px 0; padding: 10px; border-left: 3px solid #ccc; }\n");
        html.append("    .step.success { border-left-color: #28a745; }\n");
        html.append("    .step.failure { border-left-color: #dc3545; }\n");
        html.append("    .metadata { background-color: #f8f9fa; padding: 10px; margin: 10px 0; }\n");
        html.append("  </style>\n");
        html.append("</head>\n<body>\n");
        
        // Header
        html.append("  <div class=\"header\">\n");
        html.append("    <h1>Agentic E2E Test Report</h1>\n");
        html.append("    <p><strong>Test ID:</strong> ").append(escapeHtml(testResult.getTestId())).append("</p>\n");
        html.append("    <p><strong>Status:</strong> <span class=\"").append(testResult.getStatus().name().toLowerCase()).append("\">")
            .append(testResult.getStatus()).append("</span></p>\n");
        html.append("    <p><strong>Duration:</strong> ").append(testResult.getExecutionTime().toSeconds()).append(" seconds</p>\n");
        html.append("    <p><strong>Started:</strong> ").append(testResult.getStartTime()).append("</p>\n");
        html.append("    <p><strong>Completed:</strong> ").append(testResult.getEndTime()).append("</p>\n");
        html.append("  </div>\n");
        
        // Summary
        html.append("  <div class=\"metadata\">\n");
        html.append("    <h2>Test Summary</h2>\n");
        html.append("    <p><strong>Total Steps:</strong> ").append(testResult.getStepResults().size()).append("</p>\n");
        html.append("    <p><strong>Passed:</strong> ").append(countPassed(testResult)).append("</p>\n");
        html.append("    <p><strong>Failed:</strong> ").append(countFailures(testResult)).append("</p>\n");
        html.append("    <p><strong>Errors:</strong> ").append(countErrors(testResult)).append("</p>\n");
        html.append("  </div>\n");
        
        // Step details
        html.append("  <h2>Step Details</h2>\n");
        for (StepResult stepResult : testResult.getStepResults()) {
            String statusClass = stepResult.getStatus() == TestStatus.PASSED ? "success" : "failure";
            html.append("  <div class=\"step ").append(statusClass).append("\">\n");
            html.append("    <h3>").append(escapeHtml(stepResult.getStepId())).append("</h3>\n");
            html.append("    <p><strong>Status:</strong> ").append(stepResult.getStatus()).append("</p>\n");
            html.append("    <p><strong>Duration:</strong> ").append(stepResult.getExecutionTime().toSeconds()).append(" seconds</p>\n");
            
            if (stepResult.getErrorMessage() != null) {
                html.append("    <p><strong>Error:</strong> ").append(escapeHtml(stepResult.getErrorMessage())).append("</p>\n");
            }
            
            if (stepResult.getDetails() != null) {
                html.append("    <p><strong>Details:</strong></p>\n");
                html.append("    <pre>").append(escapeHtml(stepResult.getDetails())).append("</pre>\n");
            }
            
            html.append("  </div>\n");
        }
        
        html.append("</body>\n</html>\n");
        
        return html.toString();
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
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;");
    }
}