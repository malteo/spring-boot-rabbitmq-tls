# Spring Boot RabbitMQ with TLS

A multi-module Maven project demonstrating secure communication between Spring Boot applications using RabbitMQ with TLS/SSL authentication using PEM certificates.

## Quick Start

Get up and running in two commands:

```bash
./mvnw package && docker compose up
```

This will:
1. Build both Spring Boot applications and create Docker images
2. Start RabbitMQ with TLS configuration
3. Start the producer (port 8080) and consumer services
4. All services communicate securely using TLS certificates

**Prerequisites**: Docker and Docker Compose installed. The certificates are already generated and included in the `certs/` directory.

### Test the System

Once all services are running:

```bash
curl -X POST http://localhost:8080/messages \
  -H "Content-Type: application/json" \
  -d '"Hello RabbitMQ with TLS!"'
```

You'll see the message flow through the system in the Docker logs.

## Project Structure

```
spring-boot-rabbitmq-tls/
├── pom.xml                     # Parent POM
├── producer/                   # Producer application
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── me/matteogiordano/producer/
│       │   │       ├── ProducerApplication.java
│       │   │       ├── Message.java
│       │   │       ├── MessageController.java
│       │   │       └── OutputListener.java
│       │   └── resources/
│       │       └── application.yaml
│       └── test/
├── consumer/                   # Consumer application
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── me/matteogiordano/consumer/
│       │   │       ├── ConsumerApplication.java
│       │   │       ├── Message.java
│       │   │       └── MessageListener.java
│       │   └── resources/
│       │       └── application.yaml
│       └── test/
├── docker-compose.yml          # RabbitMQ container configuration
├── rabbitmq/
│   ├── definitions.json        # RabbitMQ queues, users, permissions
│   └── rabbitmq.conf          # RabbitMQ TLS configuration
└── certs/                      # TLS certificates (generated)
```

## Message Flow

1. **Producer** receives HTTP POST request at `/messages`
2. **Producer** sends JSON message to `input.queue`
3. **Consumer** listens on `input.queue`, enriches message (sets `enrichedBy` field)
4. **Consumer** sends enriched message to `output.queue`
5. **Producer** listens on `output.queue` and logs received enriched message

## TLS Certificates

**Note**: This project includes pre-generated TLS certificates in the `certs/` directory, so you can run the application immediately without generating certificates first.

If you need to regenerate certificates (e.g., for production use or if certificates have expired):

### Quick Start (Recommended)

Use the provided script to generate all certificates automatically:

```bash
./generate-certs.sh
```

This script will create all necessary certificates in the `certs/` directory and verify them.

### Manual Generation (Step-by-Step)

If you prefer to generate certificates manually or want to understand the process:

#### Prerequisites

- OpenSSL installed on your system

### Step 1: Create Certificates Directory

```bash
mkdir -p certs
cd certs
```

### Step 2: Generate Certificate Authority (CA)

Generate a private key for the CA:

```bash
openssl genrsa -out ca-key.pem 4096
```

Create a self-signed CA certificate (valid for 10 years):

```bash
openssl req -new -x509 -days 3650 -key ca-key.pem -out ca-cert.pem \
  -subj "/CN=MyCA"
```

### Step 3: Generate RabbitMQ Server Certificate

Generate server private key:

```bash
openssl genrsa -out server-key.pem 4096
```

Create a certificate signing request (CSR):

```bash
openssl req -new -key server-key.pem -out server.csr \
  -subj "/CN=localhost"
```

Create a configuration file for Subject Alternative Name (SAN):

```bash
cat > server-ext.cnf << EOF
subjectAltName = DNS:localhost,DNS:rabbitmq,DNS:rabbitmq-tls,IP:127.0.0.1
EOF
```

Sign the server certificate with the CA (valid for 365 days):

```bash
openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem \
  -CAcreateserial -out server-cert.pem -days 365 \
  -extfile server-ext.cnf
```

Clean up temporary files:

```bash
rm server.csr server-ext.cnf
```

