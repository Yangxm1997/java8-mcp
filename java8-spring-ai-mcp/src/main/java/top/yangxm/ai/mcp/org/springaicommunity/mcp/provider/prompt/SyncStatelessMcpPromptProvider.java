package top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Prompt;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncPromptSpec;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.adapter.PromptAdapter;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpPrompt;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.method.prompt.SyncStatelessMcpPromptMethodCallback;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SyncStatelessMcpPromptProvider extends AbstractMcpPromptProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(SyncStatelessMcpPromptProvider.class);

    public SyncStatelessMcpPromptProvider(List<Object> promptObjects) {
        super(promptObjects);
    }

    public List<SyncPromptSpec> getPromptSpecs() {
        List<SyncPromptSpec> promptSpecs = this.promptObjects.stream()
                .map(resourceObject -> Stream.of(doGetClassMethods(resourceObject))
                        .filter(method -> method.isAnnotationPresent(McpPrompt.class))
                        .filter(method -> !Mono.class.isAssignableFrom(method.getReturnType()))
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpPromptMethod -> {
                            McpPrompt promptAnnotation = mcpPromptMethod.getAnnotation(McpPrompt.class);
                            Prompt mcpPrompt = PromptAdapter.asPrompt(promptAnnotation, mcpPromptMethod);

                            SyncStatelessMcpPromptMethodCallback methodCallback = SyncStatelessMcpPromptMethodCallback.builder()
                                    .method(mcpPromptMethod)
                                    .bean(resourceObject)
                                    .prompt(mcpPrompt)
                                    .build();

                            return SyncPromptSpec.builder()
                                    .prompt(mcpPrompt)
                                    .promptHandler(methodCallback)
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (promptSpecs.isEmpty()) {
            logger.warn("No prompt methods found in the provided prompt objects: {}", this.promptObjects);
        }
        return promptSpecs;
    }
}
