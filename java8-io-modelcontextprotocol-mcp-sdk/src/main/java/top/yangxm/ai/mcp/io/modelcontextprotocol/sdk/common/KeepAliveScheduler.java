package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class KeepAliveScheduler {
    private static final Logger logger = LoggerFactoryHolder.getLogger(KeepAliveScheduler.class);
    private static final TypeRef<Object> OBJECT_TYPE_REF = new TypeRef<Object>() {
    };
    private final Duration initialDelay;
    private final Duration interval;
    private final Scheduler scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Disposable currentSubscription;
    // TODO Currently we do not support the streams
    private final Supplier<Flux<McpSession>> mcpSessions;

    KeepAliveScheduler(Scheduler scheduler, Duration initialDelay, Duration interval, Supplier<Flux<McpSession>> mcpSessions) {
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.mcpSessions = mcpSessions;
    }

    public Disposable start() {
        if (this.isRunning.compareAndSet(false, true)) {
            this.currentSubscription = Flux.interval(this.initialDelay, this.interval, this.scheduler)
                    .doOnNext(tick -> this.mcpSessions.get()
                            .flatMap(session -> session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF)
                                    .doOnError(e -> logger.warn("Failed to send keep-alive ping to session {}: {}", session,
                                            e.getMessage()))
                                    .onErrorComplete())
                            .subscribe())
                    .doOnCancel(() -> this.isRunning.set(false))
                    .doOnComplete(() -> this.isRunning.set(false))
                    .onErrorComplete(error -> {
                        logger.error("KeepAlive scheduler error", error);
                        this.isRunning.set(false);
                        return true;
                    })
                    .subscribe();
            return this.currentSubscription;
        } else {
            throw new IllegalStateException("KeepAlive scheduler is already running. Stop it first.");
        }
    }

    public void stop() {
        if (this.currentSubscription != null && !this.currentSubscription.isDisposed()) {
            this.currentSubscription.dispose();
        }
        this.isRunning.set(false);
    }

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public void shutdown() {
        stop();
        if (this.scheduler != null) {
            ((Disposable) this.scheduler).dispose();
        }
    }

    public static Builder builder(Supplier<Flux<McpSession>> mcpSessions) {
        return new Builder(mcpSessions);
    }

    public static class Builder {
        private Scheduler scheduler = Schedulers.boundedElastic();
        private Duration initialDelay = Duration.ofSeconds(0);
        private Duration interval = Duration.ofSeconds(30);
        private final Supplier<Flux<McpSession>> mcpSessions;

        Builder(Supplier<Flux<McpSession>> mcpSessions) {
            Assert.notNull(mcpSessions, "McpSessions supplier must not be null");
            this.mcpSessions = mcpSessions;
        }

        public Builder scheduler(Scheduler scheduler) {
            Assert.notNull(scheduler, "Scheduler must not be null");
            this.scheduler = scheduler;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            Assert.notNull(initialDelay, "Initial delay must not be null");
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder interval(Duration interval) {
            Assert.notNull(interval, "Interval must not be null");
            this.interval = interval;
            return this;
        }

        public KeepAliveScheduler build() {
            return new KeepAliveScheduler(scheduler, initialDelay, interval, mcpSessions);
        }

    }
}