### Step 4: Generate Producer Client Certificate

Generate producer private key:

```bash
openssl genrsa -out producer-key.pem 4096
```

Create CSR for producer:

```bash
openssl req -new -key producer-key.pem -out producer.csr \
  -subj "/CN=producer"
```

Sign the producer certificate with the CA:

```bash
openssl x509 -req -in producer.csr -CA ca-cert.pem -CAkey ca-key.pem \
  -CAcreateserial -out producer-cert.pem -days 365
```

Clean up:

```bash
rm producer.csr
```

### Step 5: Generate Consumer Client Certificate

Generate consumer private key:

```bash
openssl genrsa -out consumer-key.pem 4096
```

Create CSR for consumer:

```bash
openssl req -new -key consumer-key.pem -out consumer.csr \
  -subj "/CN=consumer"
```

Sign the consumer certificate with the CA:

```bash
openssl x509 -req -in consumer.csr -CA ca-cert.pem -CAkey ca-key.pem \
  -CAcreateserial -out consumer-cert.pem -days 365
```

Clean up:

```bash
rm consumer.csr ca-cert.srl
```

### Step 6: Verify Certificates

Verify the certificate chain:

```bash
# Verify server certificate
openssl verify -CAfile ca-cert.pem server-cert.pem

# Verify producer certificate
openssl verify -CAfile ca-cert.pem producer-cert.pem

# Verify consumer certificate
openssl verify -CAfile ca-cert.pem consumer-cert.pem
```

Return to project root:

```bash
cd ..
```

Your `certs/` directory should now contain:
- `ca-cert.pem` - CA certificate
- `ca-key.pem` - CA private key
- `server-cert.pem` - RabbitMQ server certificate
- `server-key.pem` - RabbitMQ server private key
- `producer-cert.pem` - Producer client certificate
- `producer-key.pem` - Producer client private key
- `consumer-cert.pem` - Consumer client certificate
- `consumer-key.pem` - Consumer client private key

## Build the Project

Build both modules and create Docker images:

```bash
./mvnw package
```

This will:
- Compile and package both producer and consumer applications
- Create Docker images: `sbrt-producer` and `sbrt-consumer`
- Run unit tests

## Run the Applications

### Option 1: Docker Compose (Recommended)

Start all services with a single command:

```bash
docker compose up
```

This will start:
- **RabbitMQ** with TLS on port 5671 (management UI on port 15671)
- **Producer** application on port 8080
- **Consumer** application

All services communicate securely using TLS certificates. The applications will automatically connect to RabbitMQ once it's healthy.

To run in detached mode:
```bash
docker compose up -d
```

View logs:
```bash
docker compose logs -f
```

Stop all services:
```bash
docker compose down
```

### Option 2: Run Locally with Spring Boot Maven Plugin

If you prefer to run the applications locally (not in Docker):

#### Step 1: Start RabbitMQ with TLS

Start only RabbitMQ using Docker Compose:

```bash
docker compose up -d rabbitmq
```

Wait for RabbitMQ to be ready (check with `docker compose logs -f rabbitmq`).

#### Step 2: Start Consumer Application

In a new terminal:

```bash
./mvnw spring-boot:run -pl consumer
```

The consumer will start on port 8081 and listen for messages on `input.queue`.

#### Step 3: Start Producer Application

In another terminal:

```bash
./mvnw spring-boot:run -pl producer
```

The producer will start on port 8080 and expose a REST endpoint.

## Test the Application

Send a message via the producer's REST endpoint:

```bash
curl -X POST http://localhost:8080/messages \
  -H "Content-Type: application/json" \
  -d '"Hello RabbitMQ with TLS!"'
```

### Expected Behavior

1. **Producer logs** will show:
   - Sending message to input queue
   - Receiving enriched message from output queue (with `enrichedBy: "consumer"`)

2. **Consumer logs** will show:
   - Receiving message from input queue
   - Sending enriched message to output queue

## RabbitMQ Management UI

