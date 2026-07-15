package com.service.url_shortener.service;

import com.service.url_shortener.exception.InvalidAliasException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AliasPolicyTests {

    private final AliasPolicy policy = new AliasPolicy();

    @Test
    void acceptsUrlSafeAliases() {
        assertThat(policy.validate("My-link_42")).isEqualTo("My-link_42");
    }

    @Test
    void rejectsGeneratedNamespaceInvalidCharactersAndReservedRoutes() {
        assertThatThrownBy(() -> policy.validate("_future"))
                .isInstanceOf(InvalidAliasException.class);
        assertThatThrownBy(() -> policy.validate("two words"))
                .isInstanceOf(InvalidAliasException.class);
        assertThatThrownBy(() -> policy.validate("SHORTEN"))
                .isInstanceOf(InvalidAliasException.class)
                .hasMessageContaining("reserved");
    }
}
