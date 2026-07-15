package com.service.url_shortener.service;

import com.service.url_shortener.domain.LinkMappingRepository;
import com.service.url_shortener.exception.AliasConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LinkShorteningServiceTests {

    @Autowired
    private LinkShorteningService service;

    @Autowired
    private LinkMappingRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsPersistentUrlSafeCodesWithoutCollisions() {
        Set<String> codes = new HashSet<>();

        for (int index = 0; index < 250; index++) {
            ShortenResult result = service.shorten("https://example.com/resource/" + index, null);
            assertThat(result.created()).isTrue();
            assertThat(result.mapping().getCode()).matches("_[0-9A-Za-z]+");
            assertThat(codes.add(result.mapping().getCode())).isTrue();
        }

        assertThat(repository.count()).isEqualTo(250);
    }

    @Test
    void returnsExistingGeneratedCodeForEquivalentDuplicateUrl() {
        ShortenResult first = service.shorten("HTTPS://Example.COM:443/a/../docs", null);
        ShortenResult duplicate = service.shorten("https://example.com/docs", null);

        assertThat(first.created()).isTrue();
        assertThat(duplicate.created()).isFalse();
        assertThat(duplicate.mapping().getCode()).isEqualTo(first.mapping().getCode());
        assertThat(first.mapping().getOriginalUrl()).isEqualTo("HTTPS://Example.COM:443/a/../docs");
        assertThat(first.mapping().getNormalizedUrl()).isEqualTo("https://example.com/docs");
        assertThat(repository.count()).isOne();
    }

    @Test
    void customAliasIsIdempotentForSameUrlAndConflictsForAnotherUrl() {
        ShortenResult first = service.shorten("https://example.com/docs", "team-docs");
        ShortenResult duplicate = service.shorten("https://EXAMPLE.com:443/docs", "team-docs");

        assertThat(first.created()).isTrue();
        assertThat(first.mapping().getCode()).isEqualTo("team-docs");
        assertThat(duplicate.created()).isFalse();
        assertThat(duplicate.mapping().getCode()).isEqualTo("team-docs");

        assertThatThrownBy(() -> service.shorten("https://example.org/other", "team-docs"))
                .isInstanceOf(AliasConflictException.class);
    }

    @Test
    void allowsDifferentCustomAliasesForTheSameUrl() {
        service.shorten("https://example.com/docs", "first-alias");
        service.shorten("https://example.com/docs", "second-alias");

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void concurrentDuplicateRequestsConvergeOnOneMapping() throws Exception {
        int callers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<ShortenResult>> futures = java.util.stream.IntStream.range(0, callers)
                    .mapToObj(ignored -> pool.submit(() -> {
                        ready.countDown();
                        start.await();
                        return service.shorten("https://example.com/concurrent", null);
                    }))
                    .toList();

            ready.await();
            start.countDown();

            Set<String> codes = new HashSet<>();
            long createdCount = 0;
            for (Future<ShortenResult> future : futures) {
                ShortenResult result = future.get();
                codes.add(result.mapping().getCode());
                createdCount += result.created() ? 1 : 0;
            }

            assertThat(codes).hasSize(1);
            assertThat(createdCount).isOne();
            assertThat(repository.count()).isOne();
        } finally {
            pool.shutdownNow();
        }
    }
}
