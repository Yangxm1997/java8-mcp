package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.resource;

import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpUriTemplateManager;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Resource;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceTemplate;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpMeta;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpProgressToken;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class AbstractMcpResourceMethodCallback {
    public enum ContentType {
        TEXT,
        BLOB
    }

    protected final Method method;
    protected final Object bean;
    protected final String uri;
    protected final String name;
    protected final String description;
    protected final String mimeType;
    protected final List<String> uriVariables;
    protected final McpReadResourceResultConverter resultConverter;
    protected final McpUriTemplateManager uriTemplateManager;
    protected final ContentType contentType;

    protected AbstractMcpResourceMethodCallback(Method method, Object bean, String uri, String name, String description,
                                                String mimeType, McpReadResourceResultConverter resultConverter,
                                                McpUriTemplateManager.Factory uriTemplateMangerFactory, ContentType contentType) {
        Assert.hasText(uri, "URI can't be null or empty!");
        Assert.notNull(method, "Method can't be null!");
        Assert.notNull(bean, "Bean can't be null!");
        Assert.notNull(resultConverter, "Result converter can't be null!");
        Assert.notNull(uriTemplateMangerFactory, "URI template manager factory can't be null!");

        this.method = method;
        this.bean = bean;
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.resultConverter = resultConverter;
        this.uriTemplateManager = uriTemplateMangerFactory.create(this.uri);
        this.uriVariables = this.uriTemplateManager.getVariableNames();
        this.contentType = contentType;
    }

    protected void validateMethod(Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method must not be null");
        }
        this.validateReturnType(method);
        if (this.uriVariables.isEmpty()) {
            this.validateParametersWithoutUriVariables(method);
        } else {
            this.validateParametersWithUriVariables(method);
        }
    }

    protected abstract void validateReturnType(Method method);

    protected void validateParametersWithoutUriVariables(Method method) {
        String mName = method.getName();
        String mClass = method.getDeclaringClass().getName();
        Parameter[] parameters = method.getParameters();
        int nonSpecialParamCount = 0;
        for (Parameter param : parameters) {
            if (!param.isAnnotationPresent(McpProgressToken.class) && !McpMeta.class.isAssignableFrom(param.getType())) {
                nonSpecialParamCount++;
            }
        }

        if (nonSpecialParamCount > 2) {
            throw new IllegalArgumentException(
                    String.format("Method can have at most 2 input parameters (excluding @McpProgressToken and McpMeta) " +
                            "when no URI variables are present: " +
                            "%s in %s has %d non-special parameters", mName, mClass, nonSpecialParamCount)
            );
        }

        boolean hasValidParams = false;
        boolean hasExchangeParam = false;
        boolean hasRequestOrUriParam = false;
        boolean hasMetaParam = false;

        for (Parameter param : parameters) {
            if (param.isAnnotationPresent(McpProgressToken.class)) {
                continue;
            }
            Class<?> paramType = param.getType();

            if (McpMeta.class.isAssignableFrom(paramType)) {
                if (hasMetaParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one McpMeta parameter: %s in %s", mName, mClass)
                    );
                }
                hasMetaParam = true;
            } else if (isExchangeOrContextType(paramType)) {
                if (hasExchangeParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one exchange parameter: %s in %s", mName, mClass)
                    );
                }
                hasExchangeParam = true;
            } else if (ReadResourceRequest.class.isAssignableFrom(paramType) || String.class.isAssignableFrom(paramType)) {
                if (hasRequestOrUriParam) {
                    throw new IllegalArgumentException(
                            String.format("Method cannot have more than one ReadResourceRequest " +
                                    "or String parameter: %s in %s", mName, mClass)
                    );
                }
                hasRequestOrUriParam = true;
                hasValidParams = true;
            } else {
                throw new IllegalArgumentException(
                        String.format("Method parameters must be exchange, ReadResourceRequest, String, McpMeta, " +
                                "or @McpProgressToken when no URI variables are present: %s in %s " +
                                "has parameter of type %s", mName, mClass, paramType.getName())
                );
            }
        }

        if (!hasValidParams && nonSpecialParamCount > 0) {
            throw new IllegalArgumentException(
                    String.format("Method must have either ReadResourceRequest or String parameter " +
                            "when no URI variables are present: %s in %s", mName, mClass)
            );
        }
    }

    protected void validateParametersWithUriVariables(Method method) {
        String mName = method.getName();
        String mClass = method.getDeclaringClass().getName();
        Parameter[] parameters = method.getParameters();
        int exchangeParamCount = 0;
        int requestParamCount = 0;
        int progressTokenParamCount = 0;
        int metaParamCount = 0;

        for (Parameter param : parameters) {
            if (param.isAnnotationPresent(McpProgressToken.class)) {
                progressTokenParamCount++;
            } else {
                Class<?> paramType = param.getType();
                if (McpMeta.class.isAssignableFrom(paramType)) {
                    metaParamCount++;
                } else if (isExchangeOrContextType(paramType)) {
                    exchangeParamCount++;
                } else if (ReadResourceRequest.class.isAssignableFrom(paramType)) {
                    requestParamCount++;
                }
            }
        }

        if (exchangeParamCount > 1) {
            throw new IllegalArgumentException(
                    String.format("Method cannot have more than one exchange parameter: %s in %s", mName, mClass)
            );
        }

        if (requestParamCount > 1) {
            throw new IllegalArgumentException(
                    String.format("Method cannot have more than one ReadResourceRequest parameter: %s in %s", mName, mClass)
            );
        }

        if (metaParamCount > 1) {
            throw new IllegalArgumentException(
                    String.format("Method cannot have more than one McpMeta parameter: %s in %s", mName, mClass)
            );
        }

        int specialParamCount = exchangeParamCount + requestParamCount + progressTokenParamCount + metaParamCount;
        int uriVarParamCount = parameters.length - specialParamCount;

        if (uriVarParamCount != this.uriVariables.size()) {
            throw new IllegalArgumentException(
                    String.format("Method must have parameters for all URI variables. " +
                                    "Expected %d URI variable parameters, but found %d: " +
                                    "%s in %s. URI variables: %s",
                            this.uriVariables.size(), uriVarParamCount, mName, mClass, this.uriVariables)
            );
        }

        for (Parameter param : parameters) {
            if (param.isAnnotationPresent(McpProgressToken.class)) {
                continue;
            }
            Class<?> paramType = param.getType();
            if (!isExchangeOrContextType(paramType)
                    && !ReadResourceRequest.class.isAssignableFrom(paramType)
                    && !McpMeta.class.isAssignableFrom(paramType)
                    && !String.class.isAssignableFrom(paramType)) {
                throw new IllegalArgumentException(
                        String.format("URI variable parameters must be of type String: " +
                                        "%s in %s, parameter of type %s is not valid",
                                mName, mClass, paramType.getName())
                );
            }
        }
    }

    protected Object[] buildArgs(Method method,
                                 Object exchange,
                                 ReadResourceRequest request,
                                 Map<String, String> uriVariableValues) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(McpProgressToken.class)) {
                args[i] = request != null ? request.progressToken() : null;
            } else if (McpMeta.class.isAssignableFrom(parameters[i].getType())) {
                args[i] = request != null ? new McpMeta(request.meta()) : new McpMeta(null);
            }
        }
        if (!this.uriVariables.isEmpty()) {
            this.buildArgsWithUriVariables(parameters, args, exchange, request, uriVariableValues);
        } else {
            this.buildArgsWithoutUriVariables(parameters, args, exchange, request);
        }
        return args;
    }

    protected void buildArgsWithUriVariables(Parameter[] parameters,
                                             Object[] args,
                                             Object exchange,
                                             ReadResourceRequest request,
                                             Map<String, String> uriVariableValues) {
        List<String> assignedVariables = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(McpProgressToken.class)
                    || McpMeta.class.isAssignableFrom(parameters[i].getType())) {
                continue;
            }

            Class<?> paramType = parameters[i].getType();
            if (isExchangeOrContextType(paramType)) {
                args[i] = exchange;
            } else if (ReadResourceRequest.class.isAssignableFrom(paramType)) {
                args[i] = request;
            }
        }

        int variableIndex = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(McpProgressToken.class)
                    || McpMeta.class.isAssignableFrom(parameters[i].getType())
                    || args[i] != null) {
                continue;
            }

            if (variableIndex < this.uriVariables.size()) {
                String variableName = this.uriVariables.get(variableIndex);
                args[i] = uriVariableValues.get(variableName);
                assignedVariables.add(variableName);
                variableIndex++;
            }
        }

        if (assignedVariables.size() != this.uriVariables.size()) {
            throw new IllegalArgumentException(String.format("Failed to assign all URI variables to method parameters. " +
                    "Assigned: %s, Expected: %s", assignedVariables, this.uriVariables)
            );
        }
    }

    protected void buildArgsWithoutUriVariables(Parameter[] parameters,
                                                Object[] args,
                                                Object exchange,
                                                ReadResourceRequest request) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(McpProgressToken.class)
                    || McpMeta.class.isAssignableFrom(parameters[i].getType())) {
                continue;
            }

            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (isExchangeOrContextType(paramType)) {
                args[i] = exchange;
            } else if (ReadResourceRequest.class.isAssignableFrom(paramType)) {
                args[i] = request;
            } else if (String.class.isAssignableFrom(paramType)) {
                args[i] = request.uri();
            } else {
                args[i] = null;
            }
        }
    }


    protected abstract boolean isExchangeOrContextType(Class<?> paramType);


    public ContentType contentType() {
        return this.contentType;
    }


    public static class McpResourceMethodException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public McpResourceMethodException(String message, Throwable cause) {
            super(message, cause);
        }

        public McpResourceMethodException(String message) {
            super(message);
        }
    }

    protected abstract static class AbstractBuilder<T extends AbstractBuilder<T, R>, R> {
        protected Method method;
        protected Object bean;
        protected McpReadResourceResultConverter resultConverter;
        protected McpUriTemplateManager.Factory uriTemplateManagerFactory;
        protected ContentType contentType;
        protected String name;
        protected String description;
        protected String mimeType;
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
        public T uri(String uri) {
            this.uri = uri;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T resource(Resource resource) {
            this.uri = resource.uri();
            this.name = resource.name();
            this.description = resource.description();
            this.mimeType = resource.mimeType();
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T resource(ResourceTemplate resourceTemplate) {
            this.uri = resourceTemplate.uriTemplate();
            this.name = resourceTemplate.name();
            this.description = resourceTemplate.description();
            this.mimeType = resourceTemplate.mimeType();
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T resultConverter(McpReadResourceResultConverter resultConverter) {
            this.resultConverter = resultConverter;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T uriTemplateManagerFactory(McpUriTemplateManager.Factory uriTemplateManagerFactory) {
            this.uriTemplateManagerFactory = uriTemplateManagerFactory;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T contentType(ContentType contentType) {
            this.contentType = contentType;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T name(String name) {
            this.name = name;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T description(String description) {
            this.description = description;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T mimeType(String mimeType) {
            this.mimeType = mimeType;
            return (T) this;
        }

        protected void validate() {
            if (method == null) {
                throw new IllegalArgumentException("Method must not be null");
            }
            if (bean == null) {
                throw new IllegalArgumentException("Bean must not be null");
            }
            if (this.uri == null || this.uri.isEmpty()) {
                throw new IllegalArgumentException("URI must not be null or empty");
            }
            if (this.uriTemplateManagerFactory == null) {
                this.uriTemplateManagerFactory = McpUriTemplateManager.DEFAULT_FACTORY;
            }
            if (this.mimeType == null) {
                this.mimeType = "text/plain";
            }

            if (this.name == null) {
                this.name = method.getName();
            }
        }

        public abstract R build();
    }
}
