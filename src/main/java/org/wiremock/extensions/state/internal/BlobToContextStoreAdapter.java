package org.wiremock.extensions.state.internal;

import com.github.tomakehurst.wiremock.common.Exceptions;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.store.BlobStore;
import com.github.tomakehurst.wiremock.store.Store;

import java.util.Optional;
import java.util.stream.Stream;

public class BlobToContextStoreAdapter implements Store<String, Context> {

    private final BlobStore blobStore;

    public BlobToContextStoreAdapter(BlobStore blobStore) {
        this.blobStore = blobStore;
    }

    @Override
    public Stream<String> getAllKeys() {
        return blobStore.getAllKeys();
    }

    @Override
    public Optional<Context> get(String key) {
        try {
            return blobStore.get(key)
                .map(data -> readJsonBytes(data, Context.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Context content) {
        final byte[] value = writeJsonBytes(content);
        blobStore.put(key, value);
    }

    @Override
    public void remove(String key) {
        blobStore.remove(key);
    }

    @Override
    public void clear() {
        blobStore.clear();
    }

    private static <T> T readJsonBytes(byte[] data, Class<T> clazz) {
        return Exceptions.uncheck(() -> Json.getObjectMapper().readValue(data, clazz), clazz);
    }

    private static byte[] writeJsonBytes(Object obj) {
        return Exceptions.uncheck(() -> Json.getObjectMapper().writeValueAsBytes(obj), byte[].class);
    }
}
