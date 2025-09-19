# Quick Start Guide - Agentic E2E Tester

This guide helps you get the Agentic E2E Tester running quickly, working around common Docker issues.

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker Desktop (latest version)

## Step 1: Fix Docker Compose Command

The system uses Docker Compose v2. If you encounter `docker-compose: command not found`, use:

```bash
# Instead of docker-compose
docker compose

# Check version
docker compose version
```

## Step 2: Build the Application

```bash
# Build without tests (to avoid compilation issues)
mvn clean package -Dmaven.test.skip=true
```

## Step 3: Start Core Services

Start the essential services first:

```bash
# Start PostgreSQL and Redis
docker compose up -d postgres redis

# Check status
docker compose ps
```

## Step 4: Start Additional Services

Once core services are running:

```bash
# Start Kafka ecosystem
docker compose up -d zookeeper kafka

# Start monitoring
docker compose up -d prometheus grafana

# Start Ollama (AI service)
docker compose up -d ollama
```

## Step 5: Start the Main Application

```bash
# Start the main application
docker compose up -d agentic-tester

# Check logs
docker compose logs -f agentic-tester
```

## Troubleshooting

### Docker API Errors

If you see "500 Internal Server Error" or API version errors:

1. **Restart Docker Desktop:**
   ```bash
   # Kill Docker processes
   pkill -f "Docker Desktop"
   
   # Wait and restart
   sleep 5
   open -a "Docker Desktop"
   
   # Wait for startup (30-60 seconds)
   sleep 30
   ```

2. **Check Docker Context:**
   ```bash
   docker context ls
   docker context use desktop-linux
   ```

3. **Verify Docker is working:**
   ```bash
   docker version
   docker compose version
   ```

### Build Issues

If Maven build fails with test compilation errors:

```bash
# Use this command instead
mvn clean package -Dmaven.test.skip=true
```

### Service Startup Issues

Start services incrementally:

```bash
# 1. Database first
docker compose up -d postgres
sleep 10

# 2. Message queue
docker compose up -d zookeeper
sleep 10
docker compose up -d kafka
sleep 10

# 3. AI service
docker compose up -d ollama
sleep 10

# 4. Main application
docker compose up -d agentic-tester
```

## Service URLs

Once everything is running:

- **Main Application**: http://localhost:8080
- **Health Check**: http://localhost:8081/actuator/health
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Kafka UI**: http://localhost:8082

## Minimal Setup

For a minimal working setup, you only need:

```bash
# Essential services only
docker compose up -d postgres redis ollama agentic-tester
```

## Alternative: Use Deployment Script

If Docker is working properly, use the deployment script:

```bash
# Make executable
chmod +x scripts/deploy-docker.sh

# Deploy
./scripts/deploy-docker.sh dev
```

## Logs and Monitoring

```bash
# View all logs
docker compose logs -f

# View specific service logs
docker compose logs -f agentic-tester

# Check service status
docker compose ps

# Stop all services
docker compose down
```

## Next Steps

1. Access the application at http://localhost:8080
2. Check the health endpoint at http://localhost:8081/actuator/health
3. View monitoring dashboards at http://localhost:3000
4. Explore the API documentation (if available)

For detailed configuration and advanced features, see the full documentation in `docs/`.