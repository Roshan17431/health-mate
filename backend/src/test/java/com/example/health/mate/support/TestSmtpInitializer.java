package com.example.health.mate.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.io.IOException;

/** Configures real SMTP delivery to a local sink, never to an external mail provider. */
public final class TestSmtpInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override public void initialize(ConfigurableApplicationContext context) {
        try {
            LocalSmtpServer smtp = new LocalSmtpServer();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "app.mail.host=localhost", "app.mail.port=" + smtp.port(),
                    "app.mail.from=healthmate@example.test", "app.mail.auth=false",
                    "app.mail.starttls-enabled=false", "app.mail.starttls-required=false",
                    "app.mail.ssl-enabled=false", "app.mail.connection-timeout-ms=1000",
                    "app.mail.timeout-ms=1000", "app.mail.write-timeout-ms=1000");
            context.getBeanFactory().registerSingleton("testSmtpServer", smtp);
            context.addApplicationListener(event -> {
                if (event instanceof ContextClosedEvent) {
                    try { smtp.close(); } catch (IOException ex) { throw new IllegalStateException(ex); }
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Could not start local test SMTP server", ex);
        }
    }
}
