package it.unive.dais.legodroid.lib.util;

import java.io.IOException;

@FunctionalInterface
public interface Consumer<T> {
    void call(T data) throws IOException;
}
