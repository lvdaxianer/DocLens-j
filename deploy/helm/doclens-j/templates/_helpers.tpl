{{/*
Expand the chart name.
*/}}
{{- define "doclens-j.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "doclens-j.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Common labels.
*/}}
{{- define "doclens-j.labels" -}}
helm.sh/chart: {{ include "doclens-j.chart" . }}
{{ include "doclens-j.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Chart label.
*/}}
{{- define "doclens-j.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Selector labels.
*/}}
{{- define "doclens-j.selectorLabels" -}}
app.kubernetes.io/name: {{ include "doclens-j.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Service account name.
*/}}
{{- define "doclens-j.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "doclens-j.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/*
Application secret name.
*/}}
{{- define "doclens-j.appSecretName" -}}
{{- default (printf "%s-app" (include "doclens-j.fullname" .)) .Values.app.database.existingSecret -}}
{{- end -}}

{{/*
PostgreSQL service name.
*/}}
{{- define "doclens-j.postgresqlFullname" -}}
{{- printf "%s-postgresql" (include "doclens-j.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Application database URL.
*/}}
{{- define "doclens-j.databaseUrl" -}}
{{- if .Values.postgresql.enabled -}}
{{- printf "jdbc:postgresql://%s:%v/%s" (include "doclens-j.postgresqlFullname" .) .Values.postgresql.service.port .Values.postgresql.database -}}
{{- else -}}
{{- required "app.database.url is required when postgresql.enabled=false" .Values.app.database.url -}}
{{- end -}}
{{- end -}}

{{/*
Application database username.
*/}}
{{- define "doclens-j.databaseUsername" -}}
{{- if .Values.postgresql.enabled -}}
{{- .Values.postgresql.username -}}
{{- else -}}
{{- .Values.app.database.username -}}
{{- end -}}
{{- end -}}
