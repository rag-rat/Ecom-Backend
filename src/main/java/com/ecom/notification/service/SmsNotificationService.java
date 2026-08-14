package com.ecom.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/*
 * WHY THIS FILE EXISTS
 * --------------------
 * A STUBBED SMS implementation — demonstrates the extension point
 * the interface enables, without requiring a real Twilio/SMS gateway
 * account for this learning build. To wire a REAL SMS provider later:
 *   1. Add the provider's SDK dependency to pom.xml
 *   2. Add credentials to application.yml (environment-variable backed,
 *      same pattern as every other external integration in this project)
 *   3. Replace the log.info() below with the actual API call
 *   4. Nothing else in the codebase changes — no consumer, no controller,
 *      no other service needs to know this swap happened
 *
 * NOT @Service-annotated as-is — if BOTH EmailNotificationService and
 * this were @Service simultaneously, Spring would have TWO beans
 * implementing NotificationService and wouldn't know which to inject
 * wherever NotificationService is autowired, throwing a
 * NoUniqueBeanDefinitionException at startup. Leave this commented out
 * or use @Qualifier-based injection if you want both active at once for
 * different use cases (e.g. email for order confirmation, SMS for
 * delivery updates) — flagged here as a deliberate choice, not an
 * oversight: for THIS build, EmailNotificationService alone is wired
 * as THE NotificationService implementation.
 */


//@Service  ---> intentionally commented out
@Slf4j
public class SmsNotificationService implements NotificationService {

    @Override
    public void send(String recipient, String subject, String body) {
        // STUB — replace with real Twilio/SMS gateway call when needed
        log.info("[SMS STUB] Would send SMS to {}: {}", recipient, body);
    }
}
