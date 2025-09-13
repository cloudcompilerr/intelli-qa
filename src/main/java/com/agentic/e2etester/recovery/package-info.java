/**
 * Comprehensive error handling and recovery system for the agentic E2E tester.
 * 
 * This package provides:
 * - Circuit breaker patterns for service failure protection
 * - Retry mechanisms with exponential backoff
 * - Graceful degradation strategies
 * - Rollback capabilities for test cleanup
 * - Coordinated error handling across all system components
 * 
 * Key components:
 * - {@link com.agentic.e2etester.recovery.ErrorHandlingService} - Main coordination service
 * - {@link com.agentic.e2etester.recovery.CircuitBreaker} - Circuit breaker implementation
 * - {@link com.agentic.e2etester.recovery.RetryExecutor} - Retry logic with backoff
 * - {@link com.agentic.e2etester.recovery.GracefulDegradationManager} - Degradation strategies
 * - {@link com.agentic.e2etester.recovery.RollbackManager} - Rollback coordination
 * 
 * @author Agentic E2E Tester
 * @version 1.0
 */
package com.agentic.e2etester.recovery;