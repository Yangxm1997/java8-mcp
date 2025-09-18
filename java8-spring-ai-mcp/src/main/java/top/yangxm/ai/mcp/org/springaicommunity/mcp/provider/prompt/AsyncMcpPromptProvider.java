package top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Prompt;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.adapter.PromptAdapter;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpPrompt;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.method.prompt.AsyncMcpPromptMethodCallback;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class AsyncMcpPromptProvider extends AbstractMcpPromptProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(AsyncMcpPromptProvider.class);

    public AsyncMcpPromptProvider(List<Object> promptObjects) {
        super(promptObjects);
    }

    public List<AsyncPromptSpec> getPromptSpecs() {
        List<AsyncPromptSpec> promptSpecs = this.promptObjects.stream()
                .map(promptObject -> Stream.of(doGetClassMethods(promptObject))
                        .filter(method -> method.isAnnotationPresent(McpPrompt.class))
                        .filter(method -> Mono.class.isAssignableFrom(method.getReturnType())
                                || Flux.class.isAssignableFrom(method.getReturnType())
                                || Publisher.class.isAssignableFrom(method.getReturnType()))
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpPromptMethod -> {
                            McpPrompt promptAnnotation = mcpPromptMethod.getAnnotation(McpPrompt.class);
                            Prompt mcpPrompt = PromptAdapter.asPrompt(promptAnnotation, mcpPromptMethod);

                            AsyncMcpPromptMethodCallback methodCallback = AsyncMcpPromptMethodCallback
                                    .builder()
                                    .method(mcpPromptMethod)
                                    .bean(promptObject)
                                    .prompt(mcpPrompt)
                                    .build();

                            return AsyncPromptSpec.builder()
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
