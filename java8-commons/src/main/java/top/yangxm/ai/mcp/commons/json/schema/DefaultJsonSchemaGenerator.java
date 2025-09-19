package top.yangxm.ai.mcp.commons.json.schema;

import top.yangxm.ai.mcp.commons.json.JsonMapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class DefaultJsonSchemaGenerator implements JsonSchemaGenerator {
    private static final JsonMapper JSON_MAPPER = JsonMapper.getDefault();

    DefaultJsonSchemaGenerator() {
    }

    @Override
    public String generateStringSchema(Class<?> clazz) {
        return JSON_MAPPER.writeValueAsString(getClassSchema(clazz, new HashSet<>()));
    }

    @Override
    public String generateStringSchema(Type type) {
        return JSON_MAPPER.writeValueAsString(getGenericSchema(type, new HashSet<>()));
    }

    @Override
    public Map<String, Object> generateMapSchema(Class<?> clazz) {
        return getClassSchema(clazz, new HashSet<>());
    }

    @Override
    public Map<String, Object> generateMapSchema(Type type) {
        return getGenericSchema(type, new HashSet<>());
    }

    private static Map<String, Object> getClassSchema(Class<?> clazz, Set<Class<?>> visited) {
        if (visited.contains(clazz)) {
            return new LinkedHashMap<String, Object>() {{
                put("type", "object");
                put("note", "circular reference to " + clazz.getSimpleName());
            }};
        }
        visited.add(clazz);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> props = new LinkedHashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            Type genericType = field.getGenericType();
            props.put(field.getName(), getTypeSchema(fieldType, genericType, visited));
        }
        schema.put("properties", props);
        return schema;
    }

    private static Map<String, Object> getTypeSchema(Class<?> type, Type genericType, Set<Class<?>> visited) {
        Map<String, Object> schema = new LinkedHashMap<>();

        if (type == String.class) {
            schema.put("type", "string");
        } else if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == short.class || type == Short.class) {
            schema.put("type", "integer");
        } else if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) {
            schema.put("type", "number");
        } else if (type == boolean.class || type == Boolean.class) {
            schema.put("type", "boolean");
        } else if (type.isArray()) {
            schema.put("type", "array");
            schema.put("items", getTypeSchema(type.getComponentType(), type.getComponentType(), visited));
        } else if (Collection.class.isAssignableFrom(type)) {
            schema.put("type", "array");
            if (genericType instanceof ParameterizedType) {
                Type elementType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                schema.put("items", getGenericSchema(elementType, visited));
            } else {
                schema.put("items", createObjectSchema());
            }
        } else if (Map.class.isAssignableFrom(type)) {
            schema.put("type", "object");
            if (genericType instanceof ParameterizedType) {
                Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
                schema.put("additionalProperties", getGenericSchema(valueType, visited));
            } else {
                schema.put("additionalProperties", createObjectSchema());
            }
        } else if (type.getPackage() != null && type.getPackage().getName().startsWith("java.")) {
            schema.put("type", "string");
        } else {
            schema = getClassSchema(type, visited);
        }
        return schema;
    }

    private static Map<String, Object> getGenericSchema(Type type, Set<Class<?>> visited) {
        if (type instanceof Class<?>) {
            return getTypeSchema((Class<?>) type, type, visited);
        } else if (type instanceof ParameterizedType) {
            return getTypeSchema((Class<?>) ((ParameterizedType) type).getRawType(), type, visited);
        } else {
            return createObjectSchema();
        }
    }

    private static Map<String, Object> createObjectSchema() {
        return new LinkedHashMap<String, Object>() {{
            put("type", "object");
        }};
    }
}
