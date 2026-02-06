# Quick Start Guide

## Overview
This is a multi-module Maven project demonstrating secure communication between Spring Boot applications using RabbitMQ with TLS authentication.

## Architecture
- **Producer**: REST API (port 8080) that sends messages to `input.queue` and listens for enriched messages on `output.queue`
- **Consumer**: Background service (port 8081) that listens to `input.queue`, enriches messages, and sends them to `output.queue`
- **RabbitMQ**: Message broker with TLS/SSL (port 5671) and management UI (port 15671)

## Quick Setup

### 1. Generate Certificates
```bash
./generate-certs.sh
```

This creates all necessary PEM certificates in the `certs/` directory.

### 2. Start RabbitMQ
```bash
docker-compose up -d
```

Wait ~10 seconds for RabbitMQ to fully start.

### 3. Start Applications

Terminal 1 - Consumer:
```bash
./mvnw spring-boot:run -pl consumer
```

Terminal 2 - Producer:
```bash
./mvnw spring-boot:run -pl producer
```

### 4. Test the System
```bash
curl -X POST http://localhost:8080/messages \
  -H "Content-Type: application/json" \
  -d '"Hello from producer!"'
```

### 5. Check Logs
- **Producer logs**: You'll see the message being sent to input.queue and received from output.queue (enriched)
- **Consumer logs**: You'll see the message being received, enriched, and sent to output.queue

## Project Structure
```
spring-boot-rabbitmq-tls/
├── pom.xml                         # Parent POM
├── producer/                       # Producer module
│   ├── src/main/java/
│   │   └── me/matteogiordano/producer/
│   │       ├── ProducerApplication.java    # Main class
│   │       ├── Message.java                # DTO record
│   │       ├── MessageController.java      # REST endpoint
│   │       └── OutputListener.java         # Listens to output.queue
│   └── src/main/resources/
│       └── application.yaml                # SSL bundle config
├── consumer/                       # Consumer module
│   ├── src/main/java/
│   │   └── me/matteogiordano/consumer/
│   │       ├── ConsumerApplication.java    # Main class
│   │       ├── Message.java                # DTO record
│   │       └── MessageListener.java        # Processes messages
│   └── src/main/resources/
│       └── application.yaml                # SSL bundle config
├── docker-compose.yml              # RabbitMQ with TLS
├── rabbitmq/
│   ├── definitions.json            # Queues, users, permissions
│   └── rabbitmq.conf              # TLS configuration
├── generate-certs.sh              # Certificate generation script
└── README.md                      # Full documentation
```

## Key Features

### Security
- ✅ Mutual TLS (mTLS) authentication
- ✅ Separate client certificates for producer and consumer
- ✅ PEM format (no keystores or passwords needed)
- ✅ Spring Boot 3 SSL bundles

### RabbitMQ Configuration
- ✅ Explicit queue definitions via `definitions.json`
- ✅ User-level permissions (producer can only write to input/read from output)
- ✅ Durable queues
- ✅ TLS on both AMQP and management ports

### Code Style
- ✅ Minimal, idiomatic Spring Boot code
- ✅ Java records for DTOs
- ✅ Constructor injection
- ✅ Proper logging with SLF4J

## Message Flow

1. POST to `http://localhost:8080/messages` with JSON body
2. Producer creates `Message` object and sends to `input.queue`
3. Consumer receives message from `input.queue`
4. Consumer enriches message (sets `enrichedBy = "consumer"`)
5. Consumer sends enriched message to `output.queue`
6. Producer receives enriched message from `output.queue` and logs it

## Configuration Details

### Producer (application.yaml)
```yaml
spring:
  rabbitmq:
    ssl:
      enabled: true
      bundle: producer-bundle
  ssl:
    bundle:
      pem:
        producer-bundle:
          truststore:
            certificate: file:certs/ca-cert.pem
          keystore:
            certificate: file:certs/producer-cert.pem
            private-key: file:certs/producer-key.pem
```

### RabbitMQ Users & Authentication
- **Admin**: Password-based (username: `admin`, password: `admin`) - for management UI
- **Producer**: Certificate-based (CN=`producer`) - authenticated via client certificate
- **Consumer**: Certificate-based (CN=`consumer`) - authenticated via client certificate

**Authentication Method**: Applications use their TLS client certificates for authentication. RabbitMQ extracts the Common Name (CN) from the certificate and maps it to the corresponding user.

**Management UI**: Access at `https://localhost:15671` (use admin credentials for full access)

## Troubleshooting

### RabbitMQ not starting?
```bash
docker-compose logs rabbitmq
```

### Certificate errors?
Regenerate certificates:
```bash
rm -rf certs/
./generate-certs.sh
```

### Applications not connecting?
1. Verify RabbitMQ is running: `docker-compose ps`
2. Check certificates exist: `ls -la certs/`
3. Review application logs for SSL errors

## Clean Up
```bash
# Stop applications (Ctrl+C in their terminals)
docker-compose down
```

## Technology Stack
- Java 17
- Spring Boot 3.5.10
- Spring AMQP
- RabbitMQ 3.13
- Docker & Docker Compose
- Maven (multi-module)

## Next Steps
- Add message validation
- Implement dead letter queues
- Add retry logic with exponential backoff
- Create integration tests with Testcontainers
- Add monitoring with Micrometer/Prometheus

For detailed documentation, see [README.md](README.md).

