/**
 * Comprehensive assertion framework for validating business, technical, and data outcomes.
 * 
 * <p>This package provides a flexible and extensible assertion system that supports:
 * <ul>
 *   <li>Business logic assertions for validating end-to-end business processes</li>
 *   <li>Technical assertions for system behavior, performance, and metrics validation</li>
 *   <li>Data assertions for consistency, integrity, and persistence validation</li>
 *   <li>Custom assertion evaluators for domain-specific validation logic</li>
 * </ul>
 * 
 * <p>The main components include:
 * <ul>
 *   <li>{@link AssertionEngine} - Core interface for assertion evaluation</li>
 *   <li>{@link DefaultAssertionEngine} - Default implementation with built-in assertion types</li>
 *   <li>{@link BusinessAssertion} - Specialized assertions for business rule validation</li>
 *   <li>{@link TechnicalAssertion} - Specialized assertions for technical metrics</li>
 *   <li>{@link DataAssertion} - Specialized assertions for data validation</li>
 *   <li>{@link CustomAssertionEvaluator} - Interface for custom assertion logic</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * AssertionEngine engine = new DefaultAssertionEngine();
 * 
 * // Business assertion
 * BusinessAssertion businessAssertion = new BusinessAssertion(
 *     "order-completion", "Order completion validation", 
 *     "order_completed", Map.of("orderType", "premium")
 * );
 * 
 * // Technical assertion
 * TechnicalAssertion techAssertion = new TechnicalAssertion(
 *     "response-time", "Response time validation", "response_time"
 * );
 * techAssertion.setResponseTimeThreshold(Duration.ofMillis(2000));
 * 
 * // Data assertion
 * DataAssertion dataAssertion = new DataAssertion(
 *     "data-validation", "Order data validation", "orderData"
 * );
 * dataAssertion.setRequiredFields(Arrays.asList("id", "status", "total"));
 * 
 * // Evaluate assertions
 * List<AssertionResult> results = engine.evaluateAssertions(
 *     Arrays.asList(businessAssertion, techAssertion, dataAssertion), 
 *     testContext
 * );
 * }</pre>
 */
package com.agentic.e2etester.testing.assertion;