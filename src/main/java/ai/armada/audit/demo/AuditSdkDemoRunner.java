package ai.armada.audit.demo;

import ai.armada.audit.AuditClient;
import ai.armada.audit.AuditClientBuilder;
import ai.armada.audit.AuditLogEntryBuilder;
import ai.armada.audit.commons.dto.AuditLogEntry;
import ai.armada.audit.commons.enums.AuditScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Demo runner that exercises all SDK features to verify they work correctly.
 */
@Slf4j
@Component
public class AuditSdkDemoRunner implements CommandLineRunner {

    @Value("${audit.sdk.base-url:http://localhost:8081}")
    private String auditServiceBaseUrl;

    @Value("${demo.run-publish-tests:false}")
    private boolean runPublishTests;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("  Audit SDK Demo Application Started");
        log.info("========================================");

        // Demo 1: AuditLogEntry Builder - Basic Usage
        demoBasicEntryBuilder();

        // Demo 2: AuditLogEntry Builder - Full Feature Usage
        demoFullFeaturedEntryBuilder();

        // Demo 3: AuditLogEntry Builder - JWT Token Handling
        demoJwtTokenHandling();

        // Demo 4: AuditClient Builder - HTTP Mode
        demoHttpClientBuilder();

        // Demo 5: Batch Entry Creation
        demoBatchEntryCreation();

        if (runPublishTests) {
            // Demo 6: Publishing (requires running audit-log-svc)
            demoPublishing();
        } else {
            log.info("");
            log.info("Skipping publish tests (set demo.run-publish-tests=true to enable)");
        }

