package top.yangxm.ai.mcp.commons.logger;

import org.slf4j.Marker;

@SuppressWarnings("unused")
public final class Logger implements org.slf4j.Logger {
    private final String prefix;

    private final org.slf4j.Logger logger;

    public Logger(org.slf4j.Logger logger, Class<?> clazz) {
        final Package _package = clazz.getPackage();

        String name = _package.getName();
        if (name.isEmpty()) {
            name = "unknown";
        }

        String version = _package.getImplementationVersion();
        if (version == null || version.isEmpty()) {
            version = "dev";
        }
        prefix = String.format("[%s-%s]", name, version);
        this.logger = logger;
    }

    private String msg(String s) {
        return prefix + s;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(msg(s));
    }

    @Override
    public void trace(String s, Object o) {
        logger.trace(msg(s), o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logger.trace(msg(s), o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        logger.trace(msg(s), objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logger.trace(msg(s), throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        logger.trace(marker, msg(s));
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        logger.trace(marker, msg(s), o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        logger.trace(marker, msg(s), o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        logger.trace(marker, msg(s), objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        logger.trace(marker, msg(s), throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(msg(s));
    }

    @Override
    public void debug(String s, Object o) {
        logger.debug(msg(s), o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        logger.debug(msg(s), o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        logger.debug(msg(s), objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logger.debug(msg(s), throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        logger.debug(marker, msg(s));
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        logger.debug(marker, msg(s), o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        logger.debug(marker, msg(s), o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        logger.debug(marker, msg(s), objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        logger.debug(marker, msg(s), throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(msg(s));
    }

    @Override
    public void info(String s, Object o) {
        logger.info(msg(s), o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logger.info(msg(s), o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        logger.info(msg(s), objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        logger.info(msg(s), throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        logger.info(marker, msg(s));
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        logger.info(marker, msg(s), o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        logger.info(marker, msg(s), o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        logger.info(marker, msg(s), objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        logger.info(marker, msg(s), throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(msg(s));
    }

    @Override
    public void warn(String s, Object o) {
        logger.warn(msg(s), o);
    }

    @Override
    public void warn(String s, Object... objects) {
        logger.warn(msg(s), objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logger.warn(msg(s), o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(msg(s), throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        logger.warn(marker, msg(s));
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        logger.warn(marker, msg(s), o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        logger.warn(marker, msg(s), o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        logger.warn(marker, msg(s), objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        logger.warn(marker, msg(s), throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        logger.error(msg(s));
    }

    @Override
    public void error(String s, Object o) {
        logger.error(msg(s), o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logger.error(msg(s), o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        logger.error(msg(s), objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.error(msg(s), throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        logger.error(marker, msg(s));
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        logger.error(marker, msg(s), o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        logger.error(marker, msg(s), o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        logger.error(marker, msg(s), objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        logger.error(marker, msg(s), throwable);
    }
}
