{{/*
Expand the name of the chart.
*/}}
{{- define "fnflow-json-processors-kafka.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "fnflow-json-processors-kafka.fullname" -}}
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
{{- define "fnflow-json-processors-kafka.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "fnflow-json-processors-kafka.labels" -}}
helm.sh/chart: {{ include "fnflow-json-processors-kafka.chart" . }}
{{ include "fnflow-json-processors-kafka.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "fnflow-json-processors-kafka.selectorLabels" -}}
app.kubernetes.io/name: {{ include "fnflow-json-processors-kafka.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Args formatted from yaml
*/}}

{{- define "recurseFlattenMap" -}}
{{- $map := first . -}}
{{- $label := last . -}}
{{- range $key, $val := $map -}}
  {{- $sublabel := list $label $key | join "." -}}
  {{- if kindOf $val | eq "map" -}}
    {{- list $val $sublabel | include "recurseFlattenMap" -}}
  {{- else -}}
- {{ list "--" $sublabel "=" $val | join "" | quote }}
{{ end -}}
{{- end -}}
{{- end -}}
