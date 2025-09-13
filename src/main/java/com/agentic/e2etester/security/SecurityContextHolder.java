package com.agentic.e2etester.security;

/**
 * Thread-local holder for security context.
 */
public class SecurityContextHolder {
    
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Sets the security context for the current thread.
     *
     * @param context the security context
     */
    public static void setContext(SecurityContext context) {
        contextHolder.set(context);
    }
    
    /**
     * Gets the security context for the current thread.
     *
     * @return the security context, or null if not set
     */
    public static SecurityContext getContext() {
        return contextHolder.get();
    }
    
    /**
     * Clears the security context for the current thread.
     */
    public static void clearContext() {
        contextHolder.remove();
    }
    
    /**
     * Checks if there is a security context set for the current thread.
     *
     * @return true if context is set
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
}