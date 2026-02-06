# Certificate-Based Authentication Summary

## What Changed

The project now uses **pure certificate-based authentication** instead of password-based authentication for the producer and consumer applications.

## Why This is Better

1. **More Secure**: No passwords stored or transmitted
2. **Simpler**: Certificates serve dual purpose (encryption + authentication)
3. **Industry Standard**: Following TLS mutual authentication best practices
4. **True mTLS**: Leveraging the full power of mutual TLS

## How It Works

### Authentication Flow

1. **Client Connects**: Producer/Consumer initiates TLS connection with client certificate
2. **Certificate Verification**: RabbitMQ verifies the certificate is signed by the trusted CA
3. **CN Extraction**: RabbitMQ extracts the Common Name (CN) from the certificate
4. **User Mapping**: CN is matched to a user in RabbitMQ (CN must equal username)
5. **Authorization**: User's permissions are applied

### Certificate to User Mapping

| Certificate | Common Name (CN) | RabbitMQ User | Authentication Method |
|-------------|------------------|---------------|----------------------|
| `producer-cert.pem` | `producer` | `producer` | Certificate (EXTERNAL) |
| `consumer-cert.pem` | `consumer` | `consumer` | Certificate (EXTERNAL) |
| N/A | `admin` | `admin` | Password (management UI only) |

## Configuration Changes

### 1. RabbitMQ Configuration (`rabbitmq.conf`)

Added certificate-based authentication settings:

```conf
# Enable certificate-based authentication
auth_mechanisms.1 = EXTERNAL
auth_backends.1 = rabbit_auth_backend_internal

# Map certificate CN to username
ssl_cert_login_from = common_name
```

**What this does:**
- `EXTERNAL`: Use TLS client certificate for authentication
- `rabbit_auth_backend_internal`: Still use RabbitMQ's internal user database for permissions
- `ssl_cert_login_from = common_name`: Extract username from certificate's CN field

### 2. User Definitions (`definitions.json`)

Removed passwords for producer and consumer:

```json
{
  "name": "producer",
  "password": "",
  "tags": ["producer"]
}
```

**Why empty password?**
- Certificate authentication doesn't use passwords
- Users are authenticated by certificate CN, not credentials
- Admin still has password for management UI access

### 3. Spring Boot Configurations

Removed username and password from `application.yaml`:

**Before:**
```yaml
spring:
  rabbitmq:
    username: producer
    password: producer-pass
```

**After:**
```yaml
spring:
  rabbitmq:
    # No username/password - authenticated via certificate
```

## Security Benefits

### Traditional Password-Based Auth
- ❌ Passwords can be leaked
- ❌ Passwords stored in config files
- ❌ Need to manage password rotation
- ❌ Weak passwords can be brute-forced

### Certificate-Based Auth
- ✅ No passwords to leak or manage
- ✅ Certificates are cryptographically strong
- ✅ Certificate expiration forces rotation
- ✅ Immune to brute-force attacks
- ✅ Leverages existing PKI infrastructure

## Testing

After restarting RabbitMQ and the applications:

```bash
# Restart RabbitMQ
docker-compose down
docker-compose up -d

# Start applications (they will authenticate with certificates)
./mvnw spring-boot:run -pl consumer
./mvnw spring-boot:run -pl producer

# Test message flow
curl -X POST http://localhost:8080/messages \
  -H "Content-Type: application/json" \
  -d '"Certificate-based auth works!"'
```

## How to Verify

Check RabbitMQ logs for successful certificate authentication:

```bash
docker-compose logs rabbitmq | grep -i "accepted"
```

You should see connections accepted with the certificate CNs:
```
connection <...> (... -> ...:5671): user 'producer' authenticated using EXTERNAL mechanism
connection <...> (... -> ...:5671): user 'consumer' authenticated using EXTERNAL mechanism
```

## Admin Access

The admin user still uses password authentication for the management UI:
- Username: `admin`
- Password: `admin`
- URL: `https://localhost:15671`

This is intentional - certificate-based auth is for the AMQP protocol. The management UI can use traditional login.

## Important Notes

1. **CN Must Match Username**: The certificate's Common Name MUST exactly match a user in RabbitMQ
2. **Certificate Must Be Valid**: Expired or untrusted certificates will be rejected
3. **CA Must Be Trusted**: Client certificates must be signed by the CA in `ca-cert.pem`
4. **No Password Fallback**: If certificate auth fails, connection is rejected (no password fallback)

## Troubleshooting

### Connection Refused
- **Check**: Certificate CN matches RabbitMQ username
- **Verify**: User exists in definitions.json
- **Confirm**: Certificate is signed by the trusted CA

### Authentication Failed
```bash
# Verify certificate CN
openssl x509 -in certs/producer-cert.pem -noout -subject
# Should show: subject=CN = producer

# Verify certificate is valid
openssl verify -CAfile certs/ca-cert.pem certs/producer-cert.pem
# Should show: certs/producer-cert.pem: OK
```

### Check RabbitMQ Auth Settings
```bash
docker exec rabbitmq-tls rabbitmq-diagnostics status | grep auth
```

## References

- [RabbitMQ TLS Documentation](https://www.rabbitmq.com/ssl.html)
- [RabbitMQ Authentication Mechanisms](https://www.rabbitmq.com/authentication.html)
- [X.509 Certificate Authentication](https://www.rabbitmq.com/ssl.html#peer-verification)

