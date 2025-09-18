package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import io.swagger.v3.oas.annotations.media.Schema;
import reactor.util.annotation.Nullable;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.Utils;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpAsyncServerExchange;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpSyncServerExchange;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpMeta;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpProgressToken;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpToolParam;
import top.yangxm.ai.mcp.org.springframework.ai.util.JsonParser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class JsonSchemaGenerator {
    private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;
    private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;
    private static final SchemaGenerator SUBTYPE_SCHEMA_GENERATOR;
    private static final Map<Method, String> methodSchemaCache = new ConcurrentReferenceHashMap<>(256);
    private static final Map<Class<?>, String> classSchemaCache = new ConcurrentReferenceHashMap<>(256);
    private static final Map<Type, String> typeSchemaCache = new ConcurrentReferenceHashMap<>(256);

    static {
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        Module openApiModule = new Swagger2Module();
        Module springAiSchemaModule = PROPERTY_REQUIRED_BY_DEFAULT ? new SpringAiSchemaModule()
                : new SpringAiSchemaModule(SpringAiSchemaModule.Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT);

        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(jacksonModule)
                .with(openApiModule)
                .with(springAiSchemaModule)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.STANDARD_FORMATS)
                .with(Option.PLAIN_DEFINITION_KEYS);

        SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();
        TYPE_SCHEMA_GENERATOR = new SchemaGenerator(typeSchemaGeneratorConfig);

        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .build();
        SUBTYPE_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
    }

    public static String generateForMethodInput(Method method) {
        Assert.notNull(method, "method cannot be null");
        return methodSchemaCache.computeIfAbsent(method, JsonSchemaGenerator::internalGenerateFromMethodArguments);
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
                ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
                schema.put("type", "object");
                schema.putObject("properties");
                schema.putArray("required");
                return schema.toPrettyString();
            }
        }

        ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
        schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        List<String> required = new ArrayList<>();

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
            ObjectNode parameterNode = SUBTYPE_SCHEMA_GENERATOR.generateSchema(parameterType);
            String parameterDescription = getMethodParameterDescription(method, i);
            if (Utils.hasText(parameterDescription)) {
                parameterNode.put("description", parameterDescription);
            }
            properties.set(parameterName, parameterNode);
        }

        ArrayNode requiredArray = schema.putArray("required");
        required.forEach(requiredArray::add);
        return schema.toPrettyString();
    }

    public static String generateFromClass(Class<?> clazz) {
        Assert.notNull(clazz, "clazz cannot be null");
        return classSchemaCache.computeIfAbsent(clazz, JsonSchemaGenerator::internalGenerateFromClass);
    }

    private static String internalGenerateFromClass(Class<?> clazz) {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .build();

        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(clazz);
        return jsonSchema.toPrettyString();
    }

    public static String generateFromType(Type type) {
        Assert.notNull(type, "type cannot be null");
        return typeSchemaCache.computeIfAbsent(type, JsonSchemaGenerator::internalGenerateFromType);
    }

    private static String internalGenerateFromType(Type type) {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .build();

        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode jsonSchema = generator.generateSchema(type);
        return jsonSchema.toPrettyString();
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

        JsonProperty propertyAnnotation = parameter.getAnnotation(JsonProperty.class);
        if (propertyAnnotation != null) {
            return propertyAnnotation.required();
        }

        Schema schemaAnnotation = parameter.getAnnotation(Schema.class);
        if (schemaAnnotation != null) {
            return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
                    || schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO
                    || schemaAnnotation.required();
        }

        Nullable nullableAnnotation = parameter.getAnnotation(Nullable.class);
        if (nullableAnnotation != null) {
            return false;
        }
        return PROPERTY_REQUIRED_BY_DEFAULT;
    }

    private static String getMethodParameterDescription(Method method, int index) {
        Parameter parameter = method.getParameters()[index];

        McpToolParam toolParamAnnotation = parameter.getAnnotation(McpToolParam.class);
        if (toolParamAnnotation != null && Utils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }

        JsonPropertyDescription jacksonAnnotation = parameter.getAnnotation(JsonPropertyDescription.class);
        if (jacksonAnnotation != null && Utils.hasText(jacksonAnnotation.value())) {
            return jacksonAnnotation.value();
        }

        Schema schemaAnnotation = parameter.getAnnotation(Schema.class);
        if (schemaAnnotation != null && Utils.hasText(schemaAnnotation.description())) {
            return schemaAnnotation.description();
        }

        return null;
    }
}