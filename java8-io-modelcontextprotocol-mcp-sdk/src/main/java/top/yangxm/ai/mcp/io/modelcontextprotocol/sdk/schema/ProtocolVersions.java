package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema;

public interface ProtocolVersions {
    String MCP_2024_11_05 = "2024-11-05";

    String MCP_2025_03_26 = "2025-03-26";

    String MCP_2025_06_18 = "2025-06-18";

    static String suggestedVersion() {
        return MCP_2025_06_18;
    }
}
