package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpTool;

import java.util.ArrayList;
import java.util.List;

final class Banner {
    private static final String DEFAULT_VERSION = "dev";

    private Banner() {
    }

    static void printBanner(Class<?> tspClazz) {
        List<String[]> implList = new ArrayList<>();
        implList.add(getImplementation(tspClazz));
        implList.add(getImplementation(McpSchema.class));
        implList.add(getImplementation(McpTool.class));
        implList.add(getImplementation(Banner.class));

        int maxTitleLen = 0;
        for (String[] impl : implList) {
            if (impl[0].length() > maxTitleLen) {
                maxTitleLen = impl[0].length();
            }
        }
        final String format = ":: [%-" + maxTitleLen + "s] :: %s%n";

        System.out.println("==============================================================================================================");
        System.out.println("Java8 - Spring AI Starter");
        for (String[] impl : implList) {
            System.out.printf(format, impl[0], impl[1]);
        }
        System.out.println("This project is based on the original project by: ");
        System.out.println(":: [io.modelcontextprotocol.sdk] :: https://github.com/modelcontextprotocol/java-sdk");
        System.out.println(":: [org.springframework.ai]      :: https://github.com/spring-projects/spring-ai");
        System.out.println(":: [org.springaicommunity]       :: https://github.com/spring-ai-community/mcp-annotations");
        System.out.println("This project is modified to Java 8.");
        System.out.println("All modifications are by [top.yangxm] :: [java8-mcp] :: https://github.com/Yangxm1997/java8-mcp");
        System.out.println("==============================================================================================================");
    }

    private static String[] getImplementation(Class<?> clazz) {
        final Package pkg = clazz.getPackage();
        String title = pkg.getImplementationTitle();
        if (title == null || title.isEmpty()) {
            throw new IllegalStateException("ImplementationTitle is null or empty, class: " + clazz.getName());
        }

        String version = pkg.getImplementationVersion();
        if (version == null || version.isEmpty()) {
            version = DEFAULT_VERSION;
        }
        return new String[]{title, version};
    }
}
