package top.yangxm.ai.mcp.commons.json;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

final class JsonMapperInternal {
    private static JsonMapper defaultJsonMapper = null;

    static JsonMapper getDefaultMapper() {
        if (defaultJsonMapper == null) {
            defaultJsonMapper = JsonMapperInternal.createDefaultMapper();
        }
        return defaultJsonMapper;
    }

    private static JsonMapper createDefaultMapper() {
        AtomicReference<IllegalStateException> ex = new AtomicReference<>();
        ServiceLoader<JsonMapperSupplier> loader = ServiceLoader.load(JsonMapperSupplier.class);
        for (JsonMapperSupplier supplier : loader) {
            try {
                if (supplier != null) {
                    try {
                        JsonMapper mapper = supplier.get();
                        if (mapper != null) {
                            return mapper;
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
        throw new IllegalStateException("No default JsonMapper implementation found");
    }

    private static void addException(AtomicReference<IllegalStateException> ref, Exception toAdd) {
        ref.updateAndGet(existing -> {
            if (existing == null) {
                return new IllegalStateException("Failed to initialize default JsonMapper", toAdd);
            } else {
                existing.addSuppressed(toAdd);
                return existing;
            }
        });
    }
}
