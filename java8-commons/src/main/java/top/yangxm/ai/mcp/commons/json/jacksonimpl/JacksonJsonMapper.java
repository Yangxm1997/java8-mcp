package top.yangxm.ai.mcp.commons.json.jacksonimpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.yangxm.ai.mcp.commons.json.JsonException;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;

import java.io.IOException;
import java.lang.reflect.Type;

@SuppressWarnings("unused")
public final class JacksonJsonMapper implements JsonMapper {
    private final ObjectMapper objectMapper;

    public JacksonJsonMapper(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public <T> T readValue(String content, Class<T> clazz) throws JsonException {
        notNull(content, "content");
        notNull(clazz, "clazz");
        try {
            return objectMapper.readValue(content, clazz);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert JSON from String to %s failed", clazz.getName()), e);
        }
    }

    @Override
    public <T> T readValue(byte[] content, Class<T> clazz) throws JsonException {
        notNull(content, "content");
        notNull(clazz, "clazz");
        try {
            return objectMapper.readValue(content, clazz);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert JSON from byte[] to %s failed", clazz.getName()), e);
        }
    }

    @Override
    public <T> T readValue(String content, TypeRef<T> typeRef) throws JsonException {
        notNull(content, "content");
        notNull(typeRef, "typeRef");
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(typeRef.getType());
            return objectMapper.readValue(content, javaType);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert JSON from String to %s failed", typeRef.getType().getTypeName()), e);
        }
    }

    @Override
    public <T> T readValue(byte[] content, TypeRef<T> typeRef) throws JsonException {
        notNull(content, "content");
        notNull(typeRef, "typeRef");
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(typeRef.getType());
            return objectMapper.readValue(content, javaType);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert JSON from byte[] to %s failed", typeRef.getType().getTypeName()), e);
        }
    }

    @Override
    public <T> T readValue(String content, Type type) throws JsonException {
        notNull(content, "content");
        notNull(type, "type");
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readValue(content, javaType);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert JSON from String to %s failed", type.getTypeName()), e);
        }
    }

    @Override
    public <T> T readValue(byte[] content, Type type) throws JsonException {
        notNull(content, "content");
        notNull(type, "type");
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readValue(content, javaType);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert JSON from byte[] to %s failed", type.getTypeName()), e);
        }
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> clazz) throws JsonException {
        notNull(fromValue, "fromValue");
        notNull(clazz, "clazz");
        try {
            return objectMapper.convertValue(fromValue, clazz);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert from Object to %s failed", clazz.getTypeName()), e);
        }
    }

    @Override
    public <T> T convertValue(Object fromValue, TypeRef<T> typeRef) throws JsonException {
        notNull(fromValue, "fromValue");
        notNull(typeRef, "typeRef");
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(typeRef.getType());
            return objectMapper.convertValue(fromValue, javaType);
        } catch (Exception e) {
            throw new JsonException(String.format("Convert from Object to %s failed", typeRef.getType().getTypeName()), e);
        }
    }

    @Override
    public String writeValueAsString(Object value) throws JsonException {
        notNull(value, "value");
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new JsonException("Convert from Object to JSON String failed", e);
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) throws JsonException {
        notNull(value, "value");
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new JsonException("Convert from Object to JSON byte[] failed", e);
        }
    }

    @Override
    public boolean isValidJson(String input) {
        notNull(input, "input");
        try {
            objectMapper.readTree(input);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private static void notNull(Object object, String name) throws JsonException {
        if (object == null) {
            throw new JsonException(name + " cannot be null");
        }
    }
}