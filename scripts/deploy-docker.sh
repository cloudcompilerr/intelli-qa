#!/bin/bash

# Agentic E2E Tester - Docker Deployment Script
# This script deploys the complete system using Docker Compose

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENVIRONMENT="${1:-dev}"
COMPOSE_FILES="-f docker-compose.yml"

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
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check Docker Compose
    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not available. Please install Docker with Compose plugin."
        exit 1
    fi
    
    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running. Please start Docker first."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Function to setup environment
setup_environment() {
    log_info "Setting up environment for: $ENVIRONMENT"
    
    cd "$PROJECT_ROOT"
    
    case $ENVIRONMENT in
        "prod"|"production")
            COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.prod.yml"
            setup_production_secrets
            ;;
        "staging")
            COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.staging.yml"
            ;;
        "dev"|"development")
            # Use default compose file only
            ;;
        *)
            log_warning "Unknown environment: $ENVIRONMENT. Using development settings."
            ;;
    esac
    
    log_success "Environment setup complete"
}

# Function to setup production secrets
setup_production_secrets() {
    log_info "Setting up production secrets..."
    
    mkdir -p secrets
    
    # Generate secrets if they don't exist
    if [ ! -f secrets/postgres_password.txt ]; then
        openssl rand -base64 32 > secrets/postgres_password.txt
        log_info "Generated PostgreSQL password"
    fi
    
    if [ ! -f secrets/couchbase_password.txt ]; then
        openssl rand -base64 32 > secrets/couchbase_password.txt
        log_info "Generated Couchbase password"
    fi
    
    if [ ! -f secrets/grafana_password.txt ]; then
        openssl rand -base64 32 > secrets/grafana_password.txt
        log_info "Generated Grafana password"
    fi
    
    if [ ! -f secrets/redis_password.txt ]; then
        openssl rand -base64 32 > secrets/redis_password.txt
        log_info "Generated Redis password"
    fi
    
    if [ ! -f secrets/api_key.txt ]; then
        openssl rand -base64 32 > secrets/api_key.txt
        log_info "Generated API key"
    fi
    
    # Set proper permissions
    chmod 600 secrets/*
    
    log_success "Production secrets setup complete"
}

# Function to build application
build_application() {
    log_info "Building application..."
    
    cd "$PROJECT_ROOT"
    
    # Build the application JAR
    if [ -f "pom.xml" ]; then
        log_info "Building with Maven..."
        mvn clean package -DskipTests
    else
        log_warning "No pom.xml found, skipping application build"
    fi
    
    # Build Docker image
    log_info "Building Docker image..."
    docker build -t agentic-e2e-tester:latest .
    
    log_success "Application build complete"
}

# Function to start services
start_services() {
    log_info "Starting services..."
    
    cd "$PROJECT_ROOT"
    
    # Pull latest images
    log_info "Pulling latest images..."
    docker compose $COMPOSE_FILES pull
    
    # Start services
    log_info "Starting containers..."
    docker compose $COMPOSE_FILES up -d
    
    log_success "Services started"
}

# Function to wait for services
wait_for_services() {
    log_info "Waiting for services to be ready..."
    
    # Wait for PostgreSQL
    log_info "Waiting for PostgreSQL..."
    timeout 60 bash -c 'until docker compose exec -T postgres pg_isready -U postgres; do sleep 2; done'
    
    # Wait for Kafka
    log_info "Waiting for Kafka..."
    timeout 60 bash -c 'until docker compose exec -T kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092 &>/dev/null; do sleep 2; done'
    
    # Wait for Ollama
    log_info "Waiting for Ollama..."
    timeout 60 bash -c 'until curl -f http://localhost:11434/api/tags &>/dev/null; do sleep 2; done'
    
    # Wait for Couchbase
    log_info "Waiting for Couchbase..."
    timeout 60 bash -c 'until curl -f http://localhost:8091/pools &>/dev/null; do sleep 2; done'
    
    # Wait for main application
    log_info "Waiting for Agentic E2E Tester..."
    timeout 120 bash -c 'until curl -f http://localhost:8081/actuator/health &>/dev/null; do sleep 5; done'
    
    log_success "All services are ready"
}

# Function to setup initial data
setup_initial_data() {
    log_info "Setting up initial data..."
    
    # Download and setup Ollama models
    log_info "Setting up AI models..."
    docker compose exec ollama ollama pull codellama:7b
    docker compose exec ollama ollama pull mistral:7b
    
    # Initialize vector database
    if [ -f "scripts/init-vector-db.sql" ]; then
        log_info "Initializing vector database..."
        docker compose exec -T postgres psql -U postgres -d vectordb < scripts/init-vector-db.sql
    fi
    
    log_success "Initial data setup complete"
}

# Function to run health checks
run_health_checks() {
    log_info "Running health checks..."
    
    # Check main application
    if curl -f http://localhost:8081/actuator/health &>/dev/null; then
        log_success "✓ Agentic E2E Tester is healthy"
    else
        log_error "✗ Agentic E2E Tester health check failed"
        return 1
    fi
    
    # Check Ollama
    if curl -f http://localhost:11434/api/tags &>/dev/null; then
        log_success "✓ Ollama is healthy"
    else
        log_error "✗ Ollama health check failed"
        return 1
    fi
    
    # Check PostgreSQL
    if docker compose exec -T postgres pg_isready -U postgres &>/dev/null; then
        log_success "✓ PostgreSQL is healthy"
    else
        log_error "✗ PostgreSQL health check failed"
        return 1
    fi
    
    # Check Kafka
    if docker compose exec -T kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092 &>/dev/null; then
        log_success "✓ Kafka is healthy"
    else
        log_error "✗ Kafka health check failed"
        return 1
    fi
    
    log_success "All health checks passed"
}

# Function to display service URLs
display_service_urls() {
    log_info "Service URLs:"
    echo ""
    echo "🚀 Main Application:     http://localhost:8080"
    echo "📊 Grafana Dashboard:    http://localhost:3000"
    echo "📈 Prometheus:           http://localhost:9090"
    echo "🔍 Kafka UI:             http://localhost:8082"
    echo "🏥 Health Check:         http://localhost:8081/actuator/health"
    echo "📋 API Documentation:    http://localhost:8080/swagger-ui.html"
    echo ""
    
    if [ "$ENVIRONMENT" = "prod" ] || [ "$ENVIRONMENT" = "production" ]; then
        echo "🔐 Production credentials are stored in the 'secrets' directory"
        echo "   - Grafana admin password: $(cat secrets/grafana_password.txt 2>/dev/null || echo 'Not found')"
        echo "   - API Key: $(cat secrets/api_key.txt 2>/dev/null || echo 'Not found')"
    else
        echo "🔐 Development credentials:"
        echo "   - Grafana: admin/admin"
        echo "   - PostgreSQL: postgres/postgres"
        echo "   - Couchbase: Administrator/password"
    fi
    echo ""
}

# Function to show logs
show_logs() {
    log_info "Showing service logs (press Ctrl+C to exit)..."
    docker compose $COMPOSE_FILES logs -f
}

# Function to cleanup
cleanup() {
    log_info "Cleaning up..."
    cd "$PROJECT_ROOT"
    docker compose $COMPOSE_FILES down
    log_success "Cleanup complete"
}

# Main deployment function
deploy() {
    log_info "Starting Agentic E2E Tester deployment..."
    log_info "Environment: $ENVIRONMENT"
    
    check_prerequisites
    setup_environment
    build_application
    start_services
    wait_for_services
    setup_initial_data
    run_health_checks
    display_service_urls
    
    log_success "🎉 Deployment completed successfully!"
    
    # Ask if user wants to see logs
    read -p "Would you like to view the logs? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        show_logs
    fi
}

# Script usage
usage() {
    echo "Usage: $0 [ENVIRONMENT] [COMMAND]"
    echo ""
    echo "ENVIRONMENT:"
    echo "  dev         Development environment (default)"
    echo "  staging     Staging environment"
    echo "  prod        Production environment"
    echo ""
    echo "COMMANDS:"
    echo "  deploy      Deploy the system (default)"
    echo "  start       Start existing services"
    echo "  stop        Stop services"
    echo "  restart     Restart services"
    echo "  logs        Show logs"
    echo "  health      Run health checks"
    echo "  cleanup     Stop and remove all containers"
    echo ""
    echo "Examples:"
    echo "  $0                    # Deploy in development mode"
    echo "  $0 prod deploy        # Deploy in production mode"
    echo "  $0 staging logs       # Show logs for staging"
    echo "  $0 dev cleanup        # Cleanup development deployment"
}

# Handle commands
COMMAND="${2:-deploy}"

case $COMMAND in
    "deploy")
        deploy
        ;;
    "start")
        check_prerequisites
        setup_environment
        start_services
        wait_for_services
        run_health_checks
        display_service_urls
        ;;
    "stop")
        setup_environment
        log_info "Stopping services..."
        docker compose $COMPOSE_FILES stop
        log_success "Services stopped"
        ;;
    "restart")
        setup_environment
        log_info "Restarting services..."
        docker compose $COMPOSE_FILES restart
        wait_for_services
        run_health_checks
        log_success "Services restarted"
        ;;
    "logs")
        setup_environment
        show_logs
        ;;
    "health")
        run_health_checks
        ;;
    "cleanup")
        setup_environment
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