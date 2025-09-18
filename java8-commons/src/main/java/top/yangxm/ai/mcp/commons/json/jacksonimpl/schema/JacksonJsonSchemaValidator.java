package top.yangxm.ai.mcp.commons.json.jacksonimpl.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import top.yangxm.ai.mcp.commons.json.schema.JsonSchemaValidator;
import top.yangxm.ai.mcp.commons.json.schema.ValidationResponse;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class JacksonJsonSchemaValidator implements JsonSchemaValidator {
    private static final Logger logger = LoggerFactoryHolder.getLogger(JacksonJsonSchemaValidator.class);

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    // TODO: Implement a strategy to purge the cache (TTL, size limit, etc.)
    private final ConcurrentHashMap<String, JsonSchema> schemaCache;

    public JacksonJsonSchemaValidator() {
        this(new ObjectMapper());
    }

    public JacksonJsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schemaCache = new ConcurrentHashMap<>();
    }

    @Override
    public ValidationResponse validate(Map<String, Object> schema, Object structuredContent) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema must not be null");
        }
        if (structuredContent == null) {
            throw new IllegalArgumentException("Structured content must not be null");
        }

        try {
            JsonNode jsonStructuredOutput = this.objectMapper.valueToTree(structuredContent);
            Set<ValidationMessage> validationResult = this.getOrCreateJsonSchema(schema).validate(jsonStructuredOutput);
            if (!validationResult.isEmpty()) {
                return ValidationResponse.asInvalid(
                        "Validation failed: structuredContent does not match tool outputSchema. " + "Validation errors: " + validationResult);
            }
            return ValidationResponse.asValid(jsonStructuredOutput.toString());
        } catch (JsonProcessingException e) {
            logger.error("Failed to validate CallToolResult: Error parsing schema: {}", e.getMessage());
            return ValidationResponse.asInvalid("Error parsing tool JSON Schema: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to validate CallToolResult: Unexpected error: {}", e.getMessage());
            return ValidationResponse.asInvalid("Unexpected validation error: " + e.getMessage());
        }
    }

    private JsonSchema getOrCreateJsonSchema(Map<String, Object> schema) throws JsonProcessingException {
        String cacheKey = this.generateCacheKey(schema);
        JsonSchema cachedSchema = this.schemaCache.get(cacheKey);
        if (cachedSchema != null) {
            return cachedSchema;
        }
        JsonSchema newSchema = this.createJsonSchema(schema);
        JsonSchema existingSchema = this.schemaCache.putIfAbsent(cacheKey, newSchema);
        return existingSchema != null ? existingSchema : newSchema;
    }

    private JsonSchema createJsonSchema(Map<String, Object> schema) throws JsonProcessingException {
        JsonNode schemaNode = this.objectMapper.valueToTree(schema);
        if (schemaNode == null) {
            throw new JsonProcessingException("Failed to convert schema to JsonNode") {
            };
        }

        if (schemaNode.isObject()) {
            ObjectNode objectSchemaNode = (ObjectNode) schemaNode;
            if (!objectSchemaNode.has("additionalProperties")) {
                objectSchemaNode = objectSchemaNode.deepCopy();
                objectSchemaNode.put("additionalProperties", false);
                schemaNode = objectSchemaNode;
            }
        }
        return this.schemaFactory.getSchema(schemaNode);
    }

    private String generateCacheKey(Map<String, Object> schema) {
        if (schema.containsKey("$id")) {
            return "" + schema.get("$id");
        }
        return String.valueOf(schema.hashCode());
    }

    public void clearCache() {
        this.schemaCache.clear();
    }

    public int getCacheSize() {
        return this.schemaCache.size();
    }
}
