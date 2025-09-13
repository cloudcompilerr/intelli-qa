package com.agentic.e2etester.cicd;

/**
 * Supported test result report formats for CI/CD integration.
 */
public enum ReportFormat {
    
    /**
     * JUnit XML format - widely supported by CI/CD platforms.
     */
    JUNIT_XML("junit", "application/xml", ".xml"),
    
    /**
     * TestNG XML format.
     */
    TESTNG_XML("testng", "application/xml", ".xml"),
    
    /**
     * JSON format for programmatic consumption.
     */
    JSON("json", "application/json", ".json"),
    
    /**
     * HTML format for human-readable reports.
     */
    HTML("html", "text/html", ".html"),
    
    /**
     * Markdown format for documentation.
     */
    MARKDOWN("markdown", "text/markdown", ".md"),
    
    /**
     * TAP (Test Anything Protocol) format.
     */
    TAP("tap", "text/plain", ".tap"),
    
    /**
     * Allure format for detailed reporting.
     */
    ALLURE("allure", "application/json", ".json"),
    
    /**
     * Custom format specific to the platform.
     */
    CUSTOM("custom", "application/octet-stream", "");
    
    private final String formatName;
    private final String mimeType;
    private final String fileExtension;
    
    ReportFormat(String formatName, String mimeType, String fileExtension) {
        this.formatName = formatName;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }
    
    public String getFormatName() { return formatName; }
    public String getMimeType() { return mimeType; }
    public String getFileExtension() { return fileExtension; }
}