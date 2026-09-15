package ru.itmo.secureapi.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class PlainTextSanitizer {

    // An empty allow-list removes every HTML element and attribute.
    private final PolicyFactory plainTextPolicy = new HtmlPolicyBuilder().toFactory();

    public String sanitize(String untrustedText) {
        return plainTextPolicy.sanitize(untrustedText).strip();
    }
}
