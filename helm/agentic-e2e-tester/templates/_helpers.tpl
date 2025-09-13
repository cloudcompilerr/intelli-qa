{{/*
Expand the name of the chart.
*/}}
{{- define "agentic-e2e-tester.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "agentic-e2e-tester.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "agentic-e2e-tester.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "agentic-e2e-tester.labels" -}}
helm.sh/chart: {{ include "agentic-e2e-tester.chart" . }}
{{ include "agentic-e2e-tester.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "agentic-e2e-tester.selectorLabels" -}}
app.kubernetes.io/name: {{ include "agentic-e2e-tester.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "agentic-e2e-tester.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "agentic-e2e-tester.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the secret to use
*/}}
{{- define "agentic-e2e-tester.secretName" -}}
{{- if .Values.secrets.create }}
{{- printf "%s-secrets" (include "agentic-e2e-tester.fullname" .) }}
{{- else }}
{{- .Values.secrets.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the configmap to use
*/}}
{{- define "agentic-e2e-tester.configMapName" -}}
{{- printf "%s-config" (include "agentic-e2e-tester.fullname" .) }}
{{- end }}

{{/*
Create Ollama service name
*/}}
{{- define "agentic-e2e-tester.ollamaServiceName" -}}
{{- if .Values.ollama.enabled }}
{{- printf "%s-ollama" (include "agentic-e2e-tester.fullname" .) }}
{{- else }}
{{- .Values.ollama.externalService }}
{{- end }}
{{- end }}

{{/*
Create PostgreSQL service name
*/}}
{{- define "agentic-e2e-tester.postgresqlServiceName" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "%s-postgresql" .Release.Name }}
{{- else }}
{{- .Values.postgresql.externalService }}
{{- end }}
{{- end }}

{{/*
Create Kafka service name
*/}}
{{- define "agentic-e2e-tester.kafkaServiceName" -}}
{{- if .Values.kafka.enabled }}
{{- printf "%s-kafka" .Release.Name }}
{{- else }}
{{- .Values.kafka.externalService }}
{{- end }}
{{- end }}

{{/*
Create Redis service name
*/}}
{{- define "agentic-e2e-tester.redisServiceName" -}}
{{- if .Values.redis.enabled }}
{{- printf "%s-redis-master" .Release.Name }}
{{- else }}
{{- .Values.redis.externalService }}
{{- end }}
{{- end }}

{{/*
Create Couchbase service name
*/}}
{{- define "agentic-e2e-tester.couchbaseServiceName" -}}
{{- if .Values.couchbase.enabled }}
{{- printf "%s-couchbase" (include "agentic-e2e-tester.fullname" .) }}
{{- else }}
{{- .Values.couchbase.externalService }}
{{- end }}
{{- end }}

{{/*
Generate certificates for TLS
*/}}
{{- define "agentic-e2e-tester.gen-certs" -}}
{{- $altNames := list ( printf "%s.%s" (include "agentic-e2e-tester.fullname" .) .Release.Namespace ) ( printf "%s.%s.svc" (include "agentic-e2e-tester.fullname" .) .Release.Namespace ) -}}
{{- $ca := genCA "agentic-e2e-tester-ca" 365 -}}
{{- $cert := genSignedCert ( include "agentic-e2e-tester.fullname" . ) nil $altNames 365 $ca -}}
tls.crt: {{ $cert.Cert | b64enc }}
tls.key: {{ $cert.Key | b64enc }}
ca.crt: {{ $ca.Cert | b64enc }}
{{- end }}