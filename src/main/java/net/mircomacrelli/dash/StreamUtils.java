package net.mircomacrelli.dash;

import java.util.*;
import java.util.stream.*;

final class StreamUtils {
    private StreamUtils() {
        throw new AssertionError("do not create instances of this class");
    }

    public static<T> Stream<T> concat(Stream<? extends T> first, Stream<? extends T> second, Stream<? extends T> third) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(third);

        return Stream.concat(Stream.concat(first, second), third);
    }
}
