package top.yangxm.ai.mcp.commons.util;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class InterfaceUtils {
    private InterfaceUtils() {
    }

    public static <I, S extends Supplier<I>, C extends I> I createDefaultInterface(
            Class<I> interfaceClass,
            Class<S> supplierClass,
            C defaultImplementation) {
        Assert.notNull(interfaceClass, "interfaceClass can not be null");
        Assert.notNull(supplierClass, "supplierClass can not be null");
        final String interfaceName = interfaceClass.getSimpleName();
        AtomicReference<IllegalStateException> ex = new AtomicReference<>();
        ServiceLoader<S> loader = ServiceLoader.load(supplierClass);
        for (S supplier : loader) {
            try {
                if (supplier != null) {
                    try {
                        I implementation = supplier.get();
                        if (implementation != null) {
                            return implementation;
                        }
                    } catch (Exception e) {
                        addException(ex, e, interfaceName);
                    }
                }
            } catch (Exception e) {
                addException(ex, e, interfaceName);
            }
        }

        if (defaultImplementation != null) {
            return defaultImplementation;
        }

        if (ex.get() != null) {
            throw ex.get();
        }
        throw new IllegalStateException("No default " + interfaceName + " implementation found");
    }

    private static void addException(AtomicReference<IllegalStateException> ref, Exception toAdd, String interfaceName) {
        ref.updateAndGet(existing -> {
            if (existing == null) {
                return new IllegalStateException("Failed to initialize default " + interfaceName, toAdd);
            } else {
                existing.addSuppressed(toAdd);
                return existing;
            }
        });
    }
}