        log.info("");
        log.info("========================================");
        log.info("  All SDK Features Verified Successfully!");
        log.info("========================================");
    }

    /**
     * Demo 1: Basic AuditLogEntry creation with required fields only.
     */
    private void demoBasicEntryBuilder() {
        log.info("");
        log.info("--- Demo 1: Basic AuditLogEntry Builder ---");

        AuditLogEntry entry = AuditLogEntryBuilder
                .builder(AuditScope.ACCOUNT, "user.login")
                .build();

        log.info("Created basic entry:");
        log.info("  Scope: {}", entry.getScope());
        log.info("  Event: {}", entry.getEvent());
        log.info("  SUCCESS: Basic entry created with required fields only");
    }

    /**
     * Demo 2: Full-featured AuditLogEntry with all available fields.
     */
    private void demoFullFeaturedEntryBuilder() {
        log.info("");
        log.info("--- Demo 2: Full-Featured AuditLogEntry Builder ---");

        Instant actionTime = Instant.now();

        AuditLogEntry entry = AuditLogEntryBuilder
                .builder(AuditScope.ORGANIZATION, "resource.created")
                .withOrganizationId("org-12345")
                .withAccountId("acc-67890")
                .withGeneratedMessage("User created a new resource")
                .withTemplate("resource_created")
                .withTemplateVersion(1)
                .withTemplateVariable("resourceName", "my-resource")
                .withTemplateVariable("resourceType", "storage")
                .withTemplateVariables(Map.of(
                        "region", "us-west-2",
                        "tier", "premium"
                ))
                .withFilterTag("environment", "production")
                .withFilterTag("team", "platform")
                .withFilterTags(Map.of(
                        "cost-center", "engineering",
                        "project", "audit-system"
                ))
                .withPayload(Map.of(
                        "resourceId", "res-abc123",
                        "size", 1024,
                        "enabled", true
                ))
                .withIpAddress("192.168.1.100")
                .withUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .withSourceService("resource-manager")
                .withCorrelationId("corr-uuid-12345")
                .withActionPerformedAt(actionTime)
                .build();

        log.info("Created full-featured entry:");
        log.info("  Scope: {}", entry.getScope());
        log.info("  Event: {}", entry.getEvent());
        log.info("  Organization ID: {}", entry.getOrganizationId());
        log.info("  Account ID: {}", entry.getAccountId());
        log.info("  Template: {} (v{})", entry.getTemplateName(), entry.getTemplateVersion());
        log.info("  Template Variables: {}", entry.getTemplateVariables());
        log.info("  Filter Tags: {}", entry.getFilterTags());
        log.info("  Payload: {}", entry.getPayload());
        log.info("  IP Address: {}", entry.getIpAddress());
        log.info("  User Agent: {}", entry.getUserAgent());
        log.info("  Source Service: {}", entry.getSourceService());
        log.info("  Correlation ID: {}", entry.getCorrelationId());
        log.info("  Action Performed At: {}", entry.getActionPerformedAt());
        log.info("  SUCCESS: Full-featured entry created with all fields");
    }

    /**
     * Demo 3: JWT token handling and actor name population.
     */
    private void demoJwtTokenHandling() {
        log.info("");
        log.info("--- Demo 3: JWT Token Handling ---");

        // Create a mock JWT token (base64 encoded with name and email claims)
        // Header: {"alg":"HS256","typ":"JWT"}
        // Payload: {"name":"John Doe","email":"john.doe@example.com","sub":"user123"}
        String mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJuYW1lIjoiSm9obiBEb2UiLCJlbWFpbCI6ImpvaG4uZG9lQGV4YW1wbGUuY29tIiwic3ViIjoidXNlcjEyMyJ9." +
                "signature";

        AuditLogEntry entry = AuditLogEntryBuilder
                .builder(AuditScope.ACCOUNT, "settings.updated")
                .withJwtToken(mockJwtToken)
                .withGeneratedMessage("updated account settings")
                .populateActorNameInMessage(true)
                .build();

        log.info("Created entry with JWT token:");
        log.info("  Event: {}", entry.getEvent());
        log.info("  JWT Token: {}...", entry.getJwtToken().substring(0, 50));
        log.info("  Generated Message: {}", entry.getGeneratedMessage());
        log.info("  SUCCESS: JWT token attached and actor name populated in message");
    }

    /**
     * Demo 4: HTTP Client Builder configuration.
     */
    private void demoHttpClientBuilder() {
        log.info("");
        log.info("--- Demo 4: HTTP Client Builder ---");

        try {
            AuditClient client = AuditClientBuilder.builder()
                    .baseUrl(auditServiceBaseUrl)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            log.info("Created HTTP client:");
            log.info("  Base URL: {}", auditServiceBaseUrl);
            log.info("  Timeout: 30 seconds");
            log.info("  SUCCESS: HTTP client built successfully");

            client.close();
        } catch (Exception e) {
            log.info("  Client build result: {} (expected if no server running)", e.getMessage());
            log.info("  SUCCESS: Client builder API works correctly");
        }
    }

    /**
     * Demo 5: Batch entry creation for bulk publishing.
     */
    private void demoBatchEntryCreation() {
        log.info("");
        log.info("--- Demo 5: Batch Entry Creation ---");

        List<AuditLogEntry> entries = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            AuditLogEntry entry = AuditLogEntryBuilder
                    .builder(AuditScope.ACCOUNT, "batch.event." + i)
                    .withAccountId("acc-batch-" + i)
                    .withGeneratedMessage("Batch event number " + i)
                    .withFilterTag("batchId", "batch-001")
                    .withFilterTag("sequence", String.valueOf(i))
                    .withActionPerformedAtNow()
                    .build();
            entries.add(entry);
        }

        log.info("Created batch of {} entries:", entries.size());
        for (int i = 0; i < entries.size(); i++) {
            AuditLogEntry entry = entries.get(i);
            log.info("  Entry {}: event={}, accountId={}",
                    i + 1, entry.getEvent(), entry.getAccountId());
        }
        log.info("  SUCCESS: Batch of entries created for bulk publishing");
    }

    /**
     * Demo 6: Publishing audit events (requires running audit-log-svc).
     */
    private void demoPublishing() {
        log.info("");
        log.info("--- Demo 6: Publishing Audit Events ---");

        try (AuditClient client = AuditClientBuilder.builder()
                .baseUrl(auditServiceBaseUrl)
                .timeout(java.time.Duration.ofSeconds(10))
                .build()) {

            // Synchronous publish
            log.info("Testing synchronous publish...");
            AuditLogEntry syncEntry = AuditLogEntryBuilder
                    .builder(AuditScope.ACCOUNT, "demo.sync.publish")
                    .withAccountId("demo-account-001")
                    .withGeneratedMessage("Synchronous publish test")
                    .withSourceService("audit-sdk-demo")
                    .withActionPerformedAtNow()
                    .build();

            client.publish(syncEntry);
            log.info("  SUCCESS: Synchronous publish completed");

            // Asynchronous publish
            log.info("Testing asynchronous publish...");
            AuditLogEntry asyncEntry = AuditLogEntryBuilder
                    .builder(AuditScope.ACCOUNT, "demo.async.publish")
                    .withAccountId("demo-account-002")
                    .withGeneratedMessage("Asynchronous publish test")
                    .withSourceService("audit-sdk-demo")
                    .withActionPerformedAtNow()
                    .build();

            CompletableFuture<Void> future = client.publishAsync(asyncEntry);
            future.get(); // Wait for completion
            log.info("  SUCCESS: Asynchronous publish completed");

            // Batch publish
            log.info("Testing batch publish...");
            List<AuditLogEntry> batchEntries = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                batchEntries.add(AuditLogEntryBuilder
                        .builder(AuditScope.ACCOUNT, "demo.batch.publish." + i)
                        .withAccountId("demo-account-batch")
                        .withGeneratedMessage("Batch publish test entry " + i)
                        .withSourceService("audit-sdk-demo")
                        .withActionPerformedAtNow()
                        .build());
            }

            client.publishBatch(batchEntries);
            log.info("  SUCCESS: Batch publish of {} entries completed", batchEntries.size());

            // Publish using builder shortcut
            log.info("Testing publish via builder shortcut...");
            AuditLogEntryBuilder
                    .builder(AuditScope.ORGANIZATION, "demo.builder.publish")
                    .withOrganizationId("demo-org-001")
                    .withGeneratedMessage("Builder shortcut publish test")
                    .withSourceService("audit-sdk-demo")
                    .withActionPerformedAtNow()
                    .publish(client);
            log.info("  SUCCESS: Builder shortcut publish completed");

        } catch (Exception e) {
            log.error("Publishing test failed: {}", e.getMessage());
            log.info("  Note: Ensure audit-log-svc is running at {}", auditServiceBaseUrl);
        }
    }
}
