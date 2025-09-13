#!/bin/bash

# Agentic E2E Tester - Kubernetes Deployment Script
# This script deploys the complete system to Kubernetes

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
NAMESPACE="${NAMESPACE:-agentic-e2e-tester}"
ENVIRONMENT="${1:-staging}"
HELM_RELEASE_NAME="${HELM_RELEASE_NAME:-agentic-tester}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check helm
    if ! command -v helm &> /dev/null; then
        log_error "Helm is not installed. Please install Helm first."
        exit 1
    fi
    
    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    # Check if we have sufficient permissions
    if ! kubectl auth can-i create namespace &> /dev/null; then
        log_error "Insufficient permissions to create namespaces."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Function to setup namespace
setup_namespace() {
    log_info "Setting up namespace: $NAMESPACE"
    
    cd "$PROJECT_ROOT"
    
    # Apply namespace configuration
    kubectl apply -f k8s/namespace.yaml
    
    # Wait for namespace to be ready
    kubectl wait --for=condition=Ready namespace/$NAMESPACE --timeout=60s
    
    log_success "Namespace setup complete"
}

# Function to setup secrets
setup_secrets() {
    log_info "Setting up secrets..."
    
    # Generate secrets if they don't exist
    if ! kubectl get secret agentic-secrets -n $NAMESPACE &> /dev/null; then
        log_info "Creating application secrets..."
        
        # Generate random passwords
        POSTGRES_PASSWORD=$(openssl rand -base64 32)
        COUCHBASE_PASSWORD=$(openssl rand -base64 32)
        REDIS_PASSWORD=$(openssl rand -base64 32)
        API_KEY=$(openssl rand -base64 32)
        
        # Create secret
        kubectl create secret generic agentic-secrets \
            --from-literal=postgres-username="agentic_user" \
            --from-literal=postgres-password="$POSTGRES_PASSWORD" \
            --from-literal=couchbase-username="admin" \
            --from-literal=couchbase-password="$COUCHBASE_PASSWORD" \
            --from-literal=redis-password="$REDIS_PASSWORD" \
            --from-literal=api-key="$API_KEY" \
            -n $NAMESPACE
        
        log_success "Application secrets created"
    else
        log_info "Application secrets already exist"
    fi
    
    # Setup TLS secrets for production
    if [ "$ENVIRONMENT" = "prod" ] || [ "$ENVIRONMENT" = "production" ]; then
        setup_tls_secrets
    fi
    
    log_success "Secrets setup complete"
}

# Function to setup TLS secrets
setup_tls_secrets() {
    log_info "Setting up TLS secrets..."
    
    if ! kubectl get secret agentic-tls-secret -n $NAMESPACE &> /dev/null; then
        log_warning "TLS secret not found. Please create TLS certificates manually:"
        echo "kubectl create secret tls agentic-tls-secret \\"
        echo "  --cert=path/to/tls.crt \\"
        echo "  --key=path/to/tls.key \\"
        echo "  -n $NAMESPACE"
    else
        log_success "TLS secrets already configured"
    fi
}

# Function to setup storage
setup_storage() {
    log_info "Setting up storage..."
    
    cd "$PROJECT_ROOT"
    
    # Apply storage configuration
    kubectl apply -f k8s/storage.yaml
    
    # Wait for PVCs to be bound
    log_info "Waiting for persistent volumes to be bound..."
    kubectl wait --for=condition=Bound pvc --all -n $NAMESPACE --timeout=300s
    
    log_success "Storage setup complete"
}

# Function to deploy with kubectl
deploy_with_kubectl() {
    log_info "Deploying with kubectl..."
    
    cd "$PROJECT_ROOT"
    
    # Apply configurations
    kubectl apply -f k8s/configmap.yaml
    kubectl apply -f k8s/deployments.yaml
    kubectl apply -f k8s/services.yaml
    
    # Apply ingress for non-local environments
    if [ "$ENVIRONMENT" != "local" ]; then
        kubectl apply -f k8s/ingress.yaml
    fi
    
    log_success "Kubernetes manifests applied"
}

