# Deployment Guide

This document provides comprehensive deployment instructions for the Agentic E2E Tester system.

## Quick Start

### Docker Compose (Recommended for Development)

```bash
# Development deployment
./scripts/deploy-docker.sh dev

# Production deployment
./scripts/deploy-docker.sh prod
```

### Kubernetes (Recommended for Production)

```bash
# Staging deployment with Helm
./scripts/deploy-k8s.sh staging deploy --helm

# Production deployment with Helm
./scripts/deploy-k8s.sh prod deploy --helm
```

## Deployment Options

### 1. Docker Compose

**Best for:** Development, testing, small-scale deployments

**Features:**
- Single-machine deployment
- Easy setup and teardown
- Integrated monitoring stack
- Local AI model execution

**Requirements:**
- Docker 20.10+
- Docker Compose 2.0+
- 8GB RAM minimum
- 50GB disk space

**Usage:**
```bash
# Deploy development environment
./scripts/deploy-docker.sh dev deploy

# Deploy production environment with secrets
./scripts/deploy-docker.sh prod deploy

# View logs
./scripts/deploy-docker.sh dev logs

# Health check
./scripts/deploy-docker.sh dev health

# Cleanup
./scripts/deploy-docker.sh dev cleanup
```

### 2. Kubernetes with kubectl

**Best for:** Custom Kubernetes deployments, CI/CD integration

**Features:**
- Native Kubernetes manifests
- Full control over configuration
- Custom resource definitions
- Manual scaling and management

**Requirements:**
- Kubernetes 1.24+
- kubectl configured
- Cluster admin permissions
- Persistent volume support

**Usage:**
```bash
# Deploy with kubectl
./scripts/deploy-k8s.sh staging deploy --kubectl

# Check status
./scripts/deploy-k8s.sh staging status

# View logs
./scripts/deploy-k8s.sh staging logs

# Cleanup
./scripts/deploy-k8s.sh staging cleanup
```

### 3. Kubernetes with Helm

**Best for:** Production deployments, multi-environment management

**Features:**
- Package management
- Environment-specific configurations
- Dependency management
- Easy upgrades and rollbacks
- Built-in monitoring stack

**Requirements:**
- Kubernetes 1.24+
- Helm 3.8+
- kubectl configured
- Cluster admin permissions

**Usage:**
```bash
# Deploy with Helm
./scripts/deploy-k8s.sh prod deploy --helm

# Upgrade deployment
./scripts/deploy-k8s.sh prod upgrade --helm

# Check Helm status
helm status agentic-tester -n agentic-e2e-tester

# Rollback if needed
helm rollback agentic-tester -n agentic-e2e-tester
```

## Environment Configurations

### Development Environment

**Characteristics:**
- Single replica
- Reduced resource requirements
- Debug logging enabled
- Security disabled for ease of use
- Local storage

**Access:**
- Application: http://localhost:8080
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090

### Staging Environment

**Characteristics:**
- 2 replicas for basic HA
- Moderate resource allocation
- Security enabled
- Persistent storage
- Basic monitoring

**Configuration:**
```yaml
replicaCount: 2
resources:
  limits:
    cpu: 2000m
    memory: 4Gi
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
```

### Production Environment

**Characteristics:**
- 3+ replicas for high availability
- Full resource allocation
- Enhanced security
- Persistent storage with backups
- Comprehensive monitoring and alerting
- TLS encryption

**Configuration:**
```yaml
replicaCount: 3
resources:
  limits:
    cpu: 4000m
    memory: 8Gi
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
ingress:
  enabled: true
  tls:
    enabled: true
```

## Resource Requirements

### Minimum Requirements

| Component | CPU | Memory | Storage |
|-----------|-----|--------|---------|
| Application | 500m | 2Gi | 10Gi |
| Ollama LLM | 2000m | 4Gi | 50Gi |
| PostgreSQL | 500m | 1Gi | 20Gi |
| Kafka | 1000m | 2Gi | 30Gi |
| Couchbase | 1000m | 2Gi | 40Gi |
| Monitoring | 500m | 1Gi | 15Gi |
| **Total** | **5.5 CPU** | **12Gi** | **165Gi** |

### Recommended Production

| Component | CPU | Memory | Storage |
|-----------|-----|--------|---------|
| Application | 2000m | 4Gi | 20Gi |
| Ollama LLM | 4000m | 8Gi | 100Gi |
| PostgreSQL | 2000m | 4Gi | 100Gi |
| Kafka | 2000m | 4Gi | 100Gi |
| Couchbase | 2000m | 4Gi | 200Gi |
| Monitoring | 1000m | 2Gi | 50Gi |
| **Total** | **13 CPU** | **26Gi** | **570Gi** |

## Security Configuration

### TLS/SSL Setup

1. **Generate certificates:**
```bash
# Self-signed for testing
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout tls.key -out tls.crt \
  -subj "/CN=agentic-tester.yourdomain.com"

# Create Kubernetes secret
kubectl create secret tls agentic-tls-secret \
  --cert=tls.crt --key=tls.key \
  -n agentic-e2e-tester
```

2. **Use cert-manager for automatic certificates:**
```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@yourdomain.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```

### Authentication Setup

