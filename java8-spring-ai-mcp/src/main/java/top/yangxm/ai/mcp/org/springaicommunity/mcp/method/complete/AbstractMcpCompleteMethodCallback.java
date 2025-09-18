package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.complete;

import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpUriTemplateManager;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.PromptReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceReference;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.adapter.CompleteAdapter;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpComplete;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpMeta;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpProgressToken;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class AbstractMcpCompleteMethodCallback {
    protected final Method method;
    protected final Object bean;
    protected final String prompt;
    protected final String uri;
    protected final CompleteReference completeReference;
    protected final List<String> uriVariables;
    protected final McpUriTemplateManager uriTemplateManager;

    protected AbstractMcpCompleteMethodCallback(Method method, Object bean, String prompt, String uri,
                                                McpUriTemplateManager.Factory uriTemplateManagerFactory) {
        Assert.notNull(method, "Method can't be null!");
        Assert.notNull(bean, "Bean can't be null!");
        Assert.notNull(uriTemplateManagerFactory, "URI template manager factory can't be null!");

        if ((prompt == null || prompt.isEmpty()) && (uri == null || uri.isEmpty())) {
            throw new IllegalArgumentException("Either prompt or uri must be provided!");
        }
        if ((prompt != null && !prompt.isEmpty()) && (uri != null && !uri.isEmpty())) {
            throw new IllegalArgumentException("Only one of prompt or uri can be provided!");
        }

        this.method = method;
        this.bean = bean;
        this.prompt = prompt;
        this.uri = uri;

        if (prompt != null && !prompt.isEmpty()) {
            this.completeReference = new PromptReference(prompt);
        } else {
            this.completeReference = new ResourceReference(uri);
        }

        if (uri != null && !uri.isEmpty()) {
            this.uriTemplateManager = uriTemplateManagerFactory.create(this.uri);
            this.uriVariables = this.uriTemplateManager.getVariableNames();
        } else {
            this.uriTemplateManager = null;
            this.uriVariables = new ArrayList<>();
        }
    }

    protected void validateMethod(Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method must not be null");
        }
        this.validateReturnType(method);
        this.validateParameters(method);
    }

    protected abstract void validateReturnType(Method method);

    protected void validateParameters(Method method) {
        String mName = method.getName();
        String mClass = method.getDeclaringClass().getName();
        Parameter[] parameters = method.getParameters();

        int nonSpecialParamCount = 0;
        for (Parameter param : parameters) {
            if (!param.isAnnotationPresent(McpProgressToken.class)
                    && !McpMeta.class.isAssignableFrom(param.getType())) {
                nonSpecialParamCount++;
            }
        }

        if (nonSpecialParamCount > 3) {
            throw new IllegalArgumentException(
                    String.format("Method can have at most 3 input parameters " +
                            "(excluding @McpProgressToken and McpMeta): %s in %s has %d parameters", mName, mClass, nonSpecialParamCount)
            );
        }

        boolean hasExchangeParam = false;
        boolean hasRequestParam = false;
        boolean hasArgumentParam = false;
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

            if (isExchangeType(paramType)) {
                if (hasExchangeParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one exchange parameter: %s in %s", mName, mClass)
                    );
                }
                hasExchangeParam = true;
            } else if (CompleteRequest.class.isAssignableFrom(paramType)) {
                if (hasRequestParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one CompleteRequest parameter: %s in %s", mName, mClass)
                    );
                }
                hasRequestParam = true;
            } else if (CompleteRequest.CompleteArgument.class.isAssignableFrom(paramType)) {
                if (hasArgumentParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one CompleteArgument parameter: %s in %s", mName, mClass)
                    );
                }
                hasArgumentParam = true;
            } else if (!String.class.isAssignableFrom(paramType)) {
                throw new IllegalArgumentException(
                        String.format("Method parameters must be exchange, CompleteRequest, CompleteArgument, " +
                                "or String: %s in %s has parameter of type %s", mName, mClass, paramType.getName())
                );
            }
        }
    }

    protected Object[] buildArgs(Method method, Object exchange, CompleteRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (param.isAnnotationPresent(McpProgressToken.class)) {
                args[i] = null;
            } else if (McpMeta.class.isAssignableFrom(paramType)) {
                args[i] = request != null ? new McpMeta(request.meta()) : new McpMeta(null);
            } else if (isExchangeType(paramType)) {
                args[i] = exchange;
            } else if (CompleteRequest.class.isAssignableFrom(paramType)) {
                args[i] = request;
            } else if (CompleteRequest.CompleteArgument.class.isAssignableFrom(paramType)) {
                args[i] = request != null ? request.argument() : null;
            } else if (String.class.isAssignableFrom(paramType)) {
                args[i] = request != null && request.argument() != null ? request.argument().value() : null;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    protected abstract boolean isExchangeType(Class<?> paramType);


    public static class McpCompleteMethodException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public McpCompleteMethodException(String message, Throwable cause) {
            super(message, cause);
        }

        public McpCompleteMethodException(String message) {
            super(message);
        }
    }

    protected abstract static class AbstractBuilder<T extends AbstractBuilder<T, R>, R> {
        protected Method method;
        protected Object bean;
        protected McpUriTemplateManager.Factory uriTemplateManagerFactory;
        protected String prompt;
        protected String uri;

        @SuppressWarnings("unchecked")
        public T method(Method method) {
            this.method = method;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T bean(Object bean) {
            this.bean = bean;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T prompt(String prompt) {
            this.prompt = prompt;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T uri(String uri) {
            this.uri = uri;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T reference(CompleteReference completeReference) {
            if (completeReference instanceof PromptReference) {
                PromptReference promptRef = (PromptReference) completeReference;
                this.prompt = promptRef.name();
                this.uri = "";
            } else if (completeReference instanceof ResourceReference) {
                ResourceReference resourceRef = (ResourceReference) completeReference;
                this.prompt = "";
                this.uri = resourceRef.uri();
            }
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T complete(McpComplete complete) {
            CompleteReference completeRef = CompleteAdapter.asCompleteReference(complete);
            if (completeRef instanceof PromptReference) {
                PromptReference promptRef = (PromptReference) completeRef;
                this.prompt = promptRef.name();
                this.uri = "";
            } else if (completeRef instanceof ResourceReference) {
                ResourceReference resourceRef = (ResourceReference) completeRef;
                this.prompt = "";
                this.uri = resourceRef.uri();
            }
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T uriTemplateManagerFactory(McpUriTemplateManager.Factory uriTemplateManagerFactory) {
            this.uriTemplateManagerFactory = uriTemplateManagerFactory;
            return (T) this;
        }

        protected void validate() {
            if (method == null) {
                throw new IllegalArgumentException("Method must not be null");
            }
            if (bean == null) {
                throw new IllegalArgumentException("Bean must not be null");
            }
            if ((prompt == null || prompt.isEmpty()) && (uri == null || uri.isEmpty())) {
                throw new IllegalArgumentException("Either prompt or uri must be provided");
            }
            if ((prompt != null && !prompt.isEmpty()) && (uri != null && !uri.isEmpty())) {
                throw new IllegalArgumentException("Only one of prompt or uri can be provided");
            }
            if (this.uriTemplateManagerFactory == null) {
                this.uriTemplateManagerFactory = McpUriTemplateManager.DEFAULT_FACTORY;
            }
        }

        public abstract R build();
    }
}
