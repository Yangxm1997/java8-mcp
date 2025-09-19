package top.yangxm.ai.mcp.commons.json.schema;

import java.lang.reflect.Type;
import java.util.Map;

public interface JsonSchemaGenerator {
    String generateStringSchema(Class<?> clazz);

    String generateStringSchema(Type type);

    Map<String, Object> generateMapSchema(Class<?> clazz);

    Map<String, Object> generateMapSchema(Type type);

    static JsonSchemaGenerator getDefault() {
        return JsonSchemaGeneratorInternal.getDefaultGenerator();
    }
}
