package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import top.yangxm.ai.mcp.commons.json.McpJsonMapper;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerTransportProvider;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport.WebMvcSseServerTransportProvider;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;

@SuppressWarnings("unused")
@AutoConfiguration(before = McpServerAutoConfiguration.class)
@EnableConfigurationProperties({McpServerSseProperties.class})
@ConditionalOnClass({WebMvcSseServerTransportProvider.class})
@ConditionalOnMissingBean(McpServerTransportProvider.class)
@Conditional({McpServerStdioDisabledCondition.class, McpServerAutoConfiguration.EnabledSseServerCondition.class})
public class McpServerSseWebMvcAutoConfiguration {

    public McpServerSseWebMvcAutoConfiguration() {
        final Package _package = this.getClass().getPackage();
        String version = _package.getImplementationVersion();
        if (version == null || version.isEmpty()) {
            version = "dev";
        }
        Banner.printBanner("java8-spring-ai-starter-mcp-server-webmvc", version);
    }

    @Bean
    @ConditionalOnMissingBean
    public WebMvcSseServerTransportProvider webMvcTransport(McpServerSseProperties serverProperties) {
        return WebMvcSseServerTransportProvider.builder()
                .jsonMapper(McpJsonMapper.getDefault())
                .baseUrl(serverProperties.getBaseUrl())
                .messageEndpoint(serverProperties.getSseMessageEndpoint())
                .sseEndpoint(serverProperties.getSseEndpoint())
                .keepAliveInterval(serverProperties.getKeepAliveInterval())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> mvcMcpRouterFunction(WebMvcSseServerTransportProvider webMvcProvider) {
        return webMvcProvider.getRouterFunction();
    }
}