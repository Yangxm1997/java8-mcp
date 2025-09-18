package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpTool;

final class Banner {
    private Banner() {
    }

    static void printBanner(String tsp, String version) {
        final Package mcpSdkPackage = McpSchema.class.getPackage();
        String mcpSdkVersion = mcpSdkPackage.getImplementationVersion();
        if (mcpSdkVersion == null || mcpSdkVersion.isEmpty()) {
            mcpSdkVersion = "dev";
        }

        final Package springAiMcpPackage = McpTool.class.getPackage();
        String springAiMcpVersion = springAiMcpPackage.getImplementationVersion();
        if (springAiMcpVersion == null || springAiMcpVersion.isEmpty()) {
            springAiMcpVersion = "dev";
        }

        final Package springAiStarterMcpServerPackage = Banner.class.getPackage();
        String springAiStarterMcpServerVersion = springAiStarterMcpServerPackage.getImplementationVersion();
        if (springAiStarterMcpServerVersion == null || springAiStarterMcpServerVersion.isEmpty()) {
            springAiStarterMcpServerVersion = "dev";
        }

        System.out.println("====================== Java8 - Spring AI Starter ======================");
        System.out.println(":: " + tsp + " -> " + version);
        System.out.println(":: java8-spring-ai-starter-mcp-server -> " + springAiStarterMcpServerVersion);
        System.out.println(":: java8-spring-ai-mcp -> " + springAiMcpVersion);
        System.out.println(":: java8-io-modelcontextprotocol-mcp-sdk -> " + mcpSdkVersion);
    }
}
