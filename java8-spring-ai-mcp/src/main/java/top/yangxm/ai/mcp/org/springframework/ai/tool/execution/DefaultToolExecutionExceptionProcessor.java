package top.yangxm.ai.mcp.org.springframework.ai.tool.execution;

import org.springframework.util.Assert;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class DefaultToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {
    private static final Logger logger = LoggerFactoryHolder.getLogger(DefaultToolExecutionExceptionProcessor.class);
    private static final boolean DEFAULT_ALWAYS_THROW = false;

    private final boolean alwaysThrow;
    private final List<Class<? extends RuntimeException>> rethrownExceptions;

    public DefaultToolExecutionExceptionProcessor(boolean alwaysThrow) {
        this(alwaysThrow, Collections.emptyList());
    }

    public DefaultToolExecutionExceptionProcessor(boolean alwaysThrow, List<Class<? extends RuntimeException>> rethrownExceptions) {
        this.alwaysThrow = alwaysThrow;
        this.rethrownExceptions = Collections.unmodifiableList(rethrownExceptions);
    }

    @Override
    public String process(ToolExecutionException exception) {
        Assert.notNull(exception, "exception cannot be null");
        Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException) {
            RuntimeException runtimeException = (RuntimeException) cause;
            if (this.rethrownExceptions.stream().anyMatch(rethrown -> rethrown.isAssignableFrom(cause.getClass()))) {
                throw runtimeException;
            }
        } else {
            throw exception;
        }

        if (this.alwaysThrow) {
            throw exception;
        }
        logger.debug("Exception thrown by tool: {}. Message: {}", exception.getToolDefinition().name(), exception.getMessage());
        return exception.getMessage();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean alwaysThrow = DEFAULT_ALWAYS_THROW;
        private List<Class<? extends RuntimeException>> exceptions = Collections.emptyList();

        public Builder alwaysThrow(boolean alwaysThrow) {
            this.alwaysThrow = alwaysThrow;
            return this;
        }

        public Builder rethrowExceptions(List<Class<? extends RuntimeException>> exceptions) {
            this.exceptions = exceptions;
            return this;
        }

        public DefaultToolExecutionExceptionProcessor build() {
            return new DefaultToolExecutionExceptionProcessor(this.alwaysThrow, this.exceptions);
        }
    }
}
