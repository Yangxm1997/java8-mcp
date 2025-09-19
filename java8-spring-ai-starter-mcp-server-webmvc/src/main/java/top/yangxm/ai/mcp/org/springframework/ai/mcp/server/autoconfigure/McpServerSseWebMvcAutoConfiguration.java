package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
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
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerSseWebMvcAutoConfiguration.class);

    public McpServerSseWebMvcAutoConfiguration(McpServerSseProperties sseProperties) {
        Banner.printBanner(McpServerSseWebMvcAutoConfiguration.class);
        logger.info(sseProperties.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebMvcSseServerTransportProvider webMvcTransport(McpServerSseProperties sseProperties) {
        return WebMvcSseServerTransportProvider.builder()
                .jsonMapper(JsonMapper.getDefault())
                .baseUrl(sseProperties.getBaseUrl())
                .messageEndpoint(sseProperties.getSseMessageEndpoint())
                .sseEndpoint(sseProperties.getSseEndpoint())
                .keepAliveInterval(sseProperties.getKeepAliveInterval())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> mvcMcpRouterFunction(WebMvcSseServerTransportProvider webMvcProvider) {
        return webMvcProvider.getRouterFunction();
    }
}