# Function to deploy with Helm
deploy_with_helm() {
    log_info "Deploying with Helm..."
    
    cd "$PROJECT_ROOT"
    
    # Add required Helm repositories
    log_info "Adding Helm repositories..."
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo update
    
    # Create values file for environment
    create_helm_values_file
    
    # Install or upgrade Helm release
    if helm list -n $NAMESPACE | grep -q $HELM_RELEASE_NAME; then
        log_info "Upgrading existing Helm release..."
        helm upgrade $HELM_RELEASE_NAME ./helm/agentic-e2e-tester \
            --namespace $NAMESPACE \
            --values ./helm/values-$ENVIRONMENT.yaml \
            --timeout 10m
    else
        log_info "Installing new Helm release..."
        helm install $HELM_RELEASE_NAME ./helm/agentic-e2e-tester \
            --namespace $NAMESPACE \
            --create-namespace \
            --values ./helm/values-$ENVIRONMENT.yaml \
            --timeout 10m
    fi
    
    log_success "Helm deployment complete"
}

# Function to create environment-specific Helm values
create_helm_values_file() {
    log_info "Creating Helm values file for $ENVIRONMENT..."
    
    case $ENVIRONMENT in
        "prod"|"production")
            cat > ./helm/values-$ENVIRONMENT.yaml << EOF
replicaCount: 3

image:
  tag: "latest"
  pullPolicy: Always

resources:
  limits:
    cpu: 4000m
    memory: 8Gi
  requests:
    cpu: 1000m
    memory: 4Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20

ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/rate-limit: "100"
  hosts:
    - host: agentic-tester.yourdomain.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: agentic-tls-secret
      hosts:
        - agentic-tester.yourdomain.com

postgresql:
  enabled: true
  auth:
    password: "$(kubectl get secret agentic-secrets -n $NAMESPACE -o jsonpath='{.data.postgres-password}' | base64 -d)"
  primary:
    persistence:
      size: 100Gi
    resources:
      limits:
        cpu: 2000m
        memory: 4Gi

monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
  prometheusRule:
    enabled: true

networkPolicy:
  enabled: true
EOF
            ;;
        "staging")
            cat > ./helm/values-$ENVIRONMENT.yaml << EOF
replicaCount: 2

resources:
  limits:
    cpu: 2000m
    memory: 4Gi
  requests:
    cpu: 500m
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10

ingress:
  enabled: true
  hosts:
    - host: agentic-tester-staging.yourdomain.com
      paths:
        - path: /
          pathType: Prefix

postgresql:
  enabled: true
  primary:
    persistence:
      size: 50Gi

monitoring:
  enabled: true
EOF
            ;;
        *)
            # Development/default values
            cat > ./helm/values-$ENVIRONMENT.yaml << EOF
replicaCount: 1

resources:
  limits:
    cpu: 1000m
    memory: 2Gi
  requests:
    cpu: 250m
    memory: 1Gi

autoscaling:
  enabled: false

ingress:
  enabled: false

postgresql:
  enabled: true
  primary:
    persistence:
      size: 20Gi

monitoring:
  enabled: true
EOF
            ;;
    esac
    
    log_success "Helm values file created"
}

# Function to wait for deployments
wait_for_deployments() {
    log_info "Waiting for deployments to be ready..."
    
    # Wait for all deployments
    kubectl wait --for=condition=Available deployment --all -n $NAMESPACE --timeout=600s
    
    # Wait for all pods to be ready
    kubectl wait --for=condition=Ready pod --all -n $NAMESPACE --timeout=600s
    
    log_success "All deployments are ready"
}

# Function to setup initial data
setup_initial_data() {
    log_info "Setting up initial data..."
    
    # Get Ollama pod name
    OLLAMA_POD=$(kubectl get pods -n $NAMESPACE -l app.kubernetes.io/component=ollama -o jsonpath='{.items[0].metadata.name}')
    
    if [ -n "$OLLAMA_POD" ]; then
        log_info "Setting up AI models..."
        kubectl exec $OLLAMA_POD -n $NAMESPACE -- ollama pull codellama:7b
        kubectl exec $OLLAMA_POD -n $NAMESPACE -- ollama pull mistral:7b
    else
        log_warning "Ollama pod not found, skipping model setup"
    fi
    
    # Initialize vector database
    POSTGRES_POD=$(kubectl get pods -n $NAMESPACE -l app.kubernetes.io/component=postgres -o jsonpath='{.items[0].metadata.name}')
    
    if [ -n "$POSTGRES_POD" ] && [ -f "scripts/init-vector-db.sql" ]; then
        log_info "Initializing vector database..."
        kubectl exec -i $POSTGRES_POD -n $NAMESPACE -- psql -U agentic_user -d vectordb < scripts/init-vector-db.sql
    fi
    
    log_success "Initial data setup complete"
}

