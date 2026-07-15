package com.service.url_shortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTests {

    @Test
    void encodesKnownBoundaries() {
        assertThat(Base62Encoder.encode(1)).isEqualTo("1");
        assertThat(Base62Encoder.encode(61)).isEqualTo("z");
        assertThat(Base62Encoder.encode(62)).isEqualTo("10");
    }

    @Test
    void isOneToOneAcrossRepeatedIds() {
        Set<String> values = new HashSet<>();
        for (long id = 1; id <= 10_000; id++) {
            assertThat(values.add(Base62Encoder.encode(id))).isTrue();
        }
    }

    @Test
    void rejectsNonPositiveIds() {
        assertThatThrownBy(() -> Base62Encoder.encode(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
