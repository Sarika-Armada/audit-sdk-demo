package ai.armada.audit.demo;

import ai.armada.audit.AuditClient;
import ai.armada.audit.AuditClientBuilder;
import ai.armada.audit.AuditLogEntryBuilder;
import ai.armada.audit.commons.dto.AuditLogEntry;
import ai.armada.audit.commons.enums.AuditScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for interactive SDK testing.
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
public class AuditDemoController {

    @Value("${audit.sdk.base-url:http://localhost:8081}")
    private String auditServiceBaseUrl;

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "audit-sdk-demo",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Create a sample audit entry and return its JSON representation.
     */
    @GetMapping("/entry/sample")
    public ResponseEntity<AuditLogEntry> createSampleEntry() {
        AuditLogEntry entry = AuditLogEntryBuilder
                .builder(AuditScope.ACCOUNT, "demo.sample.entry")
                .withOrganizationId("org-demo-123")
                .withAccountId("acc-demo-456")
                .withGeneratedMessage("Sample audit entry created via REST API")
                .withTemplate("demo_template")
                .withTemplateVariable("action", "create")
                .withTemplateVariable("target", "sample-resource")
                .withFilterTag("environment", "demo")
                .withFilterTag("source", "rest-api")
                .withPayload(Map.of(
                        "resourceId", "res-sample-001",
                        "operationType", "CREATE",
                        "details", Map.of("key1", "value1", "key2", "value2")
                ))
                .withIpAddress("127.0.0.1")
                .withUserAgent("audit-sdk-demo/1.0")
                .withSourceService("audit-sdk-demo")
                .withCorrelationId("corr-" + System.currentTimeMillis())
                .withActionPerformedAtNow()
                .build();

        log.info("Created sample entry: event={}, scope={}", entry.getEvent(), entry.getScope());
        return ResponseEntity.ok(entry);
    }

    /**
     * Create a custom audit entry from request parameters.
     */
    @PostMapping("/entry/create")
    public ResponseEntity<AuditLogEntry> createEntry(
            @RequestParam(defaultValue = "ACCOUNT") String scope,
            @RequestParam(defaultValue = "custom.event") String event,
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String message) {

        AuditScope auditScope = AuditScope.valueOf(scope.toUpperCase());

        AuditLogEntryBuilder builder = AuditLogEntryBuilder
                .builder(auditScope, event)
                .withSourceService("audit-sdk-demo")
                .withActionPerformedAtNow();

        if (organizationId != null) {
            builder.withOrganizationId(organizationId);
        }
        if (accountId != null) {
            builder.withAccountId(accountId);
        }
        if (message != null) {
            builder.withGeneratedMessage(message);
        }

        AuditLogEntry entry = builder.build();

        log.info("Created custom entry: event={}, scope={}", entry.getEvent(), entry.getScope());
        return ResponseEntity.ok(entry);
    }

    /**
     * Publish an audit entry to the audit service.
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishEntry(
            @RequestParam(defaultValue = "ACCOUNT") String scope,
            @RequestParam(defaultValue = "demo.publish.test") String event,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String message) {

        try (AuditClient client = AuditClientBuilder.builder()
                .baseUrl(auditServiceBaseUrl)
                .timeout(Duration.ofSeconds(10))
                .build()) {

            AuditScope auditScope = AuditScope.valueOf(scope.toUpperCase());

            AuditLogEntry entry = AuditLogEntryBuilder
                    .builder(auditScope, event)
                    .withAccountId(accountId != null ? accountId : "demo-account")
                    .withGeneratedMessage(message != null ? message : "Published via REST API")
                    .withSourceService("audit-sdk-demo")
                    .withFilterTag("source", "rest-api")
                    .withActionPerformedAtNow()
                    .build();

            client.publish(entry);

            log.info("Published entry: event={}", entry.getEvent());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "event", event,
                    "message", "Entry published successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to publish entry: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "hint", "Ensure audit-log-svc is running at " + auditServiceBaseUrl
            ));
        }
    }

    /**
     * Publish a batch of audit entries.
     */
    @PostMapping("/publish/batch")
    public ResponseEntity<Map<String, Object>> publishBatch(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "demo.batch.test") String eventPrefix) {

        try (AuditClient client = AuditClientBuilder.builder()
                .baseUrl(auditServiceBaseUrl)
                .timeout(Duration.ofSeconds(30))
                .build()) {

            List<AuditLogEntry> entries = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                entries.add(AuditLogEntryBuilder
                        .builder(AuditScope.ACCOUNT, eventPrefix + "." + i)
                        .withAccountId("batch-account")
                        .withGeneratedMessage("Batch entry " + i + " of " + count)
                        .withSourceService("audit-sdk-demo")
                        .withFilterTag("batchId", "batch-" + System.currentTimeMillis())
                        .withFilterTag("sequence", String.valueOf(i))
                        .withActionPerformedAtNow()
                        .build());
            }

            client.publishBatch(entries);

            log.info("Published batch of {} entries", count);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", count,
                    "message", "Batch published successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to publish batch: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Publish an entry asynchronously.
     */
    @PostMapping("/publish/async")
    public ResponseEntity<Map<String, Object>> publishAsync(
            @RequestParam(defaultValue = "demo.async.test") String event) {

        try (AuditClient client = AuditClientBuilder.builder()
                .baseUrl(auditServiceBaseUrl)
                .timeout(Duration.ofSeconds(10))
                .build()) {

            AuditLogEntry entry = AuditLogEntryBuilder
                    .builder(AuditScope.ACCOUNT, event)
                    .withAccountId("async-account")
                    .withGeneratedMessage("Async publish test")
                    .withSourceService("audit-sdk-demo")
                    .withActionPerformedAtNow()
                    .build();

            CompletableFuture<Void> future = client.publishAsync(entry);
            future.get(); // Wait for completion

            log.info("Async published entry: event={}", event);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "event", event,
                    "message", "Entry published asynchronously"
            ));

        } catch (Exception e) {
            log.error("Failed to publish async: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * List available AuditScope values.
     */
    @GetMapping("/scopes")
    public ResponseEntity<AuditScope[]> listScopes() {
        return ResponseEntity.ok(AuditScope.values());
    }
}
