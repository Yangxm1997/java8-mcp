package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolResult;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class SyncStatelessMcpToolMethodCallback
        extends AbstractSyncMcpToolMethodCallback<McpTransportContext>
        implements BiFunction<McpTransportContext, CallToolRequest, CallToolResult> {

    public SyncStatelessMcpToolMethodCallback(ReturnMode returnMode,
                                              Method toolMethod,
                                              Object toolObject) {
        super(returnMode, toolMethod, toolObject, Exception.class);
    }

    public SyncStatelessMcpToolMethodCallback(ReturnMode returnMode,
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
    public CallToolResult apply(McpTransportContext context, CallToolRequest request) {
        validateRequest(request);
        try {
            Object[] args = this.buildMethodArguments(context, request.arguments(), request);
            Object result = this.callMethod(args);
            return this.processResult(result);
        } catch (Exception e) {
            if (this.toolCallExceptionClass.isInstance(e)) {
                return this.createErrorResult(e);
            }
            throw e;
        }
    }
}
