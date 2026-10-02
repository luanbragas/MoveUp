#!/bin/sh
# Cria os buckets locais que imitam o S3 sa-east-1 (ARQUITETURA.md, seção 6).
set -eu
mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# Fotos de evolução: privado; incoming/ apagado após 1 dia, processed/ é a única origem de leitura.
mc mb --ignore-existing local/moveup-photos
mc anonymous set none local/moveup-photos
mc ilm rule add --prefix "incoming/" --expire-days 1 local/moveup-photos || true

# Arquivo frio do audit_log: Object Lock (retenção imutável).
mc mb --ignore-existing --with-lock local/moveup-audit-archive
mc anonymous set none local/moveup-audit-archive

echo "minio-init: buckets prontos"
