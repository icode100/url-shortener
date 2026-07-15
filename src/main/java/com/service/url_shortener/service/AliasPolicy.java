package com.service.url_shortener.service;

import com.service.url_shortener.exception.InvalidAliasException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AliasPolicy {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 32;

    private static final Pattern VALID_ALIAS = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{2,31}");
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "actuator",
            "api",
            "error",
            "favicon.ico",
            "h2-console",
            "health",
            "shorten"
    );

    public String validate(String alias) {
        if (alias == null) {
            return null;
        }
        if (!VALID_ALIAS.matcher(alias).matches()) {
            throw new InvalidAliasException(
                    "Custom alias must be 3-32 URL-safe characters, start with a letter or digit, "
                            + "and contain only letters, digits, '-' or '_'"
            );
        }
        if (RESERVED_ALIASES.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new InvalidAliasException("Custom alias '" + alias + "' is reserved");
        }
        return alias;
    }
}