# Function to run health checks
run_health_checks() {
    log_info "Running health checks..."
    
    # Get service endpoints
    if kubectl get ingress -n $NAMESPACE &> /dev/null; then
        INGRESS_HOST=$(kubectl get ingress -n $NAMESPACE -o jsonpath='{.items[0].spec.rules[0].host}')
        if [ -n "$INGRESS_HOST" ]; then
            HEALTH_URL="https://$INGRESS_HOST/actuator/health"
        else
            # Use port-forward for health check
            kubectl port-forward svc/agentic-tester-service 8081:8081 -n $NAMESPACE &
            PORT_FORWARD_PID=$!
            sleep 5
            HEALTH_URL="http://localhost:8081/actuator/health"
        fi
    else
        # Use port-forward for health check
        kubectl port-forward svc/agentic-tester-service 8081:8081 -n $NAMESPACE &
        PORT_FORWARD_PID=$!
        sleep 5
        HEALTH_URL="http://localhost:8081/actuator/health"
    fi
    
    # Check main application health
    if curl -f $HEALTH_URL &>/dev/null; then
        log_success "✓ Agentic E2E Tester is healthy"
    else
        log_error "✗ Agentic E2E Tester health check failed"
        if [ -n "$PORT_FORWARD_PID" ]; then
            kill $PORT_FORWARD_PID 2>/dev/null || true
        fi
        return 1
    fi
    
    # Clean up port-forward if used
    if [ -n "$PORT_FORWARD_PID" ]; then
        kill $PORT_FORWARD_PID 2>/dev/null || true
    fi
    
    # Check pod status
    UNHEALTHY_PODS=$(kubectl get pods -n $NAMESPACE --field-selector=status.phase!=Running --no-headers 2>/dev/null | wc -l)
    if [ "$UNHEALTHY_PODS" -eq 0 ]; then
        log_success "✓ All pods are running"
    else
        log_error "✗ $UNHEALTHY_PODS pods are not running"
        kubectl get pods -n $NAMESPACE --field-selector=status.phase!=Running
        return 1
    fi
    
    log_success "All health checks passed"
}

# Function to display access information
display_access_info() {
    log_info "Access Information:"
    echo ""
    
    # Check if ingress is configured
    if kubectl get ingress -n $NAMESPACE &> /dev/null; then
        INGRESS_HOST=$(kubectl get ingress -n $NAMESPACE -o jsonpath='{.items[0].spec.rules[0].host}')
        if [ -n "$INGRESS_HOST" ]; then
            echo "🚀 Main Application:     https://$INGRESS_HOST"
            echo "🏥 Health Check:         https://$INGRESS_HOST/actuator/health"
            echo "📋 API Documentation:    https://$INGRESS_HOST/swagger-ui.html"
        fi
    else
        echo "🔧 Use port-forwarding to access services:"
        echo "   kubectl port-forward svc/agentic-tester-service 8080:8080 -n $NAMESPACE"
        echo "   kubectl port-forward svc/grafana-service 3000:3000 -n $NAMESPACE"
        echo "   kubectl port-forward svc/prometheus-service 9090:9090 -n $NAMESPACE"
    fi
    
    echo ""
    echo "🔐 Credentials:"
    API_KEY=$(kubectl get secret agentic-secrets -n $NAMESPACE -o jsonpath='{.data.api-key}' 2>/dev/null | base64 -d 2>/dev/null || echo 'Not found')
    echo "   - API Key: $API_KEY"
    
    if kubectl get secret grafana-admin-password -n $NAMESPACE &> /dev/null; then
        GRAFANA_PASSWORD=$(kubectl get secret grafana-admin-password -n $NAMESPACE -o jsonpath='{.data.admin-password}' | base64 -d)
        echo "   - Grafana: admin/$GRAFANA_PASSWORD"
    fi
    
    echo ""
    echo "📊 Monitoring:"
    echo "   kubectl get pods -n $NAMESPACE"
    echo "   kubectl logs -f deployment/agentic-tester -n $NAMESPACE"
    echo ""
}