Access the RabbitMQ Management UI over HTTPS:

```
https://localhost:15671
```

**Note**: You'll need to accept the self-signed certificate warning in your browser.

Login credentials:
- Username: `admin` / Password: `admin` (full access to all queues and management features)
- For producer/consumer: Use client certificate authentication (no password needed when connecting via TLS)

## Security Configuration

### Certificate-Based Authentication

This setup uses **certificate-based authentication** for the Spring Boot applications:

- **Producer** and **Consumer** applications authenticate using their TLS client certificates
- RabbitMQ identifies users by the **Common Name (CN)** from the certificate
- No passwords required for producer/consumer connections
- The certificate CN must match the username in RabbitMQ definitions

Certificate to User Mapping:
- Certificate with CN=`producer` → RabbitMQ user `producer`
- Certificate with CN=`consumer` → RabbitMQ user `consumer`

### Users and Permissions

The RabbitMQ setup includes three users with different permission levels:

**Admin User:**
- Username: `admin`
- Password: `admin` (only for management console)
- Tags: `administrator`
- Permissions: Full access to all resources (configure, write, read: `.*`)
- Purpose: Management console access and administrative tasks

**Producer User:**
- Username: `producer` (authenticated via certificate CN)
- Authentication: Client certificate (no password)
- Tags: `producer`
- Permissions: Can write to `messages.exchange` and `input.queue`, read from `output.queue`

**Consumer User:**
- Username: `consumer` (authenticated via certificate CN)
- Authentication: Client certificate (no password)
- Tags: `consumer`
- Permissions: Can read from `input.queue`, write to `messages.exchange` and `output.queue`

### TLS Configuration

- **RabbitMQ** uses mutual TLS (mTLS) with client certificate verification
- **Authentication mechanism**: EXTERNAL (certificate-based)
- **User identification**: Common Name (CN) from client certificate
- **Spring Boot 3** SSL bundles with PEM format (no keystores/passwords needed)
- All communication is encrypted using TLS 1.2+

### How It Works

1. **Client presents certificate**: Producer/Consumer connects with their client certificate
2. **RabbitMQ verifies certificate**: Checks it's signed by the trusted CA
3. **RabbitMQ extracts CN**: Reads the Common Name from the certificate (e.g., "producer")
4. **RabbitMQ maps to user**: Matches CN to an existing user in the system
5. **Permissions applied**: User gets their configured permissions

## Troubleshooting

### Certificate Verification Failed

If you see SSL/TLS errors, verify:
1. All certificates are in the `certs/` directory
2. Certificates are valid (check expiration with `openssl x509 -in <cert>.pem -noout -dates`)
3. Certificate chain is correct (CA signed all certificates)

### Connection Refused

Ensure RabbitMQ is running:
```bash
docker-compose ps
docker-compose logs rabbitmq
```

### Permission Denied Errors

Check RabbitMQ logs for permission issues:
```bash
docker-compose logs rabbitmq | grep -i permission
```

## Clean Up

Stop all services:

```bash
docker compose down
```

If you ran applications locally with the Spring Boot Maven plugin, stop them with Ctrl+C in their terminals, then stop RabbitMQ:

```bash
docker compose down
```

Remove generated images (optional):

```bash
docker rmi sbrt-producer sbrt-consumer
```

Remove generated certificates (if needed):

```bash
rm -rf certs/
```

## Technology Stack

- **Java 17**
- **Spring Boot 3.5.10**
- **Spring AMQP** (RabbitMQ client)
- **RabbitMQ 3.13** with TLS
- **Docker & Docker Compose**
- **Maven** (multi-module project)

## References

- [Spring Boot SSL Bundles](https://docs.spring.io/spring-boot/reference/features/ssl.html)
- [Spring AMQP Documentation](https://docs.spring.io/spring-amqp/reference/)
- [RabbitMQ TLS Configuration](https://www.rabbitmq.com/ssl.html)
- [OpenSSL Certificate Generation](https://www.openssl.org/docs/)
