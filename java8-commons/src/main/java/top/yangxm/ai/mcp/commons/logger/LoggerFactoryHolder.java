package top.yangxm.ai.mcp.commons.logger;

@SuppressWarnings("unused")
public final class LoggerFactoryHolder {
    private static LoggerProvider provider = LoggerProvider.DEFAULT;

    private LoggerFactoryHolder() {
    }

    public static void setProvider(LoggerProvider loggerProvider) {
        if (loggerProvider != null) {
            provider = loggerProvider;
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return provider.getLogger(clazz);
    }
}