1. **JWT Configuration:**
```yaml
agentic:
  security:
    authentication:
      type: jwt
      jwt:
        secret: ${JWT_SECRET}
        expiration: PT24H
```

2. **OAuth2 Integration:**
```yaml
agentic:
  security:
    authentication:
      type: oauth2
      oauth2:
        provider: keycloak
        client-id: ${OAUTH2_CLIENT_ID}
        client-secret: ${OAUTH2_CLIENT_SECRET}
```

## Monitoring and Observability

### Metrics Collection

The system automatically exposes metrics at:
- Application metrics: `/actuator/prometheus`
- Custom business metrics: Test execution, AI performance, service health
- Infrastructure metrics: CPU, memory, disk, network

### Dashboards

Pre-configured Grafana dashboards include:
- **Application Overview**: Test execution metrics, success rates
- **AI Performance**: Model response times, error rates
- **Infrastructure**: Resource utilization, service health
- **Business Metrics**: Test volume, failure analysis

### Alerting

Default alerts configured for:
- Application downtime
- High test failure rates
- Resource exhaustion
- Service unavailability
- Performance degradation

## Backup and Recovery

### Database Backup

```bash
# Manual backup
kubectl exec deployment/postgres -n agentic-e2e-tester -- \
  pg_dump -U agentic_user vectordb > backup-$(date +%Y%m%d).sql

# Automated backup with CronJob
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: postgres:16
            command:
            - /bin/bash
            - -c
            - pg_dump -h postgres-service -U agentic_user vectordb > /backup/backup-$(date +%Y%m%d).sql
```

### Configuration Backup

```bash
# Backup all Kubernetes resources
kubectl get all,configmap,secret,pvc -n agentic-e2e-tester -o yaml > backup-k8s.yaml

# Backup Helm values
helm get values agentic-tester -n agentic-e2e-tester > backup-helm-values.yaml
```

## Troubleshooting

### Common Issues

1. **Pod startup failures:**
```bash
# Check pod status
kubectl describe pod <pod-name> -n agentic-e2e-tester

# Check logs
kubectl logs <pod-name> -n agentic-e2e-tester --previous
```

2. **Service connectivity issues:**
```bash
# Test service resolution
kubectl exec -it <pod-name> -n agentic-e2e-tester -- nslookup postgres-service

# Test port connectivity
kubectl exec -it <pod-name> -n agentic-e2e-tester -- telnet postgres-service 5432
```

3. **Resource constraints:**
```bash
# Check resource usage
kubectl top pods -n agentic-e2e-tester
kubectl top nodes

# Check resource quotas
kubectl describe resourcequota -n agentic-e2e-tester
```

### Health Checks

```bash
# Application health
curl http://localhost:8081/actuator/health

# Database health
kubectl exec deployment/postgres -n agentic-e2e-tester -- pg_isready

# AI model health
curl http://localhost:11434/api/tags
```

### Log Analysis

```bash
# Application logs
kubectl logs -f deployment/agentic-tester -n agentic-e2e-tester

# All pod logs
kubectl logs -f -l app.kubernetes.io/name=agentic-e2e-tester -n agentic-e2e-tester

# Previous container logs
kubectl logs deployment/agentic-tester -n agentic-e2e-tester --previous
```

## Scaling

### Horizontal Scaling

```bash
# Manual scaling
kubectl scale deployment agentic-tester --replicas=5 -n agentic-e2e-tester

# Auto-scaling configuration
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: agentic-tester-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: agentic-tester
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Vertical Scaling

```bash
# Update resource limits
kubectl patch deployment agentic-tester -n agentic-e2e-tester -p '
{
  "spec": {
    "template": {
      "spec": {
        "containers": [{
          "name": "agentic-tester",
          "resources": {
            "limits": {
              "cpu": "4000m",
              "memory": "8Gi"
            }
          }
        }]
      }
    }
  }
}'
```

## Maintenance

### Updates and Upgrades

```bash
# Update application image
kubectl set image deployment/agentic-tester agentic-tester=agentic-e2e-tester:v2.0.0 -n agentic-e2e-tester

# Helm upgrade
helm upgrade agentic-tester ./helm/agentic-e2e-tester \
  --namespace agentic-e2e-tester \
  --set image.tag=v2.0.0

# Rollback if needed
kubectl rollout undo deployment/agentic-tester -n agentic-e2e-tester
helm rollback agentic-tester -n agentic-e2e-tester
```

### Maintenance Windows

```bash
# Drain node for maintenance
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# Cordon node to prevent scheduling
kubectl cordon <node-name>

# Uncordon node after maintenance
kubectl uncordon <node-name>
```

## Support

For additional support:

1. **Documentation**: Check the [docs](./docs/) directory
2. **Troubleshooting**: See [troubleshooting guide](./docs/troubleshooting.md)
3. **Configuration**: Review [configuration reference](./docs/configuration.md)
4. **Examples**: Explore [examples](./docs/examples/README.md)
5. **Community**: Join our community forums or Slack channel
6. **Enterprise Support**: Contact your support representative

## Contributing

To contribute to the deployment automation:

1. Fork the repository
2. Create a feature branch
3. Test your changes thoroughly
4. Submit a pull request with detailed description
5. Ensure all CI/CD checks pass

## License

This deployment automation is part of the Agentic E2E Tester project and is licensed under the same terms.