package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerTransportProviderBase;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport.HttpServletSseServerTransportProvider;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;

import javax.servlet.http.HttpServlet;

@SuppressWarnings("unused")
@AutoConfiguration(before = McpServerAutoConfiguration.class)
@EnableConfigurationProperties({McpServerSseProperties.class})
@ConditionalOnClass({HttpServletSseServerTransportProvider.class})
@ConditionalOnMissingBean(McpServerTransportProviderBase.class)
@Conditional({McpServerStdioDisabledCondition.class, McpServerAutoConfiguration.EnabledSseServerCondition.class})
public class McpServerSseHttpServletAutoConfiguration {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerSseHttpServletAutoConfiguration.class);

    public McpServerSseHttpServletAutoConfiguration(McpServerSseProperties sseProperties) {
        Banner.printBanner(McpServerSseHttpServletAutoConfiguration.class);
        logger.info(sseProperties.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpServletSseServerTransportProvider httpServletTransport(McpServerSseProperties sseProperties) {
        return HttpServletSseServerTransportProvider.builder()
                .jsonMapper(JsonMapper.getDefault())
                .baseUrl(sseProperties.getBaseUrl())
                .messageEndpoint(sseProperties.getSseMessageEndpoint())
                .sseEndpoint(sseProperties.getSseEndpoint())
                .keepAliveInterval(sseProperties.getKeepAliveInterval())
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServlet> httpServletRouter(HttpServletSseServerTransportProvider httpServletProvider) {
        return new ServletRegistrationBean<>(httpServletProvider);
    }
}
