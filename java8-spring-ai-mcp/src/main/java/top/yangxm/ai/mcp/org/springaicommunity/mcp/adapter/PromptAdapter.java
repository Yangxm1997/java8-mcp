package top.yangxm.ai.mcp.org.springaicommunity.mcp.adapter;

import org.apache.commons.lang3.StringUtils;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Prompt;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpArg;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpPrompt;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class PromptAdapter {
    private PromptAdapter() {
    }

    public static Prompt asPrompt(McpPrompt mcpPrompt) {
        return new Prompt(mcpPrompt.name(), mcpPrompt.title(), mcpPrompt.description(), Lists.of());
    }

    public static McpSchema.Prompt asPrompt(McpPrompt mcpPrompt, Method method) {
        List<McpSchema.PromptArgument> arguments = extractPromptArguments(method);
        return new McpSchema.Prompt(getName(mcpPrompt, method), mcpPrompt.title(), mcpPrompt.description(), arguments);
    }

    private static String getName(McpPrompt promptAnnotation, Method method) {
        Assert.notNull(method, "method cannot be null");
        if (promptAnnotation == null || StringUtils.isBlank(promptAnnotation.name())) {
            return method.getName();
        }
        return promptAnnotation.name();
    }

    private static List<McpSchema.PromptArgument> extractPromptArguments(Method method) {
        List<McpSchema.PromptArgument> arguments = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (Parameter parameter : parameters) {
            if (McpAsyncServerExchange.class.isAssignableFrom(parameter.getType())
                    || McpSchema.GetPromptRequest.class.isAssignableFrom(parameter.getType())
                    || java.util.Map.class.isAssignableFrom(parameter.getType())) {
                continue;
            }

            McpArg mcpArg = parameter.getAnnotation(McpArg.class);
            if (mcpArg != null) {
                String name = !mcpArg.name().isEmpty() ? mcpArg.name() : parameter.getName();
                arguments.add(new McpSchema.PromptArgument(name, mcpArg.description(), mcpArg.required()));
            } else {
                arguments.add(new McpSchema.PromptArgument(parameter.getName(),
                        "Parameter of type " + parameter.getType().getSimpleName(), false));
            }
        }
        return arguments;
    }

    private interface McpAsyncServerExchange {
    }
}
