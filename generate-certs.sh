#!/bin/bash

# Script to generate all TLS certificates for RabbitMQ setup
# Run from project root: ./generate-certs.sh

set -e

CERTS_DIR="certs"

echo "Creating certificates directory..."
mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

echo ""
echo "Step 1: Generating Certificate Authority (CA)..."
openssl genrsa -out ca-key.pem 4096
openssl req -new -x509 -days 3650 -key ca-key.pem -out ca-cert.pem \
  -subj "/CN=MyCA"

echo ""
echo "Step 2: Generating RabbitMQ Server Certificate..."
openssl genrsa -out server-key.pem 4096
openssl req -new -key server-key.pem -out server.csr \
  -subj "/CN=localhost"

cat > server-ext.cnf << EOF
subjectAltName = DNS:localhost,DNS:rabbitmq,DNS:rabbitmq-tls,IP:127.0.0.1
EOF

openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem \
  -CAcreateserial -out server-cert.pem -days 365 \
  -extfile server-ext.cnf

rm server.csr server-ext.cnf

echo ""
echo "Step 3: Generating Producer Client Certificate..."
openssl genrsa -out producer-key.pem 4096
openssl req -new -key producer-key.pem -out producer.csr \
  -subj "/CN=producer"
openssl x509 -req -in producer.csr -CA ca-cert.pem -CAkey ca-key.pem \
  -CAcreateserial -out producer-cert.pem -days 365
rm producer.csr

echo ""
echo "Step 4: Generating Consumer Client Certificate..."
openssl genrsa -out consumer-key.pem 4096
openssl req -new -key consumer-key.pem -out consumer.csr \
  -subj "/CN=consumer"
openssl x509 -req -in consumer.csr -CA ca-cert.pem -CAkey ca-key.pem \
  -CAcreateserial -out consumer-cert.pem -days 365
rm consumer.csr ca-cert.srl

echo ""
echo "Step 5: Verifying certificates..."
echo "  Verifying server certificate..."
openssl verify -CAfile ca-cert.pem server-cert.pem
echo "  Verifying producer certificate..."
openssl verify -CAfile ca-cert.pem producer-cert.pem
echo "  Verifying consumer certificate..."
openssl verify -CAfile ca-cert.pem consumer-cert.pem

cd ..

echo ""
echo "✅ Certificate generation complete!"
echo ""
echo "Generated files in $CERTS_DIR/:"
ls -1 "$CERTS_DIR"
echo ""
echo "Next steps:"
echo "  1. Start RabbitMQ: docker-compose up -d"
echo "  2. Start Consumer: ./mvnw spring-boot:run -pl consumer"
echo "  3. Start Producer: ./mvnw spring-boot:run -pl producer"
echo "  4. Test: curl -X POST http://localhost:8080/messages -H 'Content-Type: application/json' -d '\"Hello RabbitMQ!\"'"

