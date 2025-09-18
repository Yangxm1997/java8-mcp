package top.yangxm.ai.mcp.commons.json;

import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.ClassUtils;

import java.math.BigDecimal;

@SuppressWarnings("unused")
public interface McpJsonMapper {
    <T> T readValue(String content, Class<T> type);

    <T> T readValue(byte[] content, Class<T> type);

    <T> T readValue(String content, TypeRef<T> type);

    <T> T readValue(byte[] content, TypeRef<T> type);

    <T> T convertValue(Object fromValue, Class<T> type);

    <T> T convertValue(Object fromValue, TypeRef<T> type);

    String writeValueAsString(Object value);

    byte[] writeValueAsBytes(Object value);

    boolean isValidJson(String input);

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Object toTypedObject(Object value, Class<?> type) {
        Assert.notNull(value, "value cannot be null");
        Assert.notNull(type, "type cannot be null");

        Class<?> javaType = ClassUtils.resolvePrimitiveIfNecessary(type);

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

    static McpJsonMapper getDefault() {
        return McpJsonInternal.getDefaultMapper();
    }
}
