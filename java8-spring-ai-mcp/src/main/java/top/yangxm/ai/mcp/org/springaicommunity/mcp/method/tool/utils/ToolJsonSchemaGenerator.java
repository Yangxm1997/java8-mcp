package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool.utils;

import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.schema.JsonSchemaGenerator;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.ClassUtils;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.commons.util.Maps;
import top.yangxm.ai.mcp.commons.util.Utils;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpAsyncServerExchange;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpSyncServerExchange;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpMeta;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpProgressToken;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpToolParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class ToolJsonSchemaGenerator {
    private static final JsonMapper JSON_MAPPER = JsonMapper.getDefault();
    private static final JsonSchemaGenerator JSON_SCHEMA_GENERATOR = JsonSchemaGenerator.getDefault();
    private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;
    private static final Map<Method, String> methodSchemaCache = new ConcurrentReferenceHashMap<>(256);
    private static final Map<Class<?>, String> classSchemaCache = new ConcurrentReferenceHashMap<>(256);
    private static final Map<Type, String> typeSchemaCache = new ConcurrentReferenceHashMap<>(256);


    public static String generateForMethodInput(Method method) {
        Assert.notNull(method, "method cannot be null");
        return methodSchemaCache.computeIfAbsent(method, ToolJsonSchemaGenerator::internalGenerateFromMethodArguments);
    }

    private static String internalGenerateFromMethodArguments(Method method) {
        // 检查此方法是否有CallToolRequest参数
        boolean hasCallToolRequestParam = Arrays.stream(method.getParameterTypes())
                .anyMatch(CallToolRequest.class::isAssignableFrom);

        // 如果此方法有CallToolRequest参数，返回最小化的schema
        if (hasCallToolRequestParam) {
            // 检查除了CallToolRequest，是否还含有以下类型的参数
            // McpSyncServerExchange, McpAsyncServerExchange
            // @McpProgressToken, McpMeta
            boolean hasOtherParams = Arrays.stream(method.getParameters())
                    .anyMatch(param -> {
                        Class<?> type = param.getType();
                        return !CallToolRequest.class.isAssignableFrom(type)
                                && !McpSyncServerExchange.class.isAssignableFrom(type)
                                && !McpAsyncServerExchange.class.isAssignableFrom(type)
                                && !param.isAnnotationPresent(McpProgressToken.class)
                                && !McpMeta.class.isAssignableFrom(type);
                    });

            // 如果只有CallToolRequest，返回空的schema
            if (!hasOtherParams) {
                Map<String, Object> schema = new HashMap<>();
                schema.put("type", "object");
                schema.put("properties", Maps.of());
                schema.put("required", Lists.of());
                return JSON_MAPPER.writeValueAsString(schema);
            }
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        Set<String> required = new LinkedHashSet<>();
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);

        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];
            String parameterName = parameter.getName();
            Type parameterType = method.getGenericParameterTypes()[i];

            // 忽略@McpProgressToken类型的参数
            if (parameter.isAnnotationPresent(McpProgressToken.class)) {
                continue;
            }

            // 忽略McpMeta类型的参数
            if (parameterType instanceof Class<?>) {
                Class<?> parameterClass = (Class<?>) parameterType;
                if (McpMeta.class.isAssignableFrom(parameterClass)) {
                    continue;
                }
            }

            // 忽略一些特定类型的参数
            if (parameterType instanceof Class<?>) {
                Class<?> parameterClass = (Class<?>) parameterType;
                if (ClassUtils.isAssignable(McpSyncServerExchange.class, parameterClass)
                        || ClassUtils.isAssignable(McpAsyncServerExchange.class, parameterClass)
                        || ClassUtils.isAssignable(CallToolRequest.class, parameterClass)) {
                    continue;
                }
            }

            if (isMethodParameterRequired(method, i)) {
                required.add(parameterName);
            }

            Map<String, Object> parameterSchema = JSON_SCHEMA_GENERATOR.generateMapSchema(parameterType);
            String parameterDescription = getMethodParameterDescription(method, i);
            if (Utils.hasText(parameterDescription)) {
                parameterSchema.put("description", parameterDescription);
            }
            properties.put(parameterName, parameterSchema);
        }
        return JSON_MAPPER.writeValueAsString(schema);
    }

    public static String generateFromClass(Class<?> clazz) {
        Assert.notNull(clazz, "clazz cannot be null");
        return classSchemaCache.computeIfAbsent(clazz, ToolJsonSchemaGenerator::internalGenerateFromClass);
    }

    private static String internalGenerateFromClass(Class<?> clazz) {
        return JSON_SCHEMA_GENERATOR.generateStringSchema(clazz);
    }

    public static String generateFromType(Type type) {
        Assert.notNull(type, "type cannot be null");
        return typeSchemaCache.computeIfAbsent(type, ToolJsonSchemaGenerator::internalGenerateFromType);
    }

    private static String internalGenerateFromType(Type type) {
        return JSON_SCHEMA_GENERATOR.generateStringSchema(type);
    }

    public static boolean hasCallToolRequestParameter(Method method) {
        return Arrays.stream(method.getParameterTypes()).anyMatch(CallToolRequest.class::isAssignableFrom);
    }

    private static boolean isMethodParameterRequired(Method method, int index) {
        Parameter parameter = method.getParameters()[index];
        McpToolParam toolParamAnnotation = parameter.getAnnotation(McpToolParam.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        }
        return PROPERTY_REQUIRED_BY_DEFAULT;
    }

    private static String getMethodParameterDescription(Method method, int index) {
        Parameter parameter = method.getParameters()[index];
        McpToolParam toolParamAnnotation = parameter.getAnnotation(McpToolParam.class);
        if (toolParamAnnotation != null && Utils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }
        return null;
    }
}