package top.yangxm.ai.mcp.commons.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class Sets {
    private Sets() {
    }

    public static <E> boolean isEmpty(Set<E> set) {
        return set == null || set.isEmpty();
    }

    public static <E> Set<E> of() {
        return setN();
    }

    public static <E> Set<E> of(E e1) {
        return setN(e1);
    }

    public static <E> Set<E> of(E e1, E e2) {
        return setN(e1, e2);
    }

    public static <E> Set<E> of(E e1, E e2, E e3) {
        return setN(e1, e2, e3);
    }

    public static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return setN(e1, e2, e3, e4);
    }

    public static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return setN(e1, e2, e3, e4, e5);
    }

    @SafeVarargs
    private static <E> Set<E> setN(E... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
    }
}
