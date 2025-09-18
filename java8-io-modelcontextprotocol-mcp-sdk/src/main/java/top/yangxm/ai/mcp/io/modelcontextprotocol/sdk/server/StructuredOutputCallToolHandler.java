package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.schema.JsonSchemaValidator;
import top.yangxm.ai.mcp.commons.json.schema.ValidationResponse;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolResult;

import java.util.Map;
import java.util.function.BiFunction;

public class StructuredOutputCallToolHandler<EX> implements BiFunction<EX, CallToolRequest, Mono<CallToolResult>> {
    private static final Logger logger = LoggerFactoryHolder.getLogger(StructuredOutputCallToolHandler.class);
    private final JsonSchemaValidator jsonSchemaValidator;
    private final Map<String, Object> outputSchema;
    private final BiFunction<EX, CallToolRequest, Mono<CallToolResult>> delegateHandler;

    private StructuredOutputCallToolHandler(JsonSchemaValidator jsonSchemaValidator,
                                            Map<String, Object> outputSchema,
                                            BiFunction<EX, CallToolRequest, Mono<CallToolResult>> delegateHandler) {
        Assert.notNull(jsonSchemaValidator, "jsonSchemaValidator must not be null");
        Assert.notNull(delegateHandler, "delegateHandler must not be null");
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.outputSchema = outputSchema;
        this.delegateHandler = delegateHandler;
    }

    @Override
    public Mono<CallToolResult> apply(EX exchange, CallToolRequest request) {
        return this.delegateHandler.apply(exchange, request).map(result -> {
            if (outputSchema == null) {
                if (result.structuredContent() != null) {
                    logger.warn("Tool call with no outputSchema is not expected to have a result with structured content, but got: {}",
                            result.structuredContent());
                }
                return result;
            }
            if (result.structuredContent() == null) {
                String message = "Response missing structured content which is expected when calling tool with non-empty outputSchema";
                logger.warn(message);
                return CallToolResult.builder()
                        .isError(true)
                        .addTextContent(message)
                        .build();
            }

            ValidationResponse validation = this.jsonSchemaValidator.validate(outputSchema, result.structuredContent());

            if (!validation.valid()) {
                logger.warn("Tool call result validation failed: {}", validation.errorMessage());
                return CallToolResult.builder()
                        .isError(true)
                        .addTextContent(validation.errorMessage())
                        .build();
            }

            if (Lists.isEmpty(result.content())) {
                return CallToolResult.builder()
                        .isError(result.isError())
                        .addTextContent(validation.jsonStructuredOutput())
                        .structuredContent(result.structuredContent())
                        .build();
            }
            return result;
        });
    }

    public static <EX> BiFunction<EX, CallToolRequest, Mono<CallToolResult>> withStructuredOutputHandling(
            JsonSchemaValidator jsonSchemaValidator,
            Map<String, Object> outputSchema,
            BiFunction<EX, CallToolRequest, Mono<CallToolResult>> delegateHandler) {
        if (delegateHandler instanceof StructuredOutputCallToolHandler) {
            return delegateHandler;
        }

        if (outputSchema == null) {
            return delegateHandler;
        }

        return new StructuredOutputCallToolHandler<>(jsonSchemaValidator, outputSchema, delegateHandler);
    }
}
