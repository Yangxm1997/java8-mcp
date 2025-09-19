package top.yangxm.ai.mcp.commons.json.schema;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

final class JsonSchemaValidatorInternal {
    private static JsonSchemaValidator defaultValidator = null;

    static JsonSchemaValidator getDefaultValidator() {
        if (defaultValidator == null) {
            defaultValidator = JsonSchemaValidatorInternal.createDefaultValidator();
        }
        return defaultValidator;
    }

    private static JsonSchemaValidator createDefaultValidator() {
        AtomicReference<IllegalStateException> ex = new AtomicReference<>();
        ServiceLoader<JsonSchemaValidatorSupplier> loader = ServiceLoader.load(JsonSchemaValidatorSupplier.class);

        for (JsonSchemaValidatorSupplier supplier : loader) {
            try {
                if (supplier != null) {
                    try {
                        JsonSchemaValidator validator = supplier.get();
                        if (validator != null) {
                            return validator;
                        }
                    } catch (Exception e) {
                        addException(ex, e);
                    }
                }
            } catch (Exception e) {
                addException(ex, e);
            }
        }

        if (ex.get() != null) {
            throw ex.get();
        }
        throw new IllegalStateException("No default McpJsonMapper implementation found");
    }

    private static void addException(AtomicReference<IllegalStateException> ref, Exception toAdd) {
        ref.updateAndGet(existing -> {
            if (existing == null) {
                return new IllegalStateException("Failed to initialize default JsonSchemaValidatorSupplier", toAdd);
            } else {
                existing.addSuppressed(toAdd);
                return existing;
            }
        });
    }
}
