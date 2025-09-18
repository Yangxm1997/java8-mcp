package top.yangxm.ai.mcp.commons.json;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

final class McpJsonInternal {
    private static McpJsonMapper defaultJsonMapper = null;

    static McpJsonMapper getDefaultMapper() {
        if (defaultJsonMapper == null) {
            defaultJsonMapper = McpJsonInternal.createDefaultMapper();
        }
        return defaultJsonMapper;
    }

    private static McpJsonMapper createDefaultMapper() {
        AtomicReference<IllegalStateException> ex = new AtomicReference<>();
        ServiceLoader<McpJsonMapperSupplier> loader = ServiceLoader.load(McpJsonMapperSupplier.class);
        for (McpJsonMapperSupplier supplier : loader) {
            try {
                if (supplier != null) {
                    try {
                        McpJsonMapper mapper = supplier.get();
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
        throw new IllegalStateException("No default McpJsonMapper implementation found");
    }

    private static void addException(AtomicReference<IllegalStateException> ref, Exception toAdd) {
        ref.updateAndGet(existing -> {
            if (existing == null) {
                return new IllegalStateException("Failed to initialize default McpJsonMapper", toAdd);
            } else {
                existing.addSuppressed(toAdd);
                return existing;
            }
        });
    }
}
