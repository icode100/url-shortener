package com.service.url_shortener.service;

import com.service.url_shortener.domain.LinkMapping;

public record ShortenResult(LinkMapping mapping, boolean created) {
}
