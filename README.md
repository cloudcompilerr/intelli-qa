# Agentic E2E Tester

An intelligent AI-powered end-to-end testing system built with Spring AI that autonomously executes regression tests across complex microservices architectures.

## Features

- **AI-Powered Testing**: Uses local LLM (Ollama) for intelligent test scenario parsing and execution
- **Event-Driven Architecture**: Native Kafka integration for testing event flows
- **Microservices Testing**: Comprehensive REST API and database validation
- **Intelligent Failure Analysis**: AI-driven root cause analysis and diagnostics
- **Local AI Processing**: Complete offline operation with Docker-based LLM setup

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker and Docker Compose
- 8GB+ RAM (for local LLM models)

## Quick Start

### 1. Start Infrastructure Services

```bash
# Start all required services
docker-compose up -d

# Wait for services to be ready (may take a few minutes)
docker-compose logs -f ollama
```

### 2. Setup Ollama Models

```bash
# Run the setup script to pull required AI models
./scripts/setup-ollama.sh
```

### 3. Run the Application

```bash
# Build and run the Spring Boot application
mvn spring-boot:run
```

### 4. Verify Setup

- Application: http://localhost:8080/actuator/health
- Kafka UI: http://localhost:8080 (Kafka monitoring)
- Prometheus: http://localhost:9090 (Metrics)
- Couchbase: http://localhost:8091 (admin/password)

## Architecture

The system follows a layered architecture:

- **AI Layer**: Spring AI with local Ollama LLM
- **Testing Layer**: Test execution engine and scenario parsing
- **Integration Layer**: Kafka, REST, and database adapters
- **Monitoring Layer**: Observability and metrics collection

## Configuration

Key configuration files:
- `application.yml`: Main application configuration
- `docker-compose.yml`: Infrastructure services
- `monitoring/prometheus.yml`: Metrics configuration

## Development

### Project Structure

```
src/main/java/com/agentic/e2etester/
├── ai/                 # AI components and LLM integration
├── testing/            # Core testing framework
├── integration/        # External system integrations
├── model/              # Data models and domain objects
├── service/            # Business logic services
├── config/             # Configuration classes
└── controller/         # REST API controllers
```

### Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify
```

## Docker Services

- **Ollama**: Local LLM service (port 11434)
- **PostgreSQL**: Vector store for AI embeddings (port 5432)
- **Kafka**: Event streaming platform (port 9092)
- **Couchbase**: Document database (ports 8091-8096)
- **Prometheus**: Metrics collection (port 9090)

## Next Steps

1. Implement AI agent controller (Task 2)
2. Create core data models (Task 3)
3. Build test scenario parser (Task 4)
4. Develop service integrations (Tasks 5-8)

## License

This project is licensed under the MIT License.