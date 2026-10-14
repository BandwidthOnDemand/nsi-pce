{{/*
Expand the name of the chart.
*/}}
{{- define "nsi-pce.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "nsi-pce.fullname" -}}
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
{{- define "nsi-pce.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "nsi-pce.labels" -}}
helm.sh/chart: {{ include "nsi-pce.chart" . }}
{{ include "nsi-pce.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "nsi-pce.selectorLabels" -}}
app.kubernetes.io/name: {{ include "nsi-pce.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "nsi-pce.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "nsi-pce.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/* Fail closed: a known outbound TLS mode, with the Secrets that mode needs. */}}
{{- define "nsi-pce.validateTls" -}}
{{- $client := .Values.config.tls.client -}}
{{- if not (has $client.mode (list "" "pem" "keystore")) -}}
{{- fail (printf "config.tls.client.mode must be pem, keystore or empty, not %q" $client.mode) -}}
{{- end -}}
{{- if eq $client.mode "pem" -}}
{{- $_ := required "config.tls.client.pem.identitySecretName is required" $client.pem.identitySecretName -}}
{{- $_ := required "config.tls.client.pem.caSecretName is required" $client.pem.caSecretName -}}
{{- end -}}
{{- if eq $client.mode "keystore" -}}
{{- $_ := required "config.tls.client.keystore.secretName is required" $client.keystore.secretName -}}
{{- $_ := required "config.tls.client.keystore.passwordSecretName is required" $client.keystore.passwordSecretName -}}
{{- end -}}
{{- end }}
