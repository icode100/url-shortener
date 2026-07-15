package com.service.url_shortener.service;

public final class Base62Encoder {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int RADIX = ALPHABET.length;

    private Base62Encoder() {
    }

    public static String encode(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Only positive IDs can be encoded");
        }

        StringBuilder encoded = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            encoded.append(ALPHABET[(int) (remaining % RADIX)]);
            remaining /= RADIX;
        }
        return encoded.reverse().toString();
    }
}
