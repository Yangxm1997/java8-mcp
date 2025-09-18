package top.yangxm.ai.mcp.commons.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public final class Maps {
    private Maps() {
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    public static <K, V> Map<K, V> of() {
        return mapN();
    }

    public static <K, V> Map<K, V> of(K k1, V v1) {
        return mapN(k1, v1);
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        return mapN(k1, v1, k2, v2);
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return mapN(k1, v1, k2, v2, k3, v3);
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return mapN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    private static <K, V> Map<K, V> mapN(Object... input) {
        if (input == null || input.length == 0) {
            return Collections.emptyMap();
        }

        if ((input.length & 1) != 0) {
            throw new InternalError("length is odd");
        }

        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < input.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = Objects.requireNonNull((K) input[i]);
            @SuppressWarnings("unchecked")
            V v = Objects.requireNonNull((V) input[i + 1]);

            if (map.putIfAbsent(k, v) != null) {
                throw new IllegalArgumentException("duplicate key: " + k);
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
