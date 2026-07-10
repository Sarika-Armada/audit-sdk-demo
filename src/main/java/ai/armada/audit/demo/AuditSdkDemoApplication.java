package ai.armada.audit.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo application to verify the audit-svc-java-sdk works correctly.
 *
 * This application demonstrates:
 * - Building audit log entries with the fluent builder API
 * - Creating audit clients for HTTP and EventHub modes
 * - Publishing audit events synchronously, asynchronously, and in batches
 * - Template message generation
 */
@SpringBootApplication
public class AuditSdkDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditSdkDemoApplication.class, args);
    }
}
