package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolResult;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class AsyncStatelessMcpToolMethodCallback
        extends AbstractAsyncMcpToolMethodCallback<McpTransportContext>
        implements BiFunction<McpTransportContext, CallToolRequest, Mono<CallToolResult>> {

    public AsyncStatelessMcpToolMethodCallback(ReturnMode returnMode,
                                               Method toolMethod,
                                               Object toolObject) {
        super(returnMode, toolMethod, toolObject, Exception.class);
    }

    public AsyncStatelessMcpToolMethodCallback(ReturnMode returnMode,
                                               Method toolMethod,
                                               Object toolObject,
                                               Class<? extends Throwable> toolCallExceptionClass) {
        super(returnMode, toolMethod, toolObject, toolCallExceptionClass);
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpTransportContext.class.isAssignableFrom(paramType);
    }

    @Override
    public Mono<CallToolResult> apply(McpTransportContext context, CallToolRequest request) {
        return validateRequest(request).then(Mono.defer(() -> {
            try {
                Object[] args = this.buildMethodArguments(context, request.arguments(), request);
                Object result = this.callMethod(args);
                return this.convertToCallToolResult(result);
            } catch (Exception e) {
                if (this.toolCallExceptionClass.isInstance(e)) {
                    return this.createErrorResult(e);
                }
                return Mono.error(e);
            }
        }));
    }


}
