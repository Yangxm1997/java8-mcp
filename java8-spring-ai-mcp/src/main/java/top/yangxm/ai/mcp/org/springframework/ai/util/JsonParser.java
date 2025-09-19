package top.yangxm.ai.mcp.org.springframework.ai.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.lang.Nullable;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.json.jacksonimpl.JacksonJsonMapper;

import java.lang.reflect.Type;

@SuppressWarnings("unused")
public final class JsonParser {
    private static final ObjectMapper OBJECT_MAPPER = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModules(JacksonUtils.instantiateAvailableModules())
            .build();
    private static final JsonMapper JSON_MAPPER = new JacksonJsonMapper(OBJECT_MAPPER);

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return JSON_MAPPER.readValue(json, type);
    }

    public static <T> T fromJson(String json, Type type) {
        return JSON_MAPPER.readValue(json, type);
    }

    public static <T> T fromJson(String json, TypeRef<T> type) {
        return JSON_MAPPER.readValue(json, type);
    }

    private static boolean isValidJson(String input) {
        return JSON_MAPPER.isValidJson(input);
    }

    public static String toJson(@Nullable Object object) {
        if (object instanceof String) {
            String str = (String) object;
            if (isValidJson(str)) {
                return str;
            }
        }
        return JSON_MAPPER.writeValueAsString(object);
    }

    public static Object toTypedObject(Object value, Class<?> type) {
        return JsonMapper.toTypedObject(value, type);
    }
}
