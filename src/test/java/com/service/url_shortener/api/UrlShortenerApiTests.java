package com.service.url_shortener.api;

import com.service.url_shortener.domain.LinkMapping;
import com.service.url_shortener.domain.LinkMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "shortener.base-url=https://sho.rt/")
@AutoConfigureMockMvc
class UrlShortenerApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LinkMappingRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void shortenAndRedirectRoundTripUsesExactPermanentRedirect() throws Exception {
        String originalUrl = "https://Example.com:443/a/../docs?q=one#top";

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://Example.com:443/a/../docs?q=one#top"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("https://sho\\.rt/_[0-9A-Za-z]+")))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("_[0-9A-Za-z]+")))
                .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.matchesPattern("https://sho\\.rt/_[0-9A-Za-z]+")))
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(jsonPath("$.customAlias").value(false));

        LinkMapping mapping = repository.findAll().get(0);
        mockMvc.perform(get("/{code}", mapping.getCode()))
                .andExpect(status().is(301))
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void duplicateGeneratedUrlReturnsExistingCodeWithOk() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"HTTPS://Example.com:443/a/../docs\"}"))
                .andExpect(status().isCreated());

        String code = repository.findAll().get(0).getCode();
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/docs\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));

        assertThat(repository.count()).isOne();
    }

    @Test
    void customAliasRedirectsAndConflictingDestinationReturnsProblem() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/docs\",\"customAlias\":\"team-docs\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "https://sho.rt/team-docs"))
                .andExpect(jsonPath("$.code").value("team-docs"))
                .andExpect(jsonPath("$.customAlias").value(true));

        mockMvc.perform(get("/team-docs"))
                .andExpect(status().is(301))
                .andExpect(header().string("Location", "https://example.com/docs"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://EXAMPLE.com:443/docs\",\"customAlias\":\"team-docs\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("team-docs"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org/other\",\"customAlias\":\"team-docs\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("ALIAS_CONFLICT"));
    }

    @Test
    void unknownCodeReturnsNotFoundProblem() throws Exception {
        mockMvc.perform(get("/missing-code"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("LINK_NOT_FOUND"));
    }

    @Test
    void invalidAndMalformedRequestsReturnBadRequestProblems() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_URL"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"customAlias\":\"_reserved\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ALIAS"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors.url").value("URL is required"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"));
    }

    @Test
    void analyticsReportsServerObservedRedirects() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/analytics\",\"customAlias\":\"stats-link\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/stats-link")).andExpect(status().is(301));
        mockMvc.perform(get("/stats-link")).andExpect(status().is(301));

        mockMvc.perform(get("/api/links/stats-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("stats-link"))
                .andExpect(jsonPath("$.redirectCount").value(2))
                .andExpect(jsonPath("$.lastAccessedAt").isNotEmpty());
    }
}
