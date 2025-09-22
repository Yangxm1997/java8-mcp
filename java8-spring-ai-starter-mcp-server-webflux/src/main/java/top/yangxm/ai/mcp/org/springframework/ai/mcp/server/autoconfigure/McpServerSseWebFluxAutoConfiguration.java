package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.reactive.function.server.RouterFunction;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerTransportProviderBase;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport.WebFluxSseServerTransportProvider;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;

@SuppressWarnings("unused")
@AutoConfiguration(before = McpServerAutoConfiguration.class)
@EnableConfigurationProperties({McpServerSseProperties.class})
@ConditionalOnClass({WebFluxSseServerTransportProvider.class})
@ConditionalOnMissingBean(McpServerTransportProviderBase.class)
@Conditional({McpServerStdioDisabledCondition.class, McpServerAutoConfiguration.EnabledSseServerCondition.class})
public class McpServerSseWebFluxAutoConfiguration {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerSseWebFluxAutoConfiguration.class);

    public McpServerSseWebFluxAutoConfiguration(McpServerSseProperties sseProperties) {
        Banner.printBanner(McpServerSseWebFluxAutoConfiguration.class);
        logger.info(sseProperties.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebFluxSseServerTransportProvider webFluxTransport(McpServerSseProperties sseProperties) {
        return WebFluxSseServerTransportProvider.builder()
                .jsonMapper(JsonMapper.getDefault())
                .baseUrl(sseProperties.getBaseUrl())
                .messageEndpoint(sseProperties.getSseMessageEndpoint())
                .sseEndpoint(sseProperties.getSseEndpoint())
                .keepAliveInterval(sseProperties.getKeepAliveInterval())
                .build();
    }

    @Bean
    public RouterFunction<?> webfluxMcpRouterFunction(WebFluxSseServerTransportProvider webFluxProvider) {
        return webFluxProvider.getRouterFunction();
    }
}
