package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.NoThisCapabilityException;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Prompt;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Resource;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceTemplate;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourcesUpdatedNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ServerCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Tool;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncToolSpec;

import java.util.List;
import java.util.Optional;

public interface IMcpServerAsync {
    ServerCapabilities serverCapabilities();

    Implementation serverInfo();

    default Mono<Void> addTool(AsyncToolSpec toolSpec) {
        return Mono.error(new NoThisCapabilityException("No tool capability"));
    }

    default Mono<Void> removeTool(String toolName) {
        return Mono.error(new NoThisCapabilityException("No tool capability"));
    }

    default Mono<Void> notifyToolsListChanged() {
        return Mono.error(new NoThisCapabilityException("No tool capability"));
    }

    default List<Tool> getToolList() {
        throw new NoThisCapabilityException("No tool capability");
    }

    default Optional<Tool> getTool(String toolName) {
        return this.getToolSpec(toolName).map(AsyncToolSpec::tool);
    }

    default Optional<AsyncToolSpec> getToolSpec(String toolName) {
        throw new NoThisCapabilityException("No tool capability");
    }

    default Mono<Void> addResource(AsyncResourceSpec resourceSpec) {
        return Mono.error(new NoThisCapabilityException("No resource capability"));
    }

    default Mono<Void> removeResource(String resourceUri) {
        return Mono.error(new NoThisCapabilityException("No resource capability"));
    }

    default Mono<Void> notifyResourcesListChanged() {
        return Mono.error(new NoThisCapabilityException("No resource capability"));
    }

    default Mono<Void> notifyResourcesUpdated(ResourcesUpdatedNotification resourcesUpdatedNotification) {
        return Mono.error(new NoThisCapabilityException("No resource capability"));
    }

    default List<Resource> getResourceList() {
        throw new NoThisCapabilityException("No resource capability");
    }

    default List<ResourceTemplate> getResourceTemplateList() {
        throw new NoThisCapabilityException("No resource capability");
    }

    default Optional<Resource> getResource(String resourceUri) {
        return this.getResourceSpec(resourceUri).map(AsyncResourceSpec::resource);
    }

    default Optional<AsyncResourceSpec> getResourceSpec(String resourceUri) {
        throw new NoThisCapabilityException("No resource capability");
    }

    default Mono<Void> addPrompt(AsyncPromptSpec promptSpec) {
        return Mono.error(new NoThisCapabilityException("No prompt capability"));
    }

    default Mono<Void> removePrompt(String promptName) {
        return Mono.error(new NoThisCapabilityException("No prompt capability"));
    }

    default Mono<Void> notifyPromptsListChanged() {
        return Mono.error(new NoThisCapabilityException("No prompt capability"));
    }

    default List<Prompt> getPromptList() {
        throw new NoThisCapabilityException("No prompt capability");
    }

    default Optional<Prompt> getPrompt(String promptName) {
        return this.getPromptSpec(promptName).map(AsyncPromptSpec::prompt);
    }

    default Optional<AsyncPromptSpec> getPromptSpec(String promptName) {
        throw new NoThisCapabilityException("No prompt capability");
    }

    Mono<Void> closeGracefully();

    void close();
}
