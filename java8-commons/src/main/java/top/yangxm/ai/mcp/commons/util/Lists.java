package top.yangxm.ai.mcp.commons.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class Lists {
    private Lists() {
    }

    public static <E> boolean isEmpty(List<E> list) {
        return list == null || list.isEmpty();
    }

    public static <E> List<E> of() {
        return listN();
    }

    public static <E> List<E> of(E e1) {
        return listN(e1);
    }

    public static <E> List<E> of(E e1, E e2) {
        return listN(e1, e2);
    }

    public static <E> List<E> of(E e1, E e2, E e3) {
        return listN(e1, e2, e3);
    }

    public static <E> List<E> of(E e1, E e2, E e3, E e4) {
        return listN(e1, e2, e3, e4);
    }

    public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return listN(e1, e2, e3, e4, e5);
    }

    @SafeVarargs
    private static <E> List<E> listN(E... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(elements));
    }
}