# Function to show logs
show_logs() {
    log_info "Showing application logs (press Ctrl+C to exit)..."
    kubectl logs -f deployment/agentic-tester -n $NAMESPACE
}

# Function to cleanup
cleanup() {
    log_info "Cleaning up deployment..."
    
    if [ "$DEPLOYMENT_METHOD" = "helm" ]; then
        helm uninstall $HELM_RELEASE_NAME -n $NAMESPACE
    else
        kubectl delete -f k8s/ --ignore-not-found=true
    fi
    
    # Optionally delete namespace
    read -p "Delete namespace $NAMESPACE? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kubectl delete namespace $NAMESPACE
        log_success "Namespace deleted"
    fi
    
    log_success "Cleanup complete"
}

# Main deployment function
deploy() {
    log_info "Starting Agentic E2E Tester Kubernetes deployment..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Namespace: $NAMESPACE"
    log_info "Deployment method: $DEPLOYMENT_METHOD"
    
    check_prerequisites
    setup_namespace
    setup_secrets
    setup_storage
    
    if [ "$DEPLOYMENT_METHOD" = "helm" ]; then
        deploy_with_helm
    else
        deploy_with_kubectl
    fi
    
    wait_for_deployments
    setup_initial_data
    run_health_checks
    display_access_info
    
    log_success "🎉 Kubernetes deployment completed successfully!"
    
    # Ask if user wants to see logs
    read -p "Would you like to view the application logs? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        show_logs
    fi
}

# Script usage
usage() {
    echo "Usage: $0 [ENVIRONMENT] [COMMAND] [OPTIONS]"
    echo ""
    echo "ENVIRONMENT:"
    echo "  dev         Development environment"
    echo "  staging     Staging environment (default)"
    echo "  prod        Production environment"
    echo ""
    echo "COMMANDS:"
    echo "  deploy      Deploy the system (default)"
    echo "  upgrade     Upgrade existing deployment"
    echo "  status      Show deployment status"
    echo "  logs        Show logs"
    echo "  health      Run health checks"
    echo "  cleanup     Remove deployment"
    echo ""
    echo "OPTIONS:"
    echo "  --helm      Use Helm for deployment (default)"
    echo "  --kubectl   Use kubectl for deployment"
    echo "  --namespace Specify namespace (default: agentic-e2e-tester)"
    echo ""
    echo "Examples:"
    echo "  $0                           # Deploy staging with Helm"
    echo "  $0 prod deploy --helm        # Deploy production with Helm"
    echo "  $0 dev deploy --kubectl      # Deploy development with kubectl"
    echo "  $0 staging logs              # Show staging logs"
    echo "  $0 prod cleanup              # Cleanup production deployment"
}

# Parse command line arguments
DEPLOYMENT_METHOD="helm"
COMMAND="deploy"

while [[ $# -gt 0 ]]; do
    case $1 in
        --helm)
            DEPLOYMENT_METHOD="helm"
            shift
            ;;
        --kubectl)
            DEPLOYMENT_METHOD="kubectl"
            shift
            ;;
        --namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        deploy|upgrade|status|logs|health|cleanup|help|-h|--help)
            COMMAND="$1"
            shift
            ;;
        dev|staging|prod|production)
            if [ -z "$ENVIRONMENT_SET" ]; then
                ENVIRONMENT="$1"
                ENVIRONMENT_SET=true
            fi
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Handle commands
case $COMMAND in
    "deploy")
        deploy
        ;;
    "upgrade")
        if [ "$DEPLOYMENT_METHOD" = "helm" ]; then
            deploy_with_helm
            wait_for_deployments
            run_health_checks
            display_access_info
        else
            log_error "Upgrade command only supported with Helm deployment"
            exit 1
        fi
        ;;
    "status")
        log_info "Deployment status for namespace: $NAMESPACE"
        kubectl get all -n $NAMESPACE
        ;;
    "logs")
        show_logs
        ;;
    "health")
        run_health_checks
        ;;
    "cleanup")
        cleanup
        ;;
    "help"|"-h"|"--help")
        usage
        ;;
    *)
        log_error "Unknown command: $COMMAND"
        usage
        exit 1
        ;;
esac