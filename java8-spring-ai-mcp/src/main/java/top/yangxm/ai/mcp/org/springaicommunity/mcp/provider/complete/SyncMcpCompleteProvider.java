package top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.complete;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncCompletionSpec;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.adapter.CompleteAdapter;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpComplete;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.method.complete.SyncMcpCompleteMethodCallback;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SyncMcpCompleteProvider extends AbstractMcpCompleteProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(SyncMcpCompleteProvider.class);

    public SyncMcpCompleteProvider(List<Object> completeObjects) {
        super(completeObjects);
    }

    public List<SyncCompletionSpec> getCompleteSpecs() {
        List<SyncCompletionSpec> completeSpecs = this.completeObjects.stream()
                .map(completeObject -> Stream.of(doGetClassMethods(completeObject))
                        .filter(method -> method.isAnnotationPresent(McpComplete.class))
                        .filter(method -> !Mono.class.isAssignableFrom(method.getReturnType()))
                        .sorted(Comparator.comparing(Method::getName))
                        .map(mcpCompleteMethod -> {
                            McpComplete completeAnnotation = mcpCompleteMethod.getAnnotation(McpComplete.class);
                            CompleteReference completeRef = CompleteAdapter.asCompleteReference(completeAnnotation, mcpCompleteMethod);

                            SyncMcpCompleteMethodCallback methodCallback = SyncMcpCompleteMethodCallback.builder()
                                    .method(mcpCompleteMethod)
                                    .bean(completeObject)
                                    .reference(completeRef)
                                    .build();

                            return SyncCompletionSpec.builder()
                                    .reference(completeRef)
                                    .completionHandler(methodCallback)
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (completeSpecs.isEmpty()) {
            logger.warn("No complete methods found in the provided complete objects: {}", this.completeObjects);
        }
        return completeSpecs;
    }
}
