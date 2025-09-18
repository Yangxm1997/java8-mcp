package top.yangxm.ai.mcp.commons.util;

public final class Utils {
    private Utils() {
    }

    public static boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
