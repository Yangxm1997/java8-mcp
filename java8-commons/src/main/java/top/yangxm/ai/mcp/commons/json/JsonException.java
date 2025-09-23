package top.yangxm.ai.mcp.commons.json;

@SuppressWarnings("unused")
public class JsonException extends RuntimeException {
    public JsonException(String message) {
        super(message);
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
