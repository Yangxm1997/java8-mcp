package top.yangxm.ai.mcp.commons.json;
import top.yangxm.ai.mcp.commons.util.ClassUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;

@SuppressWarnings("unused")
public interface JsonMapper {
    <T> T readValue(String content, Class<T> type) throws JsonException;

    <T> T readValue(byte[] content, Class<T> type) throws JsonException;

    <T> T readValue(String content, TypeRef<T> type) throws JsonException;

    <T> T readValue(byte[] content, TypeRef<T> type) throws JsonException;

    <T> T readValue(String content, Type type) throws JsonException;

    <T> T readValue(byte[] content, Type type) throws JsonException;

    <T> T convertValue(Object fromValue, Class<T> type) throws JsonException;

    <T> T convertValue(Object fromValue, TypeRef<T> type) throws JsonException;

    String writeValueAsString(Object value) throws JsonException;

    byte[] writeValueAsBytes(Object value) throws JsonException;

    boolean isValidJson(String input);

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Object toTypedObject(Object value, Class<?> clazz) {
        if (value == null) {
            throw new JsonException("value cannot be null");
        }

        if (clazz == null) {
            throw new JsonException("clazz cannot be null");
        }

        Class<?> javaType = ClassUtils.resolvePrimitiveIfNecessary(clazz);

        if (javaType == String.class) {
            return value.toString();
        } else if (javaType == Byte.class) {
            return Byte.parseByte(value.toString());
        } else if (javaType == Integer.class) {
            BigDecimal bigDecimal = new BigDecimal(value.toString());
            return bigDecimal.intValueExact();
        } else if (javaType == Short.class) {
            return Short.parseShort(value.toString());
        } else if (javaType == Long.class) {
            BigDecimal bigDecimal = new BigDecimal(value.toString());
            return bigDecimal.longValueExact();
        } else if (javaType == Double.class) {
            return Double.parseDouble(value.toString());
        } else if (javaType == Float.class) {
            return Float.parseFloat(value.toString());
        } else if (javaType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (javaType.isEnum()) {
            return Enum.valueOf((Class<Enum>) javaType, value.toString());
        }

        Object result = null;
        if (value instanceof String) {
            String jsonString = (String) value;
            try {
                result = getDefault().readValue(jsonString, javaType);
            } catch (Exception e) {
                // ignore
            }
        }

        if (result == null) {
            String json = getDefault().writeValueAsString(value);
            result = getDefault().readValue(json, javaType);
        }

        return result;
    }

    static JsonMapper getDefault() {
        return JsonMapperInternal.getDefaultMapper();
    }
}
