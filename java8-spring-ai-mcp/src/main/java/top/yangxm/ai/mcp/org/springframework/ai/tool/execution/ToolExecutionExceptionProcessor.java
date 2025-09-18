package top.yangxm.ai.mcp.org.springframework.ai.tool.execution;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ToolExecutionExceptionProcessor {
    String process(ToolExecutionException exception);
}
