package org.wiremock.extensions.state;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.tomakehurst.wiremock.store.Store;

import javax.swing.text.html.Option;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

public class CaffeineStore implements Store<String, Object> {

    private static final int DEFAULT_EXPIRATION_SECONDS = 60 * 60;

    private final Cache<String, Object> cache;

    public CaffeineStore() {
        this(0);
    }

    public CaffeineStore(int expirationSeconds) {
        var builder = Caffeine.newBuilder();
        if (expirationSeconds == 0) {
            builder.expireAfterWrite(Duration.ofSeconds(DEFAULT_EXPIRATION_SECONDS));
        } else {
            builder.expireAfterWrite(Duration.ofSeconds(expirationSeconds));
        }
        cache = builder.build();
    }

    @Override
    public Stream<String> getAllKeys() {
        return cache.asMap().keySet().stream();
    }

    @Override
    public Optional<Object> get(String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    @Override
    public void put(String key, Object content) {
        cache.put(key, content);
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }
}
