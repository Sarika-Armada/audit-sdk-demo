# Audit SDK Demo Application

A demo application that verifies the `audit-svc-java-sdk` works correctly by exercising all SDK features.

## Prerequisites

1. **Java 17+** installed
2. **Maven 3.6+** installed
3. **audit-svc-java-sdk** installed in local Maven repository

## Setup

### Install the SDK locally

Before running the demo, install the SDK to your local Maven repository:

```bash
cd ../audit-svc-java-sdk
mvn clean install -DskipTests
```

## Running the Demo

### Option 1: Command Line Demo

Run the application to execute automated SDK verification tests:

```bash
mvn spring-boot:run
```

This runs the `AuditSdkDemoRunner` which tests:
- ✅ Basic AuditLogEntry creation
- ✅ Full-featured AuditLogEntry with all fields
- ✅ JWT token handling and actor name population
- ✅ HTTP Client Builder configuration
- ✅ Batch entry creation

### Option 2: REST API Demo

The application also provides REST endpoints for interactive testing:

```bash
mvn spring-boot:run
```

Then use the following endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/demo/health` | GET | Health check |
| `/api/demo/entry/sample` | GET | Create and return a sample audit entry |
| `/api/demo/entry/create` | POST | Create a custom audit entry |
| `/api/demo/publish` | POST | Publish a single audit entry |
| `/api/demo/publish/batch` | POST | Publish a batch of entries |
| `/api/demo/publish/async` | POST | Publish asynchronously |
| `/api/demo/scopes` | GET | List available audit scopes |

### Example API Calls

```bash
# Health check
curl http://localhost:8080/api/demo/health

# Create sample entry
curl http://localhost:8080/api/demo/entry/sample

# Create custom entry
curl -X POST "http://localhost:8080/api/demo/entry/create?scope=ORGANIZATION&event=user.login&organizationId=org-123&message=User logged in"

# List available scopes
curl http://localhost:8080/api/demo/scopes
```

## Testing with Audit Service

To test actual publishing, you need the `audit-log-svc` running:

1. Start the audit service:
   ```bash
   # In the audit-log-svc directory
   docker-compose up -d
   ```

2. Enable publish tests:
   ```bash
   mvn spring-boot:run -Ddemo.run-publish-tests=true
   ```

   Or use the REST API:
   ```bash
   curl -X POST "http://localhost:8080/api/demo/publish?event=test.event&accountId=acc-123"
   ```

## Configuration

Edit `src/main/resources/application.yml` to configure:

```yaml
audit:
  sdk:
    base-url: http://localhost:8081  # Audit service URL
    timeout: 10s                      # Request timeout

demo:
  run-publish-tests: false            # Enable/disable publish tests
```

## SDK Features Demonstrated

### 1. AuditLogEntry Builder

```java
AuditLogEntry entry = AuditLogEntryBuilder
    .builder(AuditScope.ACCOUNT, "user.login")
    .withAccountId("acc-123")
    .withGeneratedMessage("User logged in successfully")
    .withFilterTag("environment", "production")
    .withPayload(Map.of("browser", "Chrome"))
    .withActionPerformedAtNow()
    .build();
```

### 2. AuditClient Builder

```java
AuditClient client = AuditClientBuilder.builder()
    .baseUrl("http://localhost:8081")
    .timeout(Duration.ofSeconds(10))
    .build();
```

### 3. Publishing

```java
// Synchronous
client.publish(entry);

// Asynchronous
CompletableFuture<Void> future = client.publishAsync(entry);

// Batch
client.publishBatch(entries);

// Builder shortcut
AuditLogEntryBuilder.builder(scope, event)
    .withAccountId("acc-123")
    .publish(client);
```

### 4. JWT Token Handling

```java
AuditLogEntry entry = AuditLogEntryBuilder
    .builder(AuditScope.ACCOUNT, "settings.updated")
    .withJwtToken(jwtToken)
    .withGeneratedMessage("updated settings")
    .populateActorNameInMessage(true)  // Extracts name from JWT
    .build();
```

## Project Structure

```
audit-sdk-demo/
├── pom.xml                           # Maven configuration
├── README.md                         # This file
└── src/main/
    ├── java/ai/armada/audit/demo/
    │   ├── AuditSdkDemoApplication.java  # Spring Boot main class
    │   ├── AuditSdkDemoRunner.java       # Automated demo runner
    │   └── AuditDemoController.java      # REST API controller
    └── resources/
        └── application.yml               # Configuration
```
