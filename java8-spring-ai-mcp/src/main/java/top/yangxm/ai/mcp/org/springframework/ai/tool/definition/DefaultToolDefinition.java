package top.yangxm.ai.mcp.org.springframework.ai.tool.definition;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import top.yangxm.ai.mcp.org.springframework.ai.util.ParsingUtils;

@SuppressWarnings("unused")
public final class DefaultToolDefinition implements ToolDefinition {
    private final String name;
    private final String description;
    private final String inputSchema;

    public DefaultToolDefinition(String name, String description, String inputSchema) {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String inputSchema() {
        return inputSchema;
    }

    @Override
    public String toString() {
        return "DefaultToolDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", inputSchema='" + inputSchema + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        @Nullable
        private String name;
        @Nullable
        private String description;
        @Nullable
        private String inputSchema;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder inputSchema(String inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        public ToolDefinition build() {
            assert this.name != null;
            if (!StringUtils.hasText(this.description)) {
                Assert.hasText(this.name, "toolName cannot be null or empty");
                this.description = ParsingUtils.reConcatenateCamelCase(this.name, " ");
            }
            assert this.description != null;
            assert this.inputSchema != null;
            return new DefaultToolDefinition(this.name, this.description, this.inputSchema);
        }
    }
}
