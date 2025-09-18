package top.yangxm.ai.mcp.commons.json.schema;

@SuppressWarnings("unused")
public final class ValidationResponse {
    private final boolean valid;
    private final String errorMessage;
    private final String jsonStructuredOutput;

    private ValidationResponse(boolean valid, String errorMessage, String jsonStructuredOutput) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.jsonStructuredOutput = jsonStructuredOutput;
    }

    public static ValidationResponse asValid(String jsonStructuredOutput) {
        return new ValidationResponse(true, null, jsonStructuredOutput);
    }

    public static ValidationResponse asInvalid(String message) {
        return new ValidationResponse(false, message, null);
    }

    public boolean valid() {
        return valid;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public String jsonStructuredOutput() {
        return jsonStructuredOutput;
    }
}
