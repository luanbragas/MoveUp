#!/bin/sh
# Cria os buckets locais que imitam o S3 sa-east-1 (ARQUITETURA.md, seção 6).
# Roda com o aws-cli oficial contra o RustFS (S3 local); idempotente.
set -eu

s3api() { aws --endpoint-url "$S3_ENDPOINT" s3api "$@"; }

# O RustFS leva alguns segundos para aceitar conexões.
tries=0
until s3api list-buckets > /dev/null 2>&1; do
  tries=$((tries + 1))
  if [ "$tries" -ge 30 ]; then
    echo "s3-init: S3 local não respondeu" >&2
    exit 1
  fi
  sleep 1
done

bucket_exists() { s3api head-bucket --bucket "$1" > /dev/null 2>&1; }

# Fotos de evolução: privado; incoming/ apagado após 1 dia, processed/ é a única origem de leitura.
bucket_exists moveup-photos || s3api create-bucket --bucket moveup-photos > /dev/null
s3api put-bucket-lifecycle-configuration --bucket moveup-photos --lifecycle-configuration \
  '{"Rules":[{"ID":"expire-incoming","Status":"Enabled","Filter":{"Prefix":"incoming/"},"Expiration":{"Days":1}}]}'

# Arquivo frio do audit_log: Object Lock (retenção imutável). Só dá para ligar na criação.
bucket_exists moveup-audit-archive \
  || s3api create-bucket --bucket moveup-audit-archive --object-lock-enabled-for-bucket > /dev/null

echo "s3-init: buckets prontos"
