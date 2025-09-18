package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import io.swagger.v3.oas.annotations.media.Schema;
import top.yangxm.ai.mcp.commons.util.Utils;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpToolParam;

import java.util.stream.Stream;

public final class SpringAiSchemaModule implements Module {
    private final boolean requiredByDefault;

    public SpringAiSchemaModule(Option... options) {
        this.requiredByDefault = Stream.of(options)
                .noneMatch(option -> option == Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        this.applyToConfigBuilder(builder.forFields());
    }

    private void applyToConfigBuilder(SchemaGeneratorConfigPart<FieldScope> configPart) {
        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withRequiredCheck(this::checkRequired);
    }

    /**
     * 从 {@code @ToolParam(description = ...)} 提取 description
     */
    private String resolveDescription(MemberScope<?, ?> member) {
        McpToolParam toolParamAnnotation = member.getAnnotationConsideringFieldAndGetter(McpToolParam.class);
        if (toolParamAnnotation != null && Utils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }
        return null;
    }

    /**
     * 从以下注解中判断该参数是的required
     * <ul>
     * <li>{@code @ToolParam(required = ...)}</li>
     * <li>{@code @JsonProperty(required = ...)}</li>
     * <li>{@code @Schema(required = ...)}</li>
     * <li>{@code @Nullable}</li>
     * </ul>
     */
    private boolean checkRequired(MemberScope<?, ?> member) {
        McpToolParam toolParamAnnotation = member.getAnnotationConsideringFieldAndGetter(McpToolParam.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        }

        JsonProperty propertyAnnotation = member.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
        if (propertyAnnotation != null) {
            return propertyAnnotation.required();
        }

        Schema schemaAnnotation = member.getAnnotationConsideringFieldAndGetter(Schema.class);
        if (schemaAnnotation != null) {
            return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
                    || schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO
                    || schemaAnnotation.required();
        }

        return this.requiredByDefault;
    }


    public enum Option {
        PROPERTY_REQUIRED_FALSE_BY_DEFAULT
    }
}
