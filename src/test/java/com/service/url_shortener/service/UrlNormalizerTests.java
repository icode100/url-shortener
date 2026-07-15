package com.service.url_shortener.service;

import com.service.url_shortener.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlNormalizerTests {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void normalizesSchemeHostDefaultPortAndDotSegments() {
        assertThat(normalizer.normalize("  HTTPS://Example.COM:443/a/../docs?q=1#top  "))
                .isEqualTo("https://example.com/docs?q=1#top");
        assertThat(normalizer.normalize("http://example.com"))
                .isEqualTo("http://example.com/");
    }

    @Test
    void keepsNonDefaultPortAndEncodedPath() {
        assertThat(normalizer.normalize("https://Example.com:8443/a%2Fb"))
                .isEqualTo("https://example.com:8443/a%2Fb");
    }

    @Test
    void rejectsUnsafeOrNonAbsoluteUrls() {
        assertThatThrownBy(() -> normalizer.normalize("javascript:alert(1)"))
                .isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> normalizer.normalize("/relative/path"))
                .isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> normalizer.normalize("https://user:secret@example.com/private"))
                .isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> normalizer.normalize("https:///missing-host"))
                .isInstanceOf(InvalidUrlException.class);
    }
}
