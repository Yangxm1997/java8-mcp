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
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport.WebFluxStatelessServerTransport;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStatelessHttpProperties;

@SuppressWarnings("unused")
@AutoConfiguration(before = McpServerStatelessAutoConfiguration.class)
@ConditionalOnClass({McpSchema.class})
@ConditionalOnMissingBean(McpStatelessServerTransport.class)
@EnableConfigurationProperties({McpServerStatelessHttpProperties.class})
@Conditional({
        McpServerStdioDisabledCondition.class,
        McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class
})
public class McpServerStatelessWebFluxAutoConfiguration {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerStatelessWebFluxAutoConfiguration.class);

    public McpServerStatelessWebFluxAutoConfiguration(McpServerStatelessHttpProperties statelessProperties) {
        Banner.printBanner(McpServerStatelessWebFluxAutoConfiguration.class);
        logger.info(statelessProperties.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebFluxStatelessServerTransport webFluxTransport(McpServerStatelessHttpProperties statelessProperties) {
        return WebFluxStatelessServerTransport.builder()
                .jsonMapper(JsonMapper.getDefault())
                .messageEndpoint(statelessProperties.getMcpEndpoint())
                .build();
    }

    @Bean
    public RouterFunction<?> webfluxMcpRouterFunction(WebFluxStatelessServerTransport webFluxProvider) {
        return webFluxProvider.getRouterFunction();
    }
}
