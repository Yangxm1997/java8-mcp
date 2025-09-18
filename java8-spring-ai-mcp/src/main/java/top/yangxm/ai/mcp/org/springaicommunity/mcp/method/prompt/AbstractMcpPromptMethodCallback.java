package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.prompt;

import org.apache.commons.lang3.StringUtils;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Prompt;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.PromptMessage;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Role;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.TextContent;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpArg;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpMeta;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpProgressToken;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public abstract class AbstractMcpPromptMethodCallback {
    protected final Method method;
    protected final Object bean;
    protected final Prompt prompt;

    protected AbstractMcpPromptMethodCallback(Method method, Object bean, Prompt prompt) {
        this.method = method;
        this.bean = bean;
        this.prompt = prompt;
        this.validateMethod(this.method);
    }

    protected void validateMethod(Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method must not be null");
        }
        this.validateReturnType(method);
        this.validateParameters(method);
    }

    protected abstract void validateReturnType(Method method);

    protected abstract boolean isExchangeOrContextType(Class<?> paramType);

    protected void validateParameters(Method method) {
        String mName = method.getName();
        String mClass = method.getDeclaringClass().getName();
        Parameter[] parameters = method.getParameters();

        boolean hasExchangeParam = false;
        boolean hasRequestParam = false;
        boolean hasMapParam = false;
        boolean hasProgressTokenParam = false;
        boolean hasMetaParam = false;

        for (Parameter param : parameters) {
            Class<?> paramType = param.getType();
            if (param.isAnnotationPresent(McpProgressToken.class)) {
                if (hasProgressTokenParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one @McpProgressToken parameter: %s in %s", mName, mClass)
                    );
                }
                hasProgressTokenParam = true;
                continue;
            }

            if (McpMeta.class.isAssignableFrom(paramType)) {
                if (hasMetaParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one McpMeta parameter: %s in %s", mName, mClass)
                    );
                }
                hasMetaParam = true;
                continue;
            }

            if (isExchangeOrContextType(paramType)) {
                if (hasExchangeParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one exchange parameter: %s in %s", mName, mClass)
                    );
                }
                hasExchangeParam = true;
            } else if (GetPromptRequest.class.isAssignableFrom(paramType)) {
                if (hasRequestParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one GetPromptRequest parameter: %s in %s", mName, mClass)
                    );
                }
                hasRequestParam = true;
            } else if (Map.class.isAssignableFrom(paramType)) {
                if (hasMapParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one Map parameter: %s in %s", mName, mClass)
                    );
                }
                hasMapParam = true;
            }
        }
    }

    protected Object[] buildArgs(Method method, Object exchange, GetPromptRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(McpProgressToken.class)) {
                args[i] = null;
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            if (McpMeta.class.isAssignableFrom(parameters[i].getType())) {
                args[i] = request != null ? new McpMeta(request.meta()) : new McpMeta(null);
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            if (args[i] != null
                    || parameters[i].isAnnotationPresent(McpProgressToken.class)
                    || McpMeta.class.isAssignableFrom(parameters[i].getType())) {
                continue;
            }

            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (isExchangeOrContextType(paramType)) {
                args[i] = exchange;
            } else if (GetPromptRequest.class.isAssignableFrom(paramType)) {
                args[i] = request;
            } else if (Map.class.isAssignableFrom(paramType)) {
                args[i] = request != null && request.arguments() != null ? request.arguments() : new HashMap<>();
            } else {
                McpArg arg = param.getAnnotation(McpArg.class);
                String paramName = arg != null && !StringUtils.isBlank(arg.name()) ? arg.name() : param.getName();
                if (request != null && request.arguments() != null && request.arguments().containsKey(paramName)) {
                    Object argValue = request.arguments().get(paramName);
                    args[i] = convertArgumentValue(argValue, paramType);
                } else {
                    args[i] = null;
                }
            }
        }
        return args;
    }

    protected Object convertArgumentValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } else if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else {
                return Long.parseLong(value.toString());
            }
        } else if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                return Double.parseDouble(value.toString());
            }
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    protected GetPromptResult convertToGetPromptResult(Object result) {
        if (result instanceof GetPromptResult) {
            return (GetPromptResult) result;
        } else if (result instanceof List) {
            List<?> list = (List<?>) result;
            if (!list.isEmpty()) {
                if (list.get(0) instanceof PromptMessage) {
                    return new GetPromptResult(null, (List<PromptMessage>) list);
                } else if (list.get(0) instanceof String) {
                    List<PromptMessage> messages = ((List<String>) list).stream()
                            .map(text -> new PromptMessage(Role.ASSISTANT,
                                    new TextContent(text)))
                            .collect(Collectors.toList());
                    return new GetPromptResult(null, messages);
                }
            }
        } else if (result instanceof PromptMessage) {
            return new GetPromptResult(null, Lists.of((PromptMessage) result));
        } else if (result instanceof String) {
            return new GetPromptResult(null, Lists.of(new PromptMessage(Role.ASSISTANT, new TextContent((String) result))));
        }
        throw new IllegalArgumentException("Unsupported result type: " + (result != null ? result.getClass().getName() : "null"));
    }

    protected abstract static class AbstractBuilder<B extends AbstractBuilder<B, T>, T extends AbstractMcpPromptMethodCallback> {
        protected Method method;
        protected Object bean;
        protected Prompt prompt;

        @SuppressWarnings("unchecked")
        public B method(Method method) {
            this.method = method;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B bean(Object bean) {
            this.bean = bean;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B prompt(Prompt prompt) {
            this.prompt = prompt;
            return (B) this;
        }

        protected void validate() {
            Assert.notNull(method, "Method must not be null");
            Assert.notNull(bean, "Bean must not be null");
            Assert.notNull(prompt, "Prompt must not be null");
        }

        public abstract T build();

    }

    public static class McpPromptMethodException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public McpPromptMethodException(String message, Throwable cause) {
            super(message, cause);
        }

        public McpPromptMethodException(String message) {
            super(message);
        }
    }
}
