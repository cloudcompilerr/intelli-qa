# Deployment Guide

This guide covers various deployment options for the Agentic E2E Tester system.

## Table of Contents

1. [Docker Compose Deployment](#docker-compose-deployment)
2. [Kubernetes Deployment](#kubernetes-deployment)
3. [Helm Chart Deployment](#helm-chart-deployment)
4. [Production Considerations](#production-considerations)
5. [Monitoring Setup](#monitoring-setup)
6. [Troubleshooting](#troubleshooting)

## Docker Compose Deployment

### Quick Start

For development and testing environments:

```bash
# Clone the repository
git clone <repository-url>
cd agentic-e2e-tester

# Start all services
docker compose up -d

# Check service status
docker compose ps

# View logs
docker compose logs -f agentic-tester
```

### Production Deployment

For production environments with enhanced security and performance:

```bash
# Create secrets directory
mkdir -p secrets

# Generate secure passwords
echo "$(openssl rand -base64 32)" > secrets/postgres_password.txt
echo "$(openssl rand -base64 32)" > secrets/couchbase_password.txt
echo "$(openssl rand -base64 32)" > secrets/grafana_password.txt
echo "$(openssl rand -base64 32)" > secrets/redis_password.txt
echo "$(openssl rand -base64 32)" > secrets/api_key.txt

# Deploy with production overrides
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Service URLs

After deployment, access the services at:

- **Main Application**: http://localhost:8080
- **Grafana Dashboard**: http://localhost:3000
- **Prometheus**: http://localhost:9090
- **Kafka UI**: http://localhost:8082

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.24+)
- kubectl configured
- Sufficient resources (see resource requirements)

### Manual Deployment

```bash
# Create namespace and apply manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/storage.yaml
kubectl apply -f k8s/deployments.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/ingress.yaml

# Check deployment status
kubectl get pods -n agentic-e2e-tester
kubectl get services -n agentic-e2e-tester
```

### Configuration

Update the following before deployment:

1. **Secrets** (`k8s/secrets.yaml`):
   ```bash
   # Generate base64 encoded secrets
   echo -n "your-password" | base64
   ```

2. **Ingress** (`k8s/ingress.yaml`):
   ```yaml
   spec:
     rules:
     - host: your-domain.com  # Replace with your domain
   ```

3. **Storage Classes**:
   Update `storageClassName` in `k8s/storage.yaml` to match your cluster.

## Helm Chart Deployment

### Prerequisites

- Helm 3.8+
- Kubernetes cluster
- Access to required container registries

### Installation

```bash
# Add required Helm repositories
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Install the chart
helm install agentic-tester ./helm/agentic-e2e-tester \
  --namespace agentic-e2e-tester \
  --create-namespace \
  --values ./helm/agentic-e2e-tester/values.yaml
```

### Custom Configuration

Create a custom values file:

```yaml
# custom-values.yaml
ingress:
  enabled: true
  hosts:
    - host: agentic-tester.yourdomain.com
      paths:
        - path: /
          pathType: Prefix

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

postgresql:
  auth:
    password: "your-secure-password"

secrets:
  postgresPassword: "your-secure-password"
  apiKey: "your-api-key"
```

Deploy with custom values:

```bash
helm install agentic-tester ./helm/agentic-e2e-tester \
  --namespace agentic-e2e-tester \
  --create-namespace \
  --values custom-values.yaml
```

### Upgrading

```bash
# Upgrade the deployment
helm upgrade agentic-tester ./helm/agentic-e2e-tester \
  --namespace agentic-e2e-tester \
  --values custom-values.yaml

# Check upgrade status
helm status agentic-tester -n agentic-e2e-tester
```

## Production Considerations

### Security

1. **TLS/SSL Configuration**:
   ```bash
   # Generate TLS certificates
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout tls.key -out tls.crt \
     -subj "/CN=agentic-tester.yourdomain.com"
   
   # Create TLS secret
   kubectl create secret tls agentic-tls-secret \
     --cert=tls.crt --key=tls.key \
     -n agentic-e2e-tester
   ```

2. **Network Policies**:
   ```yaml
   # Enable network policies in values.yaml
   networkPolicy:
     enabled: true
     ingress:
       enabled: true
     egress:
       enabled: true
   ```

3. **RBAC Configuration**:
   ```yaml
   # Configure service account permissions
   serviceAccount:
     create: true
     annotations:
       eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/agentic-tester-role
   ```

### High Availability

1. **Multi-replica Deployment**:
   ```yaml
   replicaCount: 3
   
   autoscaling:
     enabled: true
     minReplicas: 3
     maxReplicas: 20
   
   podDisruptionBudget:
     enabled: true
     minAvailable: 2
   ```

2. **Database High Availability**:
   ```yaml
   postgresql:
     architecture: replication
     replication:
       enabled: true
       numSynchronousReplicas: 1
   ```

3. **Load Balancing**:
   ```yaml
   service:
     type: LoadBalancer
     annotations:
       service.beta.kubernetes.io/aws-load-balancer-type: nlb
   ```

### Performance Optimization

1. **Resource Allocation**:
   ```yaml
   resources:
     limits:
       cpu: 4000m
       memory: 8Gi
     requests:
       cpu: 1000m
       memory: 4Gi
   
   ollama:
     resources:
       limits:
         cpu: 8000m
         memory: 16Gi
   ```

2. **Storage Optimization**:
   ```yaml
   postgresql:
     primary:
       persistence:
         storageClass: fast-ssd
         size: 100Gi
   
   ollama:
     persistence:
       storageClass: fast-ssd
       size: 200Gi
   ```

3. **JVM Tuning**:
   ```yaml
   env:
     JAVA_OPTS: >-
       -Xmx6g -Xms3g
       -XX:+UseG1GC
       -XX:+UseContainerSupport
       -XX:MaxRAMPercentage=75.0
       -XX:+HeapDumpOnOutOfMemoryError
   ```

## Monitoring Setup

### Prometheus Configuration

1. **Service Discovery**:
   ```yaml
   prometheus:
     server:
       global:
         scrape_interval: 15s
       scrape_configs:
         - job_name: 'agentic-e2e-tester'
           kubernetes_sd_configs:
             - role: pod
   ```

2. **Alerting Rules**:
   ```yaml
   prometheus:
     serverFiles:
       alerting_rules.yml:
         groups:
           - name: agentic-alerts
             rules:
               - alert: HighTestFailureRate
                 expr: rate(agentic_tests_failed_total[5m]) > 0.1
   ```

### Grafana Dashboards

1. **Import Dashboards**:
   ```bash
   # Copy dashboard JSON files
   kubectl create configmap grafana-dashboards \
     --from-file=monitoring/grafana/dashboards/ \
     -n agentic-e2e-tester
   ```

2. **Configure Data Sources**:
   ```yaml
   grafana:
     datasources:
       datasources.yaml:
         apiVersion: 1
         datasources:
           - name: Prometheus
             type: prometheus
             url: http://prometheus:9090
   ```

### Log Aggregation

1. **ELK Stack Integration**:
   ```yaml
   # Add to deployment
   spec:
     template:
       spec:
         containers:
         - name: filebeat
           image: elastic/filebeat:8.5.0
           volumeMounts:
           - name: logs
             mountPath: /app/logs
   ```

2. **Structured Logging**:
   ```yaml
   logging:
     pattern:
       console: '{"timestamp":"%d","level":"%level","logger":"%logger","message":"%msg"}%n'
   ```

## Troubleshooting

### Common Issues

1. **Pod Startup Issues**:
   ```bash
   # Check pod status
   kubectl describe pod <pod-name> -n agentic-e2e-tester
   
   # Check logs
   kubectl logs <pod-name> -n agentic-e2e-tester --previous
   ```

2. **Service Discovery Problems**:
   ```bash
   # Test service connectivity
   kubectl exec -it <pod-name> -n agentic-e2e-tester -- \
     curl http://ollama-service:11434/api/tags
   ```

3. **Resource Constraints**:
   ```bash
   # Check resource usage
   kubectl top pods -n agentic-e2e-tester
   kubectl top nodes
   ```

4. **Storage Issues**:
   ```bash
   # Check PVC status
   kubectl get pvc -n agentic-e2e-tester
   
   # Check storage class
   kubectl get storageclass
   ```

### Health Checks

1. **Application Health**:
   ```bash
   curl http://agentic-tester-service:8081/actuator/health
   ```

2. **Database Connectivity**:
   ```bash
   kubectl exec -it postgres-pod -n agentic-e2e-tester -- \
     psql -U agentic_user -d vectordb -c "SELECT 1;"
   ```

3. **AI Model Status**:
   ```bash
   curl http://ollama-service:11434/api/tags
   ```

### Performance Tuning

1. **JVM Memory Analysis**:
   ```bash
   # Enable heap dumps
   kubectl exec -it <pod-name> -n agentic-e2e-tester -- \
     jcmd <pid> GC.run_finalization
   ```

2. **Database Performance**:
   ```sql
   -- Check slow queries
   SELECT query, mean_time, calls 
   FROM pg_stat_statements 
   ORDER BY mean_time DESC 
   LIMIT 10;
   ```

3. **Kafka Monitoring**:
   ```bash
   # Check consumer lag
   kubectl exec -it kafka-pod -n agentic-e2e-tester -- \
     kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
     --describe --group agentic-e2e-tester
   ```

## Backup and Recovery

### Database Backup

```bash
# Automated backup script
kubectl create job postgres-backup --from=cronjob/postgres-backup
```

### Configuration Backup

```bash
# Backup Kubernetes resources
kubectl get all,configmap,secret,pvc -n agentic-e2e-tester -o yaml > backup.yaml
```

### Disaster Recovery

1. **Data Recovery**:
   ```bash
   # Restore from backup
   kubectl apply -f backup.yaml
   ```

2. **Service Recovery**:
   ```bash
   # Rolling restart
   kubectl rollout restart deployment/agentic-tester -n agentic-e2e-tester
   ```

For more detailed troubleshooting, see the [Troubleshooting Guide](../troubleshooting.md).