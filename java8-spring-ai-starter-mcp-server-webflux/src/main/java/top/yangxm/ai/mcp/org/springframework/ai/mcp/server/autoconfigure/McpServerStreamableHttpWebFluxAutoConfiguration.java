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
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport.WebFluxStreamableServerTransportProvider;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;

@SuppressWarnings("unused")
@AutoConfiguration(before = McpServerAutoConfiguration.class)
@ConditionalOnClass({McpSchema.class})
@EnableConfigurationProperties({
        McpServerProperties.class,
        McpServerStreamableHttpProperties.class
})
@Conditional({
        McpServerStdioDisabledCondition.class,
        McpServerAutoConfiguration.EnabledStreamableServerCondition.class
})
public class McpServerStreamableHttpWebFluxAutoConfiguration {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerStreamableHttpWebFluxAutoConfiguration.class);

    public McpServerStreamableHttpWebFluxAutoConfiguration(McpServerStreamableHttpProperties streamableHttpProperties) {
        Banner.printBanner(McpServerStreamableHttpWebFluxAutoConfiguration.class);
        logger.info(streamableHttpProperties.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebFluxStreamableServerTransportProvider webFluxTransport(McpServerStreamableHttpProperties streamableHttpProperties) {
        return WebFluxStreamableServerTransportProvider.builder()
                .jsonMapper(JsonMapper.getDefault())
                .messageEndpoint(streamableHttpProperties.getMcpEndpoint())
                .keepAliveInterval(streamableHttpProperties.getKeepAliveInterval())
                .disallowDelete(streamableHttpProperties.isDisallowDelete())
                .build();
    }

    @Bean
    public RouterFunction<?> webfluxMcpRouterFunction(WebFluxStreamableServerTransportProvider webFluxProvider) {
        return webFluxProvider.getRouterFunction();
    }
}
