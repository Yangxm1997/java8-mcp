package top.yangxm.ai.mcp.commons.json.jacksonimpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.util.Assert;

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
    public <T> T readValue(String content, Class<T> type) {
        Assert.notNull(content, "content cannot be null");
        Assert.notNull(type, "type cannot be null");
        try {
            return objectMapper.readValue(content, type);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Convert JSON from String to %s failed", type.getName()), e);
        }
    }

    @Override
    public <T> T readValue(byte[] content, Class<T> type) {
        Assert.notNull(content, "content cannot be null");
        Assert.notNull(type, "type cannot be null");
        try {
            return objectMapper.readValue(content, type);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Convert JSON from byte[] to %s failed", type.getName()), e);
        }
    }

    @Override
    public <T> T readValue(String content, TypeRef<T> type) {
        Assert.notNull(content, "content cannot be null");
        Assert.notNull(type, "type cannot be null");
        Type _type = type.getType();
        JavaType javaType = objectMapper.getTypeFactory().constructType(_type);
        try {
            return objectMapper.readValue(content, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Convert JSON from String to %s failed", _type.getTypeName()), e);
        }
    }

    @Override
    public <T> T readValue(byte[] content, TypeRef<T> type) {
        Assert.notNull(content, "content cannot be null");
        Assert.notNull(type, "type cannot be null");
        Type _type = type.getType();
        JavaType javaType = objectMapper.getTypeFactory().constructType(_type);
        try {
            return objectMapper.readValue(content, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Convert JSON from byte[] to %s failed", _type.getTypeName()), e);
        }
    }

    @Override
    public <T> T readValue(String content, Type type) {
        Assert.notNull(content, "content cannot be null");
        Assert.notNull(type, "type cannot be null");
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        try {
            return objectMapper.readValue(content, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Convert JSON from String to %s failed", type.getTypeName()), e);
        }
    }

    @Override
    public <T> T readValue(byte[] content, Type type) {
        Assert.notNull(content, "content cannot be null");
        Assert.notNull(type, "type cannot be null");
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        try {
            return objectMapper.readValue(content, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Convert JSON from byte[] to %s failed", type.getTypeName()), e);
        }
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> type) {
        Assert.notNull(fromValue, "fromValue cannot be null");
        Assert.notNull(type, "type cannot be null");
        return objectMapper.convertValue(fromValue, type);
    }

    @Override
    public <T> T convertValue(Object fromValue, TypeRef<T> type) {
        Assert.notNull(fromValue, "fromValue cannot be null");
        Assert.notNull(type, "type cannot be null");
        JavaType javaType = objectMapper.getTypeFactory().constructType(type.getType());
        return objectMapper.convertValue(fromValue, javaType);
    }

    @Override
    public String writeValueAsString(Object value) {
        Assert.notNull(value, "value cannot be null");
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Convert from Object to JSON String failed", e);
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) {
        Assert.notNull(value, "value cannot be null");
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new IllegalStateException("Convert from Object to JSON byte[] failed", e);
        }
    }

    @Override
    public boolean isValidJson(String input) {
        Assert.notNull(input, "input cannot be null");
        try {
            objectMapper.readTree(input);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
