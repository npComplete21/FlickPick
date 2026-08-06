#!/bin/bash
# Generates the RSA key pair User Service uses to sign JWTs.
# Public key will eventually be copied to Import/Ranking Services to verify tokens.
set -e
cd "$(dirname "$0")/src/main/resources/certs"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
openssl rsa -pubout -in private.pem -out public.pem
echo "Keys written to $(pwd)"
