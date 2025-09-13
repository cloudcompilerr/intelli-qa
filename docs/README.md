# Agentic E2E Tester Documentation

Welcome to the Agentic E2E Tester - an intelligent AI-powered testing system for end-to-end regression testing across microservices architectures.

## Table of Contents

- [Quick Start Guide](./quick-start.md)
- [API Documentation](./api/README.md)
- [User Guide](./user-guide.md)
- [Deployment Guide](./deployment/README.md)
- [Configuration Reference](./configuration.md)
- [Troubleshooting](./troubleshooting.md)
- [Examples](./examples/README.md)

## Overview

The Agentic E2E Tester is a Spring Boot application that uses AI to automatically execute comprehensive end-to-end tests across complex microservices architectures. It can:

- Parse natural language test scenarios into executable test plans
- Orchestrate tests across 16-20+ microservices
- Validate event-driven architectures using Kafka
- Perform intelligent failure analysis and root cause detection
- Integrate with CI/CD pipelines for automated regression testing

## Architecture

The system is built on:
- **Spring Boot 3.x** - Core application framework
- **Spring AI** - Local LLM integration for AI capabilities
- **Apache Kafka** - Event-driven testing and message validation
- **Couchbase** - Database integration and data validation
- **Docker** - Containerized deployment
- **Kubernetes** - Orchestration and scaling

## Key Features

### AI-Powered Test Generation
- Natural language test scenario parsing
- Intelligent test plan generation
- Dynamic test adaptation based on system responses

### Comprehensive Integration Testing
- REST API testing with dynamic endpoint discovery
- Kafka event flow validation with correlation tracking
- Database state validation and comparison
- Service health monitoring and dependency mapping

### Intelligent Analysis
- AI-powered failure analysis and root cause detection
- Pattern recognition and learning from test history
- Automated remediation suggestions

### Enterprise Integration
- CI/CD pipeline integration (Jenkins, GitLab)
- Monitoring and alerting (Prometheus, Grafana)
- Security and compliance features
- Multi-environment support

## Getting Started

1. **Prerequisites**: Docker, Java 17+, Maven 3.8+
2. **Quick Setup**: See [Quick Start Guide](./quick-start.md)
3. **Configuration**: Review [Configuration Reference](./configuration.md)
4. **First Test**: Follow [User Guide](./user-guide.md)

## Support

- **Documentation**: Browse the docs in this directory
- **Examples**: Check out [examples](./examples/README.md)
- **Issues**: Report issues in your project repository
- **Configuration**: See [troubleshooting guide](./troubleshooting.md)