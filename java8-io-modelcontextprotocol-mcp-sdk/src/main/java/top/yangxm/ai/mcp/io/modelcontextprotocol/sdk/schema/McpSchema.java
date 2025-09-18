package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import top.yangxm.ai.mcp.commons.json.McpJsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public final class McpSchema {
    private McpSchema() {
        // do nothing
    }

    public static final Logger logger = LoggerFactoryHolder.getLogger(McpSchema.class);
    public static final String JSONRPC_VERSION = "2.0";
    private static final TypeRef<HashMap<String, Object>> MAP_TYPE_REF = new TypeRef<HashMap<String, Object>>() {
    };

    public static final String FIRST_PAGE = null;

    // Lifecycle Methods
    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";
    public static final String METHOD_PING = "ping";
    public static final String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";

    // Tool Methods
    public static final String METHOD_TOOLS_LIST = "tools/list";
    public static final String METHOD_TOOLS_CALL = "tools/call";
    public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

    // Resources Methods
    public static final String METHOD_RESOURCES_LIST = "resources/list";
    public static final String METHOD_RESOURCES_READ = "resources/read";
    public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
    public static final String METHOD_NOTIFICATION_RESOURCES_UPDATED = "notifications/resources/updated";
    public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";
    public static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";
    public static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

    // Prompt Methods
    public static final String METHOD_PROMPT_LIST = "prompts/list";
    public static final String METHOD_PROMPT_GET = "prompts/get";
    public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";
    public static final String METHOD_COMPLETION_COMPLETE = "completion/complete";

    // Logging Methods
    public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";
    public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

    // Roots Methods
    public static final String METHOD_ROOTS_LIST = "roots/list";
    public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

    // Sampling Methods
    public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

    // Elicitation Methods
    public static final String METHOD_ELICITATION_CREATE = "elicitation/create";

    public static JSONRPCMessage deserializeJsonRpcMessage(McpJsonMapper jsonMapper, String jsonText) {

        logger.debug("Received JSON message: {}", jsonText);
        HashMap<String, Object> map = jsonMapper.readValue(jsonText, MAP_TYPE_REF);
        if (map.containsKey("method") && map.containsKey("id")) {
            return jsonMapper.convertValue(jsonText, JSONRPCRequest.class);
        } else if (map.containsKey("method") && !map.containsKey("id")) {
            return jsonMapper.convertValue(jsonText, JSONRPCNotification.class);
        } else if (map.containsKey("result") || map.containsKey("error")) {
            return jsonMapper.convertValue(jsonText, JSONRPCResponse.class);
        }
        throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
    }


    private static Map<String, Object> schemaToMap(McpJsonMapper jsonMapper, String schema) {
        try {
            return jsonMapper.readValue(schema, MAP_TYPE_REF);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid schema: " + schema, e);
        }
    }

    private static JsonSchema parseSchema(McpJsonMapper jsonMapper, String schema) {
        try {
            return jsonMapper.readValue(schema, JsonSchema.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid schema: " + schema, e);
        }
    }

    public interface Request extends Serializable {
        String PROGRESS_TOKEN = "progressToken";

        default Map<String, Object> getMeta() {
            return meta();
        }

        Map<String, Object> meta();

        default String progressToken() {
            Map<String, Object> meta = meta();
            if (meta != null && meta.containsKey(PROGRESS_TOKEN)) {
                return meta.get(PROGRESS_TOKEN).toString();
            }
            return null;
        }
    }

    public interface Result extends Serializable {
        default Map<String, Object> getMeta() {
            return meta();
        }

        Map<String, Object> meta();
    }

    public interface Notification extends Serializable {
        default Map<String, Object> getMeta() {
            return meta();
        }

        Map<String, Object> meta();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(value = TextContent.class, name = Content.TYPE_TEXT),
            @JsonSubTypes.Type(value = ImageContent.class, name = Content.TYPE_IMAGE),
            @JsonSubTypes.Type(value = AudioContent.class, name = Content.TYPE_AUDIO),
            @JsonSubTypes.Type(value = EmbeddedResource.class, name = Content.TYPE_RESOURCE),
            @JsonSubTypes.Type(value = ResourceLink.class, name = Content.TYPE_RESOURCE_LINK)})
    public interface Content extends Serializable {
        String TYPE_TEXT = "text";
        String TYPE_IMAGE = "image";
        String TYPE_AUDIO = "audio";
        String TYPE_RESOURCE = "resource";
        String TYPE_RESOURCE_LINK = "resource_link";

        default Map<String, Object> getMeta() {
            return meta();
        }

        default String getType() {
            return type();
        }

        Map<String, Object> meta();

        default String type() {
            if (this instanceof TextContent) {
                return TYPE_TEXT;
            } else if (this instanceof ImageContent) {
                return TYPE_IMAGE;
            } else if (this instanceof AudioContent) {
                return TYPE_AUDIO;
            } else if (this instanceof EmbeddedResource) {
                return TYPE_RESOURCE;
            } else if (this instanceof ResourceLink) {
                return TYPE_RESOURCE_LINK;
            }
            throw new IllegalArgumentException("Unknown content type: " + this);
        }
    }

    public interface BaseMetadata extends Serializable {
        default String getName() {
            return name();
        }

        default String getTitle() {
            return title();
        }

        String name();

        String title();
    }

    public interface ResourceContent extends BaseMetadata {
        default String getUri() {
            return uri();
        }

        default String getDescription() {
            return description();
        }

        default String getMimeType() {
            return mimeType();
        }

        default Long getSize() {
            return size();
        }

        default Annotations getAnnotations() {
            return annotations();
        }

        String uri();

        String description();

        String mimeType();

        Long size();

        Annotations annotations();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({@JsonSubTypes.Type(value = TextResourceContents.class, name = ResourceContents.TYPE_TEXT),
            @JsonSubTypes.Type(value = BlobResourceContents.class, name = ResourceContents.TYPE_BLOB)})
    public interface ResourceContents extends Serializable {
        String TYPE_TEXT = "text";
        String TYPE_BLOB = "blob";

        default String getUri() {
            return uri();
        }

        default String getMimeType() {
            return mimeType();
        }

        default Map<String, Object> getMeta() {
            return meta();
        }

        String uri();

        String mimeType();

        Map<String, Object> meta();
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource implements Annotated, ResourceContent {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("name")
        private String name;

        @Nullable
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("mimeType")
        private String mimeType;

        @Nullable
        @JsonProperty("size")
        private Long size;

        @JsonProperty("annotations")
        private Annotations annotations;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        Resource() {
        }

        public Resource(String uri, String name, String title,
                        String description, String mimeType, Long size,
                        Annotations annotations, Map<String, Object> meta) {
            this.uri = uri;
            this.name = name;
            this.title = title;
            this.description = description;
            this.mimeType = mimeType;
            this.size = size;
            this.annotations = annotations;
            this.meta = meta;
        }

        @Override
        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        @Override
        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        @Override
        public String description() {
            return description;
        }

        public void description(String description) {
            this.description = description;
        }

        @Override
        public String mimeType() {
            return mimeType;
        }

        public void mimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public Long size() {
            return size;
        }

        public void size(Long size) {
            this.size = size;
        }

        @Override
        public Annotations annotations() {
            return annotations;
        }

        public void annotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setName(String name) {
            this.name(name);
        }

        public void setTitle(String title) {
            this.title(title);
        }


        public void setDescription(String description) {
            this.description(description);
        }

        public void setMimeType(String mimeType) {
            this.mimeType(mimeType);
        }

        public void setSize(Long size) {
            this.size(size);
        }

        @Override
        public Annotations getAnnotations() {
            return this.annotations();
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations(annotations);
        }

        public Map<String, Object> getMeta() {
            return this.meta();
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "Resource{" +
                    "uri='" + uri + '\'' +
                    ", name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", size=" + size +
                    ", annotations=" + annotations +
                    ", meta=" + meta +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String uri;
            private String name;
            private String title;
            private String description;
            private String mimeType;
            private Long size;
            private Annotations annotations;
            private Map<String, Object> meta;

            public Builder uri(String uri) {
                this.uri = uri;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder mimeType(String mimeType) {
                this.mimeType = mimeType;
                return this;
            }

            public Builder size(Long size) {
                this.size = size;
                return this;
            }

            public Builder annotations(Annotations annotations) {
                this.annotations = annotations;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Resource build() {
                Assert.hasText(uri, "uri must not be empty");
                Assert.hasText(name, "name must not be empty");
                return new Resource(uri, name, title, description, mimeType, size, annotations, meta);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceTemplate implements Annotated, BaseMetadata {
        @JsonProperty("uriTemplate")
        private String uriTemplate;

        @JsonProperty("name")
        private String name;

        @Nullable
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("mimeType")
        private String mimeType;

        @JsonProperty("annotations")
        private Annotations annotations;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ResourceTemplate() {
        }

        public ResourceTemplate(String uriTemplate, String name, String description, String mimeType, Annotations annotations) {
            this(uriTemplate, name, null, description, mimeType, annotations, null);
        }

        public ResourceTemplate(String uriTemplate, String name, String title, String description, String mimeType, Annotations annotations) {
            this(uriTemplate, name, title, description, mimeType, annotations, null);
        }

        public ResourceTemplate(String uriTemplate, String name, String title, String description, String mimeType, Annotations annotations, Map<String, Object> meta) {
            this.uriTemplate = uriTemplate;
            this.name = name;
            this.title = title;
            this.description = description;
            this.mimeType = mimeType;
            this.annotations = annotations;
            this.meta = meta;
        }

        public String uriTemplate() {
            return uriTemplate;
        }

        public void uriTemplate(String uriTemplate) {
            this.uriTemplate = uriTemplate;
        }

        @Override
        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        @Override
        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public String description() {
            return description;
        }

        public void description(String description) {
            this.description = description;
        }

        public String mimeType() {
            return mimeType;
        }

        public void mimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public Annotations annotations() {
            return annotations;
        }

        public void annotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getUriTemplate() {
            return this.uriTemplate();
        }

        public void setUriTemplate(String uriTemplate) {
            this.uriTemplate(uriTemplate);
        }

        public void setName(String name) {
            this.name(name);
        }

        public void setTitle(String title) {
            this.title(title);
        }

        public String getDescription() {
            return this.description();
        }

        public void setDescription(String description) {
            this.description(description);
        }

        public String getMimeType() {
            return this.mimeType();
        }

        public void setMimeType(String mimeType) {
            this.mimeType(mimeType);
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations(annotations);
        }

        public Map<String, Object> getMeta() {
            return this.meta();
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ResourceTemplate{" +
                    "uriTemplate='" + uriTemplate + '\'' +
                    ", name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", annotations=" + annotations +
                    ", meta=" + meta +
                    '}';
        }
    }

    public interface CompleteReference extends Serializable {
        String REF_PROMPT = "ref/prompt";
        String REF_RESOURCE = "ref/resource";

        default String getType() {
            return type();
        }

        default String getIdentifier() {
            return identifier();
        }

        String type();

        String identifier();
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptReference implements CompleteReference, BaseMetadata {
        @JsonProperty("type")
        private String type;

        @JsonProperty("name")
        private String name;

        @Nullable
        @JsonProperty("title")
        private String title;

        PromptReference() {
        }

        public PromptReference(String type, String name) {
            this(type, name, null);
        }

        public PromptReference(String name) {
            this(REF_PROMPT, name, null);
        }

        public PromptReference(String type, String name, String title) {
            this.type = type;
            this.name = name;
            this.title = title;
        }

        @Override
        public String identifier() {
            return name;
        }

        @Override
        public String type() {
            return type;
        }

        public void type(String type) {
            this.type = type;
        }

        @Override
        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        @Override
        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public void setType(String type) {
            this.type(type);
        }

        public void setName(String name) {
            this.name(name);
        }

        public void setTitle(String title) {
            this.title(title);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            PromptReference that = (PromptReference) obj;
            return Objects.equals(identifier(), that.identifier()) && Objects.equals(type(), that.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier(), type());
        }

        @Override
        public String toString() {
            return "PromptReference{" +
                    "type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceReference implements CompleteReference {
        @JsonProperty("type")
        private String type;

        @JsonProperty("uri")
        private String uri;

        ResourceReference() {
        }

        public ResourceReference(String uri) {
            this(REF_RESOURCE, uri);
        }

        public ResourceReference(String type, String uri) {
            this.type = type;
            this.uri = uri;
        }

        @Override
        public String identifier() {
            return uri;
        }

        @Override
        public String type() {
            return type;
        }

        public void type(String type) {
            this.type = type;
        }

        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        public void setType(String type) {
            this.type(type);
        }

        public String getUri() {
            return this.uri();
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ResourceReference that = (ResourceReference) obj;
            return Objects.equals(identifier(), that.identifier()) && Objects.equals(type(), that.type());
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier(), type());
        }

        @Override
        public String toString() {
            return "ResourceReference{" +
                    "type='" + type + '\'' +
                    ", uri='" + uri + '\'' +
                    '}';
        }
    }

    public enum Role implements Serializable {
        @JsonProperty("user")
        USER,

        @JsonProperty("assistant")
        ASSISTANT
    }

    public enum LoggingLevel implements Serializable {
        @JsonProperty("debug")
        DEBUG(0),

        @JsonProperty("info")
        INFO(1),

        @JsonProperty("notice")
        NOTICE(2),

        @JsonProperty("warning")
        WARNING(3),

        @JsonProperty("error")
        ERROR(4),

        @JsonProperty("critical")
        CRITICAL(5),

        @JsonProperty("alert")
        ALERT(6),

        @JsonProperty("emergency")
        EMERGENCY(7);

        private final int level;

        LoggingLevel(int level) {
            this.level = level;
        }

        public int level() {
            return level;
        }

        public int getLevel() {
            return this.level();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Root implements Serializable {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("name")
        private String name;

        @JsonProperty("hasMore")
        private Boolean hasMore;

        Root() {
        }

        public Root(String uri, String name, Boolean hasMore) {
            this.uri = uri;
            this.name = name;
            this.hasMore = hasMore;
        }

        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        public Boolean hasMore() {
            return hasMore;
        }

        public void hasMore(Boolean hasMore) {
            this.hasMore = hasMore;
        }

        public String getUri() {
            return this.uri();
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public String getName() {
            return this.name();
        }

        public void setName(String name) {
            this.name(name);
        }

        public Boolean getHasMore() {
            return this.hasMore();
        }

        public void setHasMore(Boolean hasMore) {
            this.hasMore(hasMore);
        }

        @Override
        public String toString() {
            return "Root{" +
                    "uri='" + uri + '\'' +
                    ", name='" + name + '\'' +
                    ", hasMore=" + hasMore +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Annotations implements Serializable {
        @JsonProperty("audience")
        private List<Role> audience;

        @JsonProperty("priority")
        private Double priority;

        Annotations() {
        }

        public Annotations(List<Role> audience, Double priority) {
            this.audience = audience;
            this.priority = priority;
        }

        public List<Role> audience() {
            return audience;
        }

        public void audience(List<Role> audience) {
            this.audience = audience;
        }

        public Double priority() {
            return priority;
        }

        public void priority(Double priority) {
            this.priority = priority;
        }

        public List<Role> getAudience() {
            return this.audience();
        }

        public void setAudience(List<Role> audience) {
            this.audience(audience);
        }

        public Double getPriority() {
            return this.priority();
        }

        public void setPriority(Double priority) {
            this.priority(priority);
        }

        @Override
        public String toString() {
            return "Annotations{" +
                    "audience=" + audience +
                    ", priority=" + priority +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SamplingMessage implements Serializable {
        @JsonProperty("role")
        private Role role;

        @JsonProperty("content")
        private Content content;

        SamplingMessage() {
        }

        public SamplingMessage(Role role, Content content) {
            this.role = role;
            this.content = content;
        }

        public Role role() {
            return role;
        }

        public void role(Role role) {
            this.role = role;
        }

        public Content content() {
            return content;
        }

        public void content(Content content) {
            this.content = content;
        }

        public Role getRole() {
            return this.role();
        }

        public void setRole(Role role) {
            this.role(role);
        }

        public Content getContent() {
            return this.content();
        }

        public void setContent(Content content) {
            this.content(content);
        }

        @Override
        public String toString() {
            return "SamplingMessage{" +
                    "role=" + role +
                    ", content=" + content +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelHint implements Serializable {
        @JsonProperty("name")
        private String name;

        ModelHint() {
        }

        public ModelHint(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name();
        }

        public void setName(String name) {
            this.name(name);
        }

        @Override
        public String toString() {
            return "ModelHint{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelPreferences implements Serializable {
        @JsonProperty("hints")
        private List<ModelHint> hints;

        @JsonProperty("costPriority")
        private Double costPriority;

        @JsonProperty("speedPriority")
        private Double speedPriority;

        @JsonProperty("intelligencePriority")
        private Double intelligencePriority;

        ModelPreferences() {
        }


        public ModelPreferences(List<ModelHint> hints, Double costPriority, Double speedPriority, Double intelligencePriority) {
            this.hints = hints;
            this.costPriority = costPriority;
            this.speedPriority = speedPriority;
            this.intelligencePriority = intelligencePriority;
        }

        public List<ModelHint> hints() {
            return hints;
        }

        public void hints(List<ModelHint> hints) {
            this.hints = hints;
        }

        public Double costPriority() {
            return costPriority;
        }

        public void costPriority(Double costPriority) {
            this.costPriority = costPriority;
        }

        public Double speedPriority() {
            return speedPriority;
        }

        public void speedPriority(Double speedPriority) {
            this.speedPriority = speedPriority;
        }

        public Double intelligencePriority() {
            return intelligencePriority;
        }

        public void intelligencePriority(Double intelligencePriority) {
            this.intelligencePriority = intelligencePriority;
        }

        public List<ModelHint> getHints() {
            return this.hints();
        }

        public void setHints(List<ModelHint> hints) {
            this.hints(hints);
        }

        public Double getCostPriority() {
            return this.costPriority();
        }

        public void setCostPriority(Double costPriority) {
            this.costPriority(costPriority);
        }

        public Double getSpeedPriority() {
            return this.speedPriority();
        }

        public void setSpeedPriority(Double speedPriority) {
            this.speedPriority(speedPriority);
        }

        public Double getIntelligencePriority() {
            return this.intelligencePriority();
        }

        public void setIntelligencePriority(Double intelligencePriority) {
            this.intelligencePriority(intelligencePriority);
        }

        @Override
        public String toString() {
            return "ModelPreferences{" +
                    "hints=" + hints +
                    ", costPriority=" + costPriority +
                    ", speedPriority=" + speedPriority +
                    ", intelligencePriority=" + intelligencePriority +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<ModelHint> hints;
            private Double costPriority;
            private Double speedPriority;
            private Double intelligencePriority;

            public Builder hints(List<ModelHint> hints) {
                this.hints = hints;
                return this;
            }

            public Builder addHint(String name) {
                if (this.hints == null) {
                    this.hints = new ArrayList<>();
                }
                this.hints.add(new ModelHint(name));
                return this;
            }

            public Builder costPriority(Double costPriority) {
                this.costPriority = costPriority;
                return this;
            }

            public Builder speedPriority(Double speedPriority) {
                this.speedPriority = speedPriority;
                return this;
            }

            public Builder intelligencePriority(Double intelligencePriority) {
                this.intelligencePriority = intelligencePriority;
                return this;
            }

            public ModelPreferences build() {
                return new ModelPreferences(hints, costPriority, speedPriority, intelligencePriority);
            }
        }
    }

    public interface Annotated extends Serializable {
        default Annotations getAnnotations() {
            return annotations();
        }

        Annotations annotations();
    }

    public interface JSONRPCMessage extends Serializable {
        String jsonrpc();

        default String getJsonrpc() {
            return jsonrpc();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCRequest implements JSONRPCMessage {
        @JsonProperty("jsonrpc")
        private String jsonrpc;

        @JsonProperty("method")
        private String method;

        @JsonProperty("id")
        private Object id;

        @Nullable
        @JsonProperty("params")
        private Object params;

        JSONRPCRequest() {
        }

        public JSONRPCRequest(String jsonrpc, String method, Object id, Object params) {
            this.jsonrpc = jsonrpc;
            this.method = method;
            this.id(id);
            this.params = params;
        }

        public static JSONRPCRequest of(String method, Object id, Object params) {
            return new JSONRPCRequest(JSONRPC_VERSION, method, id, params);
        }

        @Override
        public String jsonrpc() {
            return jsonrpc;
        }

        public void jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }

        public String method() {
            return method;
        }

        public void method(String method) {
            this.method = method;
        }

        public Object id() {
            return id;
        }

        public void id(Object id) {
            Assert.notNull(id, "MCP requests MUST include an ID - null IDs are not allowed");
            Assert.isTrue(id instanceof String || id instanceof Integer || id instanceof Long,
                    "MCP requests MUST have an ID that is either a string or integer");
            this.id = id;
        }

        public Object params() {
            return params;
        }

        public void params(Object params) {
            this.params = params;
        }

        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc(jsonrpc);
        }

        public String getMethod() {
            return this.method();
        }

        public void setMethod(String method) {
            this.method(method);
        }

        public Object getId() {
            return this.id();
        }

        public void setId(Object id) {
            this.id(id);
        }

        public Object getParams() {
            return this.params();
        }

        public void setParams(Object params) {
            this.params(params);
        }

        @Override
        public String toString() {
            return "JSONRPCRequest{" +
                    "jsonrpc='" + jsonrpc + '\'' +
                    ", method='" + method + '\'' +
                    ", id=" + id +
                    ", params=" + params +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCResponse implements JSONRPCMessage {
        @JsonProperty("jsonrpc")
        private String jsonrpc;

        @JsonProperty("id")
        private Object id;

        @Nullable
        @JsonProperty("result")
        private Object result;

        @Nullable
        @JsonProperty("error")
        private JSONRPCError error;

        JSONRPCResponse() {
        }

        public JSONRPCResponse(String jsonrpc, Object id, Object result, JSONRPCError error) {
            this.jsonrpc = jsonrpc;
            this.id = id;
            this.result = result;
            this.error = error;
        }

        public static JSONRPCResponse ofSuccess(Object id, Object result) {
            return new JSONRPCResponse(JSONRPC_VERSION, id, result, null);
        }

        public static JSONRPCResponse ofError(Object id, int code, String message) {
            return ofError(id, code, message, null);
        }

        public static JSONRPCResponse ofError(Object id, int code, String message, Object result) {
            Assert.hasText(message, "message must not be empty");
            JSONRPCError error = new JSONRPCError(code, message);
            return new JSONRPCResponse(JSONRPC_VERSION, id, result, error);
        }

        public static JSONRPCResponse ofMethodNotFoundError(Object id, String method) {
            Assert.hasText(method, "method must not be empty");
            return ofError(id, ErrorCodes.METHOD_NOT_FOUND, ErrorCodes.getMethodNotFoundMessage(method));
        }

        public static JSONRPCResponse ofInternalError(Object id, String message) {
            return ofError(id, ErrorCodes.INTERNAL_ERROR, message);
        }

        @Override
        public String jsonrpc() {
            return jsonrpc;
        }

        public void jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }

        public Object id() {
            return id;
        }

        public void id(Object id) {
            this.id = id;
        }

        public Object result() {
            return result;
        }

        public void result(Object result) {
            this.result = result;
        }

        public JSONRPCError error() {
            return error;
        }

        public void error(JSONRPCError error) {
            this.error = error;
        }

        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc(jsonrpc);
        }

        public Object getId() {
            return this.id();
        }

        public void setId(Object id) {
            this.id(id);
        }

        public Object getResult() {
            return this.result();
        }

        public void setResult(Object result) {
            this.result(result);
        }

        public JSONRPCError getError() {
            return this.error();
        }

        public void setError(JSONRPCError error) {
            this.error(error);
        }

        @Override
        public String toString() {
            return "JSONRPCResponse{" +
                    "jsonrpc='" + jsonrpc + '\'' +
                    ", id=" + id +
                    ", result=" + result +
                    ", error=" + error +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCNotification implements JSONRPCMessage {
        @JsonProperty("jsonrpc")
        private String jsonrpc;

        @JsonProperty("method")
        private String method;

        @JsonProperty("params")
        private Object params;

        JSONRPCNotification() {
        }

        public JSONRPCNotification(String jsonrpc, String method, Object params) {
            this.jsonrpc = jsonrpc;
            this.method = method;
            this.params = params;
        }

        public static JSONRPCNotification of(String method, Object params) {
            return new JSONRPCNotification(JSONRPC_VERSION, method, params);
        }

        @Override
        public String jsonrpc() {
            return jsonrpc;
        }

        public void jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }

        public String method() {
            return method;
        }

        public void method(String method) {
            this.method = method;
        }

        public Object params() {
            return params;
        }

        public void params(Object params) {
            this.params = params;
        }

        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc(jsonrpc);
        }

        public String getMethod() {
            return this.method();
        }

        public void setMethod(String method) {
            this.method(method);
        }

        public Object getParams() {
            return this.params();
        }

        public void setParams(Object params) {
            this.params(params);
        }

        @Override
        public String toString() {
            return "JSONRPCNotification{" +
                    "jsonrpc='" + jsonrpc + '\'' +
                    ", method='" + method + '\'' +
                    ", params=" + params +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCError implements Serializable {
        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        @Nullable
        @JsonProperty("data")
        private Object data;

        JSONRPCError() {
        }

        public JSONRPCError(int code, String message) {
            this(code, message, null);
        }

        public JSONRPCError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int code() {
            return code;
        }

        public void code(int code) {
            this.code = code;
        }

        public String message() {
            return message;
        }

        public void message(String message) {
            this.message = message;
        }

        public Object data() {
            return data;
        }

        public void data(Object data) {
            this.data = data;
        }

        public int getCode() {
            return this.code();
        }

        public void setCode(int code) {
            this.code(code);
        }

        public String getMessage() {
            return this.message();
        }

        public void setMessage(String message) {
            this.message(message);
        }

        public Object getData() {
            return this.data();
        }

        public void setData(Object data) {
            this.data(data);
        }

        @Override
        public String toString() {
            return "JSONRPCError{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JsonSchema implements Serializable {
        @JsonProperty("type")
        private String type;

        @JsonProperty("properties")
        private Map<String, Object> properties;

        @JsonProperty("required")
        private List<String> required;

        @JsonProperty("additionalProperties")
        private Boolean additionalProperties;

        @JsonProperty("$defs")
        private Map<String, Object> defs;

        @JsonProperty("definitions")
        private Map<String, Object> definitions;

        JsonSchema() {
        }

        public JsonSchema(String type,
                          Map<String, Object> properties,
                          List<String> required,
                          Boolean additionalProperties,
                          Map<String, Object> defs,
                          Map<String, Object> definitions) {
            this.type = type;
            this.properties = properties;
            this.required = required;
            this.additionalProperties = additionalProperties;
            this.defs = defs;
            this.definitions = definitions;
        }

        public String type() {
            return type;
        }

        public void type(String type) {
            this.type = type;
        }

        public Map<String, Object> properties() {
            return properties;
        }

        public void properties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public List<String> required() {
            return required;
        }

        public void required(List<String> required) {
            this.required = required;
        }

        public Boolean additionalProperties() {
            return additionalProperties;
        }

        public void additionalProperties(Boolean additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        public Map<String, Object> defs() {
            return defs;
        }

        public void defs(Map<String, Object> defs) {
            this.defs = defs;
        }

        public Map<String, Object> definitions() {
            return definitions;
        }

        public void definitions(Map<String, Object> definitions) {
            this.definitions = definitions;
        }

        public String getType() {
            return this.type();
        }

        public void setType(String type) {
            this.type(type);
        }

        public Map<String, Object> getProperties() {
            return this.properties();
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties(properties);
        }

        public List<String> getRequired() {
            return this.required();
        }

        public void setRequired(List<String> required) {
            this.required(required);
        }

        public Boolean getAdditionalProperties() {
            return this.additionalProperties();
        }

        public void setAdditionalProperties(Boolean additionalProperties) {
            this.additionalProperties(additionalProperties);
        }

        public Map<String, Object> getDefs() {
            return this.defs();
        }

        public void setDefs(Map<String, Object> defs) {
            this.defs(defs);
        }

        public Map<String, Object> getDefinitions() {
            return this.definitions();
        }

        public void setDefinitions(Map<String, Object> definitions) {
            this.definitions(definitions);
        }

        @Override
        public String toString() {
            return "JsonSchema{" +
                    "type='" + type + '\'' +
                    ", properties=" + properties +
                    ", required=" + required +
                    ", additionalProperties=" + additionalProperties +
                    ", defs=" + defs +
                    ", definitions=" + definitions +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InitializeRequest implements Request {
        @JsonProperty("protocolVersion")
        private String protocolVersion;

        @JsonProperty("capabilities")
        private ClientCapabilities capabilities;

        @JsonProperty("clientInfo")
        private Implementation clientInfo;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        InitializeRequest() {
        }

        public InitializeRequest(String protocolVersion, ClientCapabilities capabilities, Implementation clientInfo) {
            this(protocolVersion, capabilities, clientInfo, null);
        }

        public InitializeRequest(String protocolVersion, ClientCapabilities capabilities,
                                 Implementation clientInfo, Map<String, Object> meta) {
            this.protocolVersion = protocolVersion;
            this.capabilities = capabilities;
            this.clientInfo = clientInfo;
            this.meta = meta;
        }

        public String protocolVersion() {
            return protocolVersion;
        }

        public void protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public ClientCapabilities capabilities() {
            return capabilities;
        }

        public void capabilities(ClientCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        public Implementation clientInfo() {
            return clientInfo;
        }

        public void clientInfo(Implementation clientInfo) {
            this.clientInfo = clientInfo;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getProtocolVersion() {
            return this.protocolVersion();
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion(protocolVersion);
        }

        public ClientCapabilities getCapabilities() {
            return this.capabilities();
        }

        public void setCapabilities(ClientCapabilities capabilities) {
            this.capabilities(capabilities);
        }

        public Implementation getClientInfo() {
            return this.clientInfo();
        }

        public void setClientInfo(Implementation clientInfo) {
            this.clientInfo(clientInfo);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "InitializeRequest{" +
                    "protocolVersion='" + protocolVersion + '\'' +
                    ", capabilities=" + capabilities +
                    ", clientInfo=" + clientInfo +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InitializeResult implements Result {
        @JsonProperty("protocolVersion")
        private String protocolVersion;

        @JsonProperty("capabilities")
        private ServerCapabilities capabilities;

        @JsonProperty("serverInfo")
        private Implementation serverInfo;

        @JsonProperty("instructions")
        private String instructions;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        InitializeResult() {
        }

        public InitializeResult(String protocolVersion, ServerCapabilities capabilities,
                                Implementation serverInfo, String instructions) {
            this(protocolVersion, capabilities, serverInfo, instructions, null);
        }

        public InitializeResult(String protocolVersion, ServerCapabilities capabilities,
                                Implementation serverInfo, String instructions, Map<String, Object> meta) {
            this.protocolVersion = protocolVersion;
            this.capabilities = capabilities;
            this.serverInfo = serverInfo;
            this.instructions = instructions;
            this.meta = meta;
        }

        public String protocolVersion() {
            return protocolVersion;
        }

        public void protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public ServerCapabilities capabilities() {
            return capabilities;
        }

        public void capabilities(ServerCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        public Implementation serverInfo() {
            return serverInfo;
        }

        public void serverInfo(Implementation serverInfo) {
            this.serverInfo = serverInfo;
        }

        public String instructions() {
            return instructions;
        }

        public void instructions(String instructions) {
            this.instructions = instructions;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getProtocolVersion() {
            return this.protocolVersion();
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion(protocolVersion);
        }

        public ServerCapabilities getCapabilities() {
            return this.capabilities();
        }

        public void setCapabilities(ServerCapabilities capabilities) {
            this.capabilities(capabilities);
        }

        public Implementation getServerInfo() {
            return this.serverInfo();
        }

        public void setServerInfo(Implementation serverInfo) {
            this.serverInfo(serverInfo);
        }

        public String getInstructions() {
            return this.instructions();
        }

        public void setInstructions(String instructions) {
            this.instructions(instructions);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "InitializeResult{" +
                    "protocolVersion='" + protocolVersion + '\'' +
                    ", capabilities=" + capabilities +
                    ", serverInfo=" + serverInfo +
                    ", instructions='" + instructions + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tool implements Serializable {
        @JsonProperty("name")
        private String name;

        @Nullable
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("inputSchema")
        private JsonSchema inputSchema;

        @Nullable
        @JsonProperty("outputSchema")
        private Map<String, Object> outputSchema;

        @Nullable
        @JsonProperty("annotations")
        private ToolAnnotations annotations;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        Tool() {
        }

        public Tool(String name, String title, String description,
                    JsonSchema inputSchema, Map<String, Object> outputSchema,
                    ToolAnnotations annotations, Map<String, Object> meta) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.inputSchema = inputSchema;
            this.outputSchema = outputSchema;
            this.annotations = annotations;
            this.meta = meta;
        }

        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public String description() {
            return description;
        }

        public void description(String description) {
            this.description = description;
        }

        public JsonSchema inputSchema() {
            return inputSchema;
        }

        public void inputSchema(JsonSchema inputSchema) {
            this.inputSchema = inputSchema;
        }

        public Map<String, Object> outputSchema() {
            return outputSchema;
        }

        public void outputSchema(Map<String, Object> outputSchema) {
            this.outputSchema = outputSchema;
        }

        public ToolAnnotations annotations() {
            return annotations;
        }

        public void annotations(ToolAnnotations annotations) {
            this.annotations = annotations;
        }

        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getName() {
            return this.name();
        }

        public void setName(String name) {
            this.name(name);
        }

        public String getTitle() {
            return this.title();
        }

        public void setTitle(String title) {
            this.title(title);
        }

        public String getDescription() {
            return this.description();
        }

        public void setDescription(String description) {
            this.description(description);
        }

        public JsonSchema getInputSchema() {
            return this.inputSchema();
        }

        public void setInputSchema(JsonSchema inputSchema) {
            this.inputSchema(inputSchema);
        }

        public Map<String, Object> getOutputSchema() {
            return this.outputSchema();
        }

        public void setOutputSchema(Map<String, Object> outputSchema) {
            this.outputSchema(outputSchema);
        }

        public ToolAnnotations getAnnotations() {
            return this.annotations();
        }

        public void setAnnotations(ToolAnnotations annotations) {
            this.annotations(annotations);
        }

        public Map<String, Object> getMeta() {
            return this.meta();
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "Tool{" +
                    "name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", inputSchema=" + inputSchema +
                    ", outputSchema=" + outputSchema +
                    ", annotations=" + annotations +
                    ", meta=" + meta +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private String title;
            private String description;
            private JsonSchema inputSchema;
            private Map<String, Object> outputSchema;
            private ToolAnnotations annotations;
            private Map<String, Object> meta;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder inputSchema(JsonSchema inputSchema) {
                this.inputSchema = inputSchema;
                return this;
            }

            public Builder inputSchema(McpJsonMapper jsonMapper, String inputSchema) {
                this.inputSchema = parseSchema(jsonMapper, inputSchema);
                return this;
            }

            public Builder outputSchema(Map<String, Object> outputSchema) {
                this.outputSchema = outputSchema;
                return this;
            }

            public Builder outputSchema(McpJsonMapper jsonMapper, String outputSchema) {
                this.outputSchema = schemaToMap(jsonMapper, outputSchema);
                return this;
            }

            public Builder annotations(ToolAnnotations annotations) {
                this.annotations = annotations;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Tool build() {
                Assert.hasText(name, "name must not be empty");
                return new Tool(name, title, description, inputSchema, outputSchema, annotations, meta);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolAnnotations implements Serializable {
        @JsonProperty("title")
        private String title;

        @JsonProperty("readOnlyHint")
        private Boolean readOnlyHint;

        @JsonProperty("destructiveHint")
        private Boolean destructiveHint;

        @JsonProperty("idempotentHint")
        private Boolean idempotentHint;

        @JsonProperty("openWorldHint")
        private Boolean openWorldHint;

        @JsonProperty("returnDirect")
        private Boolean returnDirect;

        ToolAnnotations() {
        }

        public ToolAnnotations(String title, Boolean readOnlyHint,
                               Boolean destructiveHint, Boolean idempotentHint,
                               Boolean openWorldHint, Boolean returnDirect) {
            this.title = title;
            this.readOnlyHint = readOnlyHint;
            this.destructiveHint = destructiveHint;
            this.idempotentHint = idempotentHint;
            this.openWorldHint = openWorldHint;
            this.returnDirect = returnDirect;
        }

        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public Boolean readOnlyHint() {
            return readOnlyHint;
        }

        public void readOnlyHint(Boolean readOnlyHint) {
            this.readOnlyHint = readOnlyHint;
        }

        public Boolean destructiveHint() {
            return destructiveHint;
        }

        public void destructiveHint(Boolean destructiveHint) {
            this.destructiveHint = destructiveHint;
        }

        public Boolean idempotentHint() {
            return idempotentHint;
        }

        public void idempotentHint(Boolean idempotentHint) {
            this.idempotentHint = idempotentHint;
        }

        public Boolean openWorldHint() {
            return openWorldHint;
        }

        public void openWorldHint(Boolean openWorldHint) {
            this.openWorldHint = openWorldHint;
        }

        public Boolean returnDirect() {
            return returnDirect;
        }

        public void returnDirect(Boolean returnDirect) {
            this.returnDirect = returnDirect;
        }

        public String getTitle() {
            return this.title();
        }

        public void setTitle(String title) {
            this.title(title);
        }

        public Boolean getReadOnlyHint() {
            return this.readOnlyHint();
        }

        public void setReadOnlyHint(Boolean readOnlyHint) {
            this.readOnlyHint(readOnlyHint);
        }

        public Boolean getDestructiveHint() {
            return this.destructiveHint();
        }

        public void setDestructiveHint(Boolean destructiveHint) {
            this.destructiveHint(destructiveHint);
        }

        public Boolean getIdempotentHint() {
            return this.idempotentHint();
        }

        public void setIdempotentHint(Boolean idempotentHint) {
            this.idempotentHint(idempotentHint);
        }

        public Boolean getOpenWorldHint() {
            return this.openWorldHint();
        }

        public void setOpenWorldHint(Boolean openWorldHint) {
            this.openWorldHint(openWorldHint);
        }

        public Boolean getReturnDirect() {
            return this.returnDirect();
        }

        public void setReturnDirect(Boolean returnDirect) {
            this.returnDirect(returnDirect);
        }

        @Override
        public String toString() {
            return "ToolAnnotations{" +
                    "title='" + title + '\'' +
                    ", readOnlyHint=" + readOnlyHint +
                    ", destructiveHint=" + destructiveHint +
                    ", idempotentHint=" + idempotentHint +
                    ", openWorldHint=" + openWorldHint +
                    ", returnDirect=" + returnDirect +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Prompt implements BaseMetadata {
        @JsonProperty("name")
        private String name;

        @Nullable
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("arguments")
        private List<PromptArgument> arguments;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        Prompt() {
        }

        public Prompt(String name, String description, List<PromptArgument> arguments) {
            this(name, null, description, arguments);
        }

        public Prompt(String name, String title, String description, List<PromptArgument> arguments) {
            this(name, title, description, arguments, null);
        }

        public Prompt(String name, String title, String description, List<PromptArgument> arguments, Map<String, Object> meta) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.arguments = arguments != null ? arguments : new ArrayList<>();
            this.meta = meta;
        }

        @Override
        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        @Override
        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public String description() {
            return description;
        }

        public void description(String description) {
            this.description = description;
        }

        public List<PromptArgument> arguments() {
            return arguments;
        }

        public void arguments(List<PromptArgument> arguments) {
            this.arguments = arguments;
        }

        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setName(String name) {
            this.name(name);
        }

        public void setTitle(String title) {
            this.title(title);
        }

        public String getDescription() {
            return this.description();
        }

        public void setDescription(String description) {
            this.description(description);
        }

        public List<PromptArgument> getArguments() {
            return this.arguments();
        }

        public void setArguments(List<PromptArgument> arguments) {
            this.arguments(arguments);
        }

        public Map<String, Object> getMeta() {
            return this.meta();
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "Prompt{" +
                    "name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", arguments=" + arguments +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptArgument implements BaseMetadata {
        @JsonProperty("name")
        private String name;

        @Nullable
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("required")
        private Boolean required;

        PromptArgument() {
        }

        public PromptArgument(String name, String description, Boolean required) {
            this(name, description, null, required);
        }

        public PromptArgument(String name, String title, String description, Boolean required) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.required = required;
        }

        @Override
        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        @Override
        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public String description() {
            return description;
        }

        public void description(String description) {
            this.description = description;
        }

        public Boolean required() {
            return required;
        }

        public void required(Boolean required) {
            this.required = required;
        }

        public void setName(String name) {
            this.name(name);
        }

        public void setTitle(String title) {
            this.title(title);
        }

        public String getDescription() {
            return this.description();
        }

        public void setDescription(String description) {
            this.description(description);
        }

        public Boolean getRequired() {
            return this.required();
        }

        public void setRequired(Boolean required) {
            this.required(required);
        }

        @Override
        public String toString() {
            return "PromptArgument{" +
                    "name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", required=" + required +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptMessage implements Serializable {
        @JsonProperty("role")
        private Role role;

        @JsonProperty("content")
        private Content content;

        PromptMessage() {
        }

        public PromptMessage(Role role, Content content) {
            this.role = role;
            this.content = content;
        }

        public Role role() {
            return role;
        }

        public void role(Role role) {
            this.role = role;
        }

        public Content content() {
            return content;
        }

        public void content(Content content) {
            this.content = content;
        }

        public Role getRole() {
            return this.role();
        }

        public void setRole(Role role) {
            this.role(role);
        }

        public Content getContent() {
            return this.content();
        }

        public void setContent(Content content) {
            this.content(content);
        }

        @Override
        public String toString() {
            return "PromptMessage{" +
                    "role=" + role +
                    ", content=" + content +
                    '}';
        }
    }

    /* Tool */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallToolRequest implements Request {
        @JsonProperty("name")
        private String name;

        @JsonProperty("arguments")
        private Map<String, Object> arguments;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        CallToolRequest() {
        }

        public CallToolRequest(McpJsonMapper jsonMapper, String name, String jsonArguments) {
            this(name, parseJsonArguments(jsonMapper, jsonArguments), null);
        }

        public CallToolRequest(String name, Map<String, Object> arguments) {
            this(name, arguments, null);
        }

        public CallToolRequest(String name, Map<String, Object> arguments, Map<String, Object> meta) {
            this.name = name;
            this.arguments = arguments;
            this.meta = meta;
        }

        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        public Map<String, Object> arguments() {
            return arguments;
        }

        public void arguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getName() {
            return this.name();
        }

        public void setName(String name) {
            this.name(name);
        }

        public Map<String, Object> getArguments() {
            return this.arguments();
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments(arguments);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "CallToolRequest{" +
                    "name='" + name + '\'' +
                    ", arguments=" + arguments +
                    ", meta=" + meta +
                    '}';
        }

        private static Map<String, Object> parseJsonArguments(McpJsonMapper jsonMapper, String jsonArguments) {
            try {
                return jsonMapper.readValue(jsonArguments, MAP_TYPE_REF);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private Map<String, Object> arguments;
            private Map<String, Object> meta;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder arguments(Map<String, Object> arguments) {
                this.arguments = arguments;
                return this;
            }

            public Builder arguments(McpJsonMapper jsonMapper, String jsonArguments) {
                this.arguments = parseJsonArguments(jsonMapper, jsonArguments);
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Builder progressToken(String progressToken) {
                if (this.meta == null) {
                    this.meta = new HashMap<>();
                }
                this.meta.put(Request.PROGRESS_TOKEN, progressToken);
                return this;
            }

            public CallToolRequest build() {
                Assert.hasText(name, "name must not be empty");
                return new CallToolRequest(name, arguments, meta);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallToolResult implements Result {
        @JsonProperty("content")
        private List<Content> content;

        @JsonProperty("isError")
        private Boolean isError;

        @Nullable
        @JsonProperty("structuredContent")
        private Object structuredContent;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        CallToolResult() {
        }

        public CallToolResult(String content, Boolean isError) {
            this(Collections.unmodifiableList(new ArrayList<Content>() {{
                add(new TextContent(content));
            }}), isError, null, null);
        }

        public CallToolResult(List<Content> content, Boolean isError, Object structuredContent, Map<String, Object> meta) {
            this.content = content;
            this.isError = isError;
            this.structuredContent = structuredContent;
            this.meta = meta;
        }

        public List<Content> content() {
            return content;
        }

        public void content(List<Content> content) {
            this.content = content;
        }

        public Boolean isError() {
            return isError;
        }

        public void isError(Boolean isError) {
            this.isError = isError;
        }

        public Object structuredContent() {
            return structuredContent;
        }

        public void structuredContent(Map<String, Object> structuredContent) {
            this.structuredContent = structuredContent;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<Content> getContent() {
            return this.content();
        }

        public void setContent(List<Content> content) {
            this.content(content);
        }

        public Boolean getError() {
            return this.isError();
        }

        public void setError(Boolean isError) {
            this.isError(isError);
        }

        public Object getStructuredContent() {
            return this.structuredContent();
        }

        public void setStructuredContent(Map<String, Object> structuredContent) {
            this.structuredContent(structuredContent);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "CallToolResult{" +
                    "content=" + content +
                    ", isError=" + isError +
                    ", structuredContent=" + structuredContent +
                    ", meta=" + meta +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<Content> content = new ArrayList<>();
            private Boolean isError = false;
            private Object structuredContent;
            private Map<String, Object> meta;

            public Builder content(List<Content> content) {
                Assert.notNull(content, "content must not be null");
                this.content = content;
                return this;
            }

            public Builder structuredContent(Object structuredContent) {
                Assert.notNull(structuredContent, "structuredContent must not be null");
                this.structuredContent = structuredContent;
                return this;
            }

            public Builder structuredContent(McpJsonMapper jsonMapper, String structuredContent) {
                Assert.hasText(structuredContent, "structuredContent must not be empty");
                try {
                    this.structuredContent = jsonMapper.readValue(structuredContent, MAP_TYPE_REF);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid structured content: " + structuredContent, e);
                }
                return this;
            }

            public Builder textContent(List<String> textContent) {
                Assert.notNull(textContent, "textContent must not be null");
                textContent.stream().map(TextContent::new).forEach(this.content::add);
                return this;
            }

            public Builder addContent(Content contentItem) {
                Assert.notNull(contentItem, "contentItem must not be null");
                if (this.content == null) {
                    this.content = new ArrayList<>();
                }
                this.content.add(contentItem);
                return this;
            }

            public Builder addTextContent(String text) {
                Assert.notNull(text, "text must not be null");
                return addContent(new TextContent(text));
            }

            public Builder isError(Boolean isError) {
                Assert.notNull(isError, "isError must not be null");
                this.isError = isError;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public CallToolResult build() {
                return new CallToolResult(content, isError, structuredContent, meta);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListToolsResult implements Result {
        @JsonProperty("tools")
        private List<Tool> tools;

        @Nullable
        @JsonProperty("nextCursor")
        private String nextCursor;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ListToolsResult() {
        }

        public ListToolsResult(List<Tool> tools, String nextCursor) {
            this(tools, nextCursor, null);
        }

        public ListToolsResult(List<Tool> tools, String nextCursor, Map<String, Object> meta) {
            this.tools = tools;
            this.nextCursor = nextCursor;
            this.meta = meta;
        }

        public List<Tool> tools() {
            return tools;
        }

        public void tools(List<Tool> tools) {
            this.tools = tools;
        }

        public String nextCursor() {
            return nextCursor;
        }

        public void nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<Tool> getTools() {
            return this.tools();
        }

        public void setTools(List<Tool> tools) {
            this.tools(tools);
        }

        public String getNextCursor() {
            return this.nextCursor();
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor(nextCursor);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ListToolsResult{" +
                    "tools=" + tools +
                    ", nextCursor='" + nextCursor + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    /* Resource */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReadResourceRequest implements Request {
        @JsonProperty("uri")
        private String uri;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ReadResourceRequest() {
        }

        public ReadResourceRequest(String uri) {
            this(uri, null);
        }

        public ReadResourceRequest(String uri, Map<String, Object> meta) {
            this.uri = uri;
            this.meta = meta;
        }

        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getUri() {
            return this.uri();
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ReadResourceRequest{" +
                    "uri='" + uri + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReadResourceResult implements Result {
        @JsonProperty("contents")
        private List<ResourceContents> contents;

        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ReadResourceResult() {
        }

        public ReadResourceResult(List<ResourceContents> contents) {
            this(contents, null);
        }

        public ReadResourceResult(List<ResourceContents> contents, Map<String, Object> meta) {
            this.contents = contents;
            this.meta = meta;
        }

        public List<ResourceContents> contents() {
            return contents;
        }

        public void contents(List<ResourceContents> contents) {
            this.contents = contents;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<ResourceContents> getContents() {
            return this.contents();
        }

        public void setContents(List<ResourceContents> contents) {
            this.contents(contents);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ReadResourceResult{" +
                    "contents=" + contents +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResourcesResult implements Result {
        @JsonProperty("resources")
        private List<Resource> resources;

        @Nullable
        @JsonProperty("nextCursor")
        private String nextCursor;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ListResourcesResult() {
        }

        public ListResourcesResult(List<Resource> resources, String nextCursor) {
            this(resources, nextCursor, null);
        }

        public ListResourcesResult(List<Resource> resources, String nextCursor, Map<String, Object> meta) {
            this.resources = resources;
            this.nextCursor = nextCursor;
            this.meta = meta;
        }

        public List<Resource> resources() {
            return resources;
        }

        public void resources(List<Resource> resources) {
            this.resources = resources;
        }

        public String nextCursor() {
            return nextCursor;
        }

        public void nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<Resource> getResources() {
            return this.resources();
        }

        public void setResources(List<Resource> resources) {
            this.resources(resources);
        }

        public String getNextCursor() {
            return this.nextCursor();
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor(nextCursor);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ListResourcesResult{" +
                    "resources=" + resources +
                    ", nextCursor='" + nextCursor + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResourceTemplatesResult implements Result {
        @JsonProperty("resourceTemplates")
        private List<ResourceTemplate> resourceTemplates;

        @Nullable
        @JsonProperty("nextCursor")
        private String nextCursor;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ListResourceTemplatesResult() {
        }

        public ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates) {
            this(resourceTemplates, null, null);
        }

        public ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates, String nextCursor) {
            this(resourceTemplates, nextCursor, null);
        }

        public ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates, String nextCursor, Map<String, Object> meta) {
            this.resourceTemplates = resourceTemplates;
            this.nextCursor = nextCursor;
            this.meta = meta;
        }

        public List<ResourceTemplate> resourceTemplates() {
            return resourceTemplates;
        }

        public void resourceTemplates(List<ResourceTemplate> resourceTemplates) {
            this.resourceTemplates = resourceTemplates;
        }

        public String nextCursor() {
            return nextCursor;
        }

        public void nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<ResourceTemplate> getResourceTemplates() {
            return this.resourceTemplates();
        }

        public void setResourceTemplates(List<ResourceTemplate> resourceTemplates) {
            this.resourceTemplates(resourceTemplates);
        }

        public String getNextCursor() {
            return this.nextCursor();
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor(nextCursor);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ListResourceTemplatesResult{" +
                    "resourceTemplates=" + resourceTemplates +
                    ", nextCursor='" + nextCursor + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    /* Prompt */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetPromptRequest implements Request {
        @JsonProperty("name")
        private String name;

        @JsonProperty("arguments")
        private Map<String, Object> arguments;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        GetPromptRequest() {
        }

        public GetPromptRequest(String name, Map<String, Object> arguments) {
            this(name, arguments, null);
        }

        public GetPromptRequest(String name, Map<String, Object> arguments, Map<String, Object> meta) {
            this.name = name;
            this.arguments = arguments;
            this.meta = meta;
        }

        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        public Map<String, Object> arguments() {
            return arguments;
        }

        public void arguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getName() {
            return this.name();
        }

        public void setName(String name) {
            this.name(name);
        }

        public Map<String, Object> getArguments() {
            return this.arguments();
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments(arguments);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "GetPromptRequest{" +
                    "name='" + name + '\'' +
                    ", arguments=" + arguments +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetPromptResult implements Result {
        @JsonProperty("description")
        private String description;

        @JsonProperty("messages")
        private List<PromptMessage> messages;

        @JsonProperty("_meta")
        private Map<String, Object> meta;

        GetPromptResult() {
        }

        public GetPromptResult(String description, List<PromptMessage> messages) {
            this(description, messages, null);
        }

        public GetPromptResult(String description, List<PromptMessage> messages, Map<String, Object> meta) {
            this.description = description;
            this.messages = messages;
            this.meta = meta;
        }

        public String description() {
            return description;
        }

        public void description(String description) {
            this.description = description;
        }

        public List<PromptMessage> messages() {
            return messages;
        }

        public void messages(List<PromptMessage> messages) {
            this.messages = messages;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getDescription() {
            return this.description();
        }

        public void setDescription(String description) {
            this.description(description);
        }

        public List<PromptMessage> getMessages() {
            return this.messages();
        }

        public void setMessages(List<PromptMessage> messages) {
            this.messages(messages);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "GetPromptResult{" +
                    "description='" + description + '\'' +
                    ", messages=" + messages +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListPromptsResult implements Result {
        @JsonProperty("prompts")
        private List<Prompt> prompts;

        @Nullable
        @JsonProperty("nextCursor")
        private String nextCursor;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ListPromptsResult() {
        }

        public ListPromptsResult(List<Prompt> prompts, String nextCursor) {
            this(prompts, nextCursor, null);
        }

        public ListPromptsResult(List<Prompt> prompts, String nextCursor, Map<String, Object> meta) {
            this.prompts = prompts;
            this.nextCursor = nextCursor;
            this.meta = meta;
        }

        public List<Prompt> prompts() {
            return prompts;
        }

        public void prompts(List<Prompt> prompts) {
            this.prompts = prompts;
        }

        public String nextCursor() {
            return nextCursor;
        }

        public void nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<Prompt> getPrompts() {
            return this.prompts();
        }

        public void setPrompts(List<Prompt> prompts) {
            this.prompts(prompts);
        }

        public String getNextCursor() {
            return this.nextCursor();
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor(nextCursor);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ListPromptsResult{" +
                    "prompts=" + prompts +
                    ", nextCursor='" + nextCursor + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    /* CreateMessage */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateMessageRequest implements Request {
        @JsonProperty("messages")
        private List<SamplingMessage> messages;

        @JsonProperty("modelPreferences")
        private ModelPreferences modelPreferences;

        @JsonProperty("systemPrompt")
        private String systemPrompt;

        @JsonProperty("includeContext")
        private ContextInclusionStrategy includeContext;

        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("maxTokens")
        private Integer maxTokens;

        @JsonProperty("stopSequences")
        private List<String> stopSequences;

        @JsonProperty("metadata")
        private Map<String, Object> metadata;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        CreateMessageRequest() {
        }

        public CreateMessageRequest(List<SamplingMessage> messages,
                                    ModelPreferences modelPreferences,
                                    String systemPrompt,
                                    ContextInclusionStrategy includeContext,
                                    Double temperature,
                                    int maxTokens,
                                    List<String> stopSequences,
                                    Map<String, Object> metadata) {
            this(messages, modelPreferences, systemPrompt, includeContext, temperature, maxTokens, stopSequences, metadata, null);
        }

        public CreateMessageRequest(List<SamplingMessage> messages,
                                    ModelPreferences modelPreferences,
                                    String systemPrompt,
                                    ContextInclusionStrategy includeContext,
                                    Double temperature,
                                    Integer maxTokens,
                                    List<String> stopSequences,
                                    Map<String, Object> metadata,
                                    Map<String, Object> meta) {
            this.messages = messages;
            this.modelPreferences = modelPreferences;
            this.systemPrompt = systemPrompt;
            this.includeContext = includeContext;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.stopSequences = stopSequences;
            this.metadata = metadata;
            this.meta = meta;
        }

        public List<SamplingMessage> messages() {
            return messages;
        }

        public void messages(List<SamplingMessage> messages) {
            this.messages = messages;
        }

        public ModelPreferences modelPreferences() {
            return modelPreferences;
        }

        public void modelPreferences(ModelPreferences modelPreferences) {
            this.modelPreferences = modelPreferences;
        }

        public String systemPrompt() {
            return systemPrompt;
        }

        public void systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public ContextInclusionStrategy includeContext() {
            return includeContext;
        }

        public void includeContext(ContextInclusionStrategy includeContext) {
            this.includeContext = includeContext;
        }

        public Double temperature() {
            return temperature;
        }

        public void temperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer maxTokens() {
            return maxTokens;
        }

        public void maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public List<String> stopSequences() {
            return stopSequences;
        }

        public void stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
        }

        public Map<String, Object> metadata() {
            return metadata;
        }

        public void metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<SamplingMessage> getMessages() {
            return this.messages();
        }

        public void setMessages(List<SamplingMessage> messages) {
            this.messages(messages);
        }

        public ModelPreferences getModelPreferences() {
            return this.modelPreferences();
        }

        public void setModelPreferences(ModelPreferences modelPreferences) {
            this.modelPreferences(modelPreferences);
        }

        public String getSystemPrompt() {
            return this.systemPrompt();
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt(systemPrompt);
        }

        public ContextInclusionStrategy getIncludeContext() {
            return this.includeContext();
        }

        public void setIncludeContext(ContextInclusionStrategy includeContext) {
            this.includeContext(includeContext);
        }

        public Double getTemperature() {
            return this.temperature();
        }

        public void setTemperature(Double temperature) {
            this.temperature(temperature);
        }

        public Integer getMaxTokens() {
            return this.maxTokens();
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens(maxTokens);
        }

        public List<String> getStopSequences() {
            return this.stopSequences();
        }

        public void setStopSequences(List<String> stopSequences) {
            this.stopSequences(stopSequences);
        }

        public Map<String, Object> getMetadata() {
            return this.metadata();
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata(metadata);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "CreateMessageRequest{" +
                    "messages=" + messages +
                    ", modelPreferences=" + modelPreferences +
                    ", systemPrompt='" + systemPrompt + '\'' +
                    ", includeContext=" + includeContext +
                    ", temperature=" + temperature +
                    ", maxTokens=" + maxTokens +
                    ", stopSequences=" + stopSequences +
                    ", metadata=" + metadata +
                    ", meta=" + meta +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<SamplingMessage> messages;
            private ModelPreferences modelPreferences;
            private String systemPrompt;
            private ContextInclusionStrategy includeContext;
            private Double temperature;
            private int maxTokens;
            private List<String> stopSequences;
            private Map<String, Object> metadata;
            private Map<String, Object> meta;

            public Builder messages(List<SamplingMessage> messages) {
                this.messages = messages;
                return this;
            }

            public Builder modelPreferences(ModelPreferences modelPreferences) {
                this.modelPreferences = modelPreferences;
                return this;
            }

            public Builder systemPrompt(String systemPrompt) {
                this.systemPrompt = systemPrompt;
                return this;
            }

            public Builder includeContext(ContextInclusionStrategy includeContext) {
                this.includeContext = includeContext;
                return this;
            }

            public Builder temperature(Double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder stopSequences(List<String> stopSequences) {
                this.stopSequences = stopSequences;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Builder progressToken(String progressToken) {
                if (this.meta == null) {
                    this.meta = new HashMap<>();
                }
                this.meta.put(Request.PROGRESS_TOKEN, progressToken);
                return this;
            }

            public CreateMessageRequest build() {
                return new CreateMessageRequest(
                        messages, modelPreferences, systemPrompt,
                        includeContext, temperature, maxTokens,
                        stopSequences, metadata, meta);
            }
        }

        public enum ContextInclusionStrategy implements Serializable {
            @JsonProperty("none")
            NONE,
            @JsonProperty("thisServer")
            THIS_SERVER,
            @JsonProperty("allServers")
            ALL_SERVERS
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateMessageResult implements Result {
        @JsonProperty("role")
        private Role role;

        @JsonProperty("content")
        private Content content;

        @JsonProperty("model")
        private String model;

        @JsonProperty("stopReason")
        private StopReason stopReason;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        CreateMessageResult() {
        }

        public CreateMessageResult(Role role, Content content, String model, StopReason stopReason) {
            this(role, content, model, stopReason, null);
        }

        public CreateMessageResult(Role role, Content content, String model, StopReason stopReason, Map<String, Object> meta) {
            this.role = role;
            this.content = content;
            this.model = model;
            this.stopReason = stopReason;
            this.meta = meta;
        }

        public Role role() {
            return role;
        }

        public void role(Role role) {
            this.role = role;
        }

        public Content content() {
            return content;
        }

        public void content(Content content) {
            this.content = content;
        }

        public String model() {
            return model;
        }

        public void model(String model) {
            this.model = model;
        }

        public StopReason stopReason() {
            return stopReason;
        }

        public void stopReason(StopReason stopReason) {
            this.stopReason = stopReason;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public Role getRole() {
            return this.role();
        }

        public void setRole(Role role) {
            this.role(role);
        }

        public Content getContent() {
            return this.content();
        }

        public void setContent(Content content) {
            this.content(content);
        }

        public String getModel() {
            return this.model();
        }

        public void setModel(String model) {
            this.model(model);
        }

        public StopReason getStopReason() {
            return this.stopReason();
        }

        public void setStopReason(StopReason stopReason) {
            this.stopReason(stopReason);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "CreateMessageResult{" +
                    "role=" + role +
                    ", content=" + content +
                    ", model='" + model + '\'' +
                    ", stopReason=" + stopReason +
                    ", meta=" + meta +
                    '}';
        }

        public enum StopReason implements Serializable {
            @JsonProperty("endTurn")
            END_TURN("endTurn"),

            @JsonProperty("stopSequence")
            STOP_SEQUENCE("stopSequence"),

            @JsonProperty("maxTokens")
            MAX_TOKENS("maxTokens"),

            @JsonProperty("unknown")
            UNKNOWN("unknown");

            private final String value;

            StopReason(String value) {
                this.value = value;
            }

            public static StopReason of(String value) {
                return Arrays.stream(StopReason.values())
                        .filter(stopReason -> stopReason.value.equals(value))
                        .findFirst()
                        .orElse(StopReason.UNKNOWN);
            }

            public String value() {
                return value;
            }

            public String getValue() {
                return this.value();
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Role role = Role.ASSISTANT;
            private Content content;
            private String model;
            private StopReason stopReason = StopReason.END_TURN;
            private Map<String, Object> meta;

            public Builder role(Role role) {
                this.role = role;
                return this;
            }

            public Builder content(Content content) {
                this.content = content;
                return this;
            }

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder stopReason(StopReason stopReason) {
                this.stopReason = stopReason;
                return this;
            }

            public Builder message(String message) {
                this.content = new TextContent(message);
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public CreateMessageResult build() {
                return new CreateMessageResult(role, content, model, stopReason, meta);
            }
        }
    }

    /* Elicit */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ElicitRequest implements Request {
        @JsonProperty("message")
        private String message;

        @JsonProperty("requestedSchema")
        private Map<String, Object> requestedSchema;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ElicitRequest() {
        }

        public ElicitRequest(String message, Map<String, Object> requestedSchema) {
            this(message, requestedSchema, null);
        }

        public ElicitRequest(String message, Map<String, Object> requestedSchema, Map<String, Object> meta) {
            this.message = message;
            this.requestedSchema = requestedSchema;
            this.meta = meta;
        }

        public String message() {
            return message;
        }

        public void message(String message) {
            this.message = message;
        }

        public Map<String, Object> requestedSchema() {
            return requestedSchema;
        }

        public void requestedSchema(Map<String, Object> requestedSchema) {
            this.requestedSchema = requestedSchema;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getMessage() {
            return this.message();
        }

        public void setMessage(String message) {
            this.message(message);
        }

        public Map<String, Object> getRequestedSchema() {
            return this.requestedSchema();
        }

        public void setRequestedSchema(Map<String, Object> requestedSchema) {
            this.requestedSchema(requestedSchema);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ElicitRequest{" +
                    "message='" + message + '\'' +
                    ", requestedSchema=" + requestedSchema +
                    ", meta=" + meta +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String message;
            private Map<String, Object> requestedSchema;
            private Map<String, Object> meta;

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder requestedSchema(Map<String, Object> requestedSchema) {
                this.requestedSchema = requestedSchema;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Builder progressToken(String progressToken) {
                if (this.meta == null) {
                    this.meta = new HashMap<>();
                }
                this.meta.put(Request.PROGRESS_TOKEN, progressToken);
                return this;
            }

            public ElicitRequest build() {
                return new ElicitRequest(message, requestedSchema, meta);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ElicitResult implements Result {
        @JsonProperty("action")
        private Action action;

        @JsonProperty("content")
        private Map<String, Object> content;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ElicitResult() {
        }

        public ElicitResult(Action action, Map<String, Object> content) {
            this(action, content, null);
        }

        public ElicitResult(Action action, Map<String, Object> content, Map<String, Object> meta) {
            this.action = action;
            this.content = content;
            this.meta = meta;
        }

        public Action action() {
            return action;
        }

        public void action(Action action) {
            this.action = action;
        }

        public Map<String, Object> content() {
            return content;
        }

        public void content(Map<String, Object> content) {
            this.content = content;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public Action getAction() {
            return this.action();
        }

        public void setAction(Action action) {
            this.action(action);
        }

        public Map<String, Object> getContent() {
            return this.content();
        }

        public void setContent(Map<String, Object> content) {
            this.content(content);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ElicitResult{" +
                    "action=" + action +
                    ", content=" + content +
                    ", meta=" + meta +
                    '}';
        }

        public enum Action implements Serializable {
            @JsonProperty("accept")
            ACCEPT("accept"),

            @JsonProperty("decline")
            DECLINE("decline"),

            @JsonProperty("cancel")
            CANCEL("cancel");

            private final String value;

            Action(String value) {
                this.value = value;
            }

            public String value() {
                return value;
            }

            public String getValue() {
                return this.value();
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Action action;
            private Map<String, Object> content;
            private Map<String, Object> meta;

            public Builder message(Action action) {
                this.action = action;
                return this;
            }

            public Builder content(Map<String, Object> content) {
                this.content = content;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public ElicitResult build() {
                return new ElicitResult(action, content, meta);
            }
        }
    }

    /* Complete */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompleteRequest implements Request {
        @Nullable
        @JsonProperty("ref")
        private CompleteReference ref;

        @JsonProperty("argument")
        private CompleteArgument argument;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        @Nullable
        @JsonProperty("context")
        private CompleteContext context;

        CompleteRequest() {
        }

        public CompleteRequest(CompleteReference ref, CompleteArgument argument) {
            this(ref, argument, null, null);
        }

        public CompleteRequest(CompleteReference ref, CompleteArgument argument, CompleteContext context) {
            this(ref, argument, null, context);
        }

        public CompleteRequest(CompleteReference ref, CompleteArgument argument, Map<String, Object> meta, CompleteContext context) {
            this.ref = ref;
            this.argument = argument;
            this.meta = meta;
            this.context = context;
        }

        public CompleteReference ref() {
            return ref;
        }

        public void ref(CompleteReference ref) {
            this.ref = ref;
        }

        public CompleteArgument argument() {
            return argument;
        }

        public void argument(CompleteArgument argument) {
            this.argument = argument;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public CompleteContext context() {
            return context;
        }

        public void context(CompleteContext context) {
            this.context = context;
        }

        public CompleteReference getRef() {
            return this.ref();
        }

        public void setRef(CompleteReference ref) {
            this.ref(ref);
        }

        public CompleteArgument getArgument() {
            return this.argument();
        }

        public void setArgument(CompleteArgument argument) {
            this.argument(argument);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        public CompleteContext getContext() {
            return this.context();
        }

        public void setContext(CompleteContext context) {
            this.context(context);
        }

        @Override
        public String toString() {
            return "CompleteRequest{" +
                    "ref=" + ref +
                    ", argument=" + argument +
                    ", meta=" + meta +
                    ", context=" + context +
                    '}';
        }

        public static class CompleteArgument implements Serializable {
            @JsonProperty("name")
            private String name;

            @JsonProperty("value")
            private String value;

            CompleteArgument() {
            }

            public CompleteArgument(String name, String value) {
                this.name = name;
                this.value = value;
            }

            public String name() {
                return name;
            }

            public void name(String name) {
                this.name = name;
            }

            public String value() {
                return value;
            }

            public void value(String value) {
                this.value = value;
            }

            public String getName() {
                return this.name();
            }

            public void setName(String name) {
                this.name(name);
            }

            public String getValue() {
                return this.value();
            }

            public void setValue(String value) {
                this.value(value);
            }

            @Override
            public String toString() {
                return "CompleteArgument{" +
                        "name='" + name + '\'' +
                        ", value='" + value + '\'' +
                        '}';
            }
        }

        public static class CompleteContext implements Serializable {
            @JsonProperty("arguments")
            private Map<String, String> arguments;

            CompleteContext() {
            }

            public CompleteContext(Map<String, String> arguments) {
                this.arguments = arguments;
            }

            public Map<String, String> arguments() {
                return arguments;
            }

            public void arguments(Map<String, String> arguments) {
                this.arguments = arguments;
            }

            public Map<String, String> getArguments() {
                return arguments;
            }

            public void setArguments(Map<String, String> arguments) {
                this.arguments = arguments;
            }

            @Override
            public String toString() {
                return "CompleteContext{" +
                        "arguments=" + arguments +
                        '}';
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompleteResult implements Result {
        @JsonProperty("completion")
        private CompleteCompletion completion;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        CompleteResult() {
        }

        public CompleteResult(CompleteCompletion completion) {
            this(completion, null);
        }

        public CompleteResult(CompleteCompletion completion, Map<String, Object> meta) {
            this.completion = completion;
            this.meta = meta;
        }

        public CompleteCompletion completion() {
            return completion;
        }

        public void completion(CompleteCompletion completion) {
            this.completion = completion;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public CompleteCompletion getCompletion() {
            return this.completion();
        }

        public void setCompletion(CompleteCompletion completion) {
            this.completion(completion);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "CompleteResult{" +
                    "completion=" + completion +
                    ", meta=" + meta +
                    '}';
        }

        public static class CompleteCompletion implements Serializable {
            @JsonProperty("values")
            private List<String> values;

            @JsonProperty("total")
            private Integer total;

            @JsonProperty("hasMore")
            private Boolean hasMore;

            CompleteCompletion() {
            }

            public CompleteCompletion(List<String> values, Integer total, Boolean hasMore) {
                this.values = values;
                this.total = total;
                this.hasMore = hasMore;
            }

            public List<String> values() {
                return values;
            }

            public void values(List<String> values) {
                this.values = values;
            }

            public Integer total() {
                return total;
            }

            public void total(Integer total) {
                this.total = total;
            }

            public Boolean hasMore() {
                return hasMore;
            }

            public void hasMore(Boolean hasMore) {
                this.hasMore = hasMore;
            }

            public List<String> getValues() {
                return this.values();
            }

            public void setValues(List<String> values) {
                this.values(values);
            }

            public Integer getTotal() {
                return this.total();
            }

            public void setTotal(Integer total) {
                this.total(total);
            }

            public Boolean getHasMore() {
                return this.hasMore();
            }

            public void setHasMore(Boolean hasMore) {
                this.hasMore(hasMore);
            }

            @Override
            public String toString() {
                return "CompleteCompletion{" +
                        "values=" + values +
                        ", total=" + total +
                        ", hasMore=" + hasMore +
                        '}';
            }
        }
    }

    /* ListRoots */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListRootsResult implements Result {
        @JsonProperty("roots")
        private List<Root> roots;

        @Nullable
        @JsonProperty("nextCursor")
        private String nextCursor;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ListRootsResult() {
        }

        public ListRootsResult(List<Root> roots) {
            this(roots, null, null);
        }

        public ListRootsResult(List<Root> roots, String nextCursor) {
            this(roots, nextCursor, null);
        }

        public ListRootsResult(List<Root> roots, String nextCursor, Map<String, Object> meta) {
            this.roots = roots;
            this.nextCursor = nextCursor;
            this.meta = meta;
        }

        public List<Root> roots() {
            return roots;
        }

        public void roots(List<Root> roots) {
            this.roots = roots;
        }

        public String nextCursor() {
            return nextCursor;
        }

        public void nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public List<Root> getRoots() {
            return this.roots();
        }

        public void setRoots(List<Root> roots) {
            this.roots(roots);
        }

        public String getNextCursor() {
            return this.nextCursor();
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor(nextCursor);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ListRootsResult{" +
                    "roots=" + roots +
                    ", nextCursor='" + nextCursor + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaginatedRequest implements Request {
        @JsonProperty("cursor")
        private String cursor;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        public PaginatedRequest() {
            this(null);
        }

        public PaginatedRequest(String cursor) {
            this(cursor, null);
        }

        public PaginatedRequest(String cursor, Map<String, Object> meta) {
            this.cursor = cursor;
            this.meta = meta;
        }

        public String cursor() {
            return cursor;
        }

        public void cursor(String cursor) {
            this.cursor = cursor;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getCursor() {
            return this.cursor();
        }

        public void setCursor(String cursor) {
            this.cursor(cursor);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "PaginatedRequest{" +
                    "cursor='" + cursor + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscribeRequest implements Request {
        @JsonProperty("uri")
        private String uri;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        SubscribeRequest() {
        }

        public SubscribeRequest(String uri) {
            this(uri, null);
        }

        public SubscribeRequest(String uri, Map<String, Object> meta) {
            this.uri = uri;
            this.meta = meta;
        }

        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getUri() {
            return this.uri();
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "SubscribeRequest{" +
                    "uri='" + uri + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnsubscribeRequest implements Request {
        @JsonProperty("uri")
        private String uri;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        UnsubscribeRequest() {
        }

        public UnsubscribeRequest(String uri) {
            this(uri, null);
        }

        public UnsubscribeRequest(String uri, Map<String, Object> meta) {
            this.uri = uri;
            this.meta = meta;
        }

        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getUri() {
            return this.uri();
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "UnsubscribeRequest{" +
                    "uri='" + uri + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SetLevelRequest implements Serializable {
        @JsonProperty("level")
        private LoggingLevel level;

        SetLevelRequest() {
        }

        public SetLevelRequest(LoggingLevel level) {
            this.level = level;
        }

        public LoggingLevel level() {
            return level;
        }

        public void level(LoggingLevel level) {
            this.level = level;
        }

        public LoggingLevel getLevel() {
            return this.level();
        }

        public void setLevel(LoggingLevel level) {
            this.level(level);
        }

        @Override
        public String toString() {
            return "SetLevelRequest{" +
                    "level=" + level +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextContent implements Annotated, Content {
        @Nullable
        @JsonProperty("annotations")
        private Annotations annotations;

        @JsonProperty("text")
        private String text;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        TextContent() {
        }

        public TextContent(String text) {
            this(null, text, null);
        }

        public TextContent(Annotations annotations, String text) {
            this(annotations, text, null);
        }

        public TextContent(Annotations annotations, String text, Map<String, Object> meta) {
            this.annotations = annotations;
            this.text = text;
            this.meta = meta;
        }

        @Override
        public Annotations annotations() {
            return annotations;
        }

        public void annotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public String text() {
            return text;
        }

        public void text(String text) {
            this.text = text;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations(annotations);
        }

        public String getText() {
            return this.text();
        }

        public void setText(String text) {
            this.text(text);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "TextContent{" +
                    "annotations=" + annotations +
                    ", text='" + text + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageContent implements Annotated, Content {
        @Nullable
        @JsonProperty("annotations")
        private Annotations annotations;

        @JsonProperty("data")
        private String data;

        @JsonProperty("mimeType")
        private String mimeType;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ImageContent() {
        }

        public ImageContent(Annotations annotations, String data, String mimeType) {
            this(annotations, data, mimeType, null);
        }

        public ImageContent(Annotations annotations, String data, String mimeType, Map<String, Object> meta) {
            this.annotations = annotations;
            this.data = data;
            this.mimeType = mimeType;
            this.meta = meta;
        }

        @Override
        public Annotations annotations() {
            return annotations;
        }

        public void annotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public String data() {
            return data;
        }

        public void data(String data) {
            this.data = data;
        }

        public String mimeType() {
            return mimeType;
        }

        public void mimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations(annotations);
        }

        public String getData() {
            return this.data();
        }

        public void setData(String data) {
            this.data(data);
        }

        public String getMimeType() {
            return this.mimeType();
        }

        public void setMimeType(String mimeType) {
            this.mimeType(mimeType);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ImageContent{" +
                    "annotations=" + annotations +
                    ", data='" + data + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AudioContent implements Annotated, Content {
        @Nullable
        @JsonProperty("annotations")
        private Annotations annotations;

        @JsonProperty("data")
        private String data;

        @JsonProperty("mimeType")
        private String mimeType;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        AudioContent() {
        }

        public AudioContent(Annotations annotations, String data, String mimeType) {
            this(annotations, data, mimeType, null);
        }

        public AudioContent(Annotations annotations, String data, String mimeType, Map<String, Object> meta) {
            this.annotations = annotations;
            this.data = data;
            this.mimeType = mimeType;
            this.meta = meta;
        }

        @Override
        public Annotations annotations() {
            return annotations;
        }

        public void annotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public String data() {
            return data;
        }

        public void data(String data) {
            this.data = data;
        }

        public String mimeType() {
            return mimeType;
        }

        public void mimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations(annotations);
        }

        public String getData() {
            return this.data();
        }

        public void setData(String data) {
            this.data(data);
        }

        public String getMimeType() {
            return this.mimeType();
        }

        public void setMimeType(String mimeType) {
            this.mimeType(mimeType);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "AudioContent{" +
                    "annotations=" + annotations +
                    ", data='" + data + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedResource implements Annotated, Content {
        @Nullable
        @JsonProperty("annotations")
        private Annotations annotations;

        @JsonProperty("resource")
        private ResourceContents resource;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        EmbeddedResource() {
        }

        public EmbeddedResource(Annotations annotations, ResourceContents resource) {
            this(annotations, resource, null);
        }

        public EmbeddedResource(Annotations annotations, ResourceContents resource, Map<String, Object> meta) {
            this.annotations = annotations;
            this.resource = resource;
            this.meta = meta;
        }

        @Override
        public Annotations annotations() {
            return annotations;
        }

        public void annotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public ResourceContents resource() {
            return resource;
        }

        public void resource(ResourceContents resource) {
            this.resource = resource;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations(annotations);
        }

        public ResourceContents getResource() {
            return this.resource();
        }

        public void setResource(ResourceContents resource) {
            this.resource(resource);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "EmbeddedResource{" +
                    "annotations=" + annotations +
                    ", resource=" + resource +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceLink implements Annotated, Content, ResourceContent {
        @JsonProperty("name")
        private String name;

        @JsonProperty("title")
        private String title;

        @JsonProperty("uri")
        private String uri;

        @JsonProperty("description")
        private String description;

        @JsonProperty("mimeType")
        private String mimeType;

        @JsonProperty("size")
        private Long size;

        @Nullable
        @JsonProperty("annotations")
        private Annotations annotations;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ResourceLink() {
        }

        public ResourceLink(String name, String title, String uri,
                            String description, String mimeType, Long size,
                            Annotations annotations, Map<String, Object> meta) {
            this.name = name;
            this.title = title;
            this.uri = uri;
            this.description = description;
            this.mimeType = mimeType;
            this.size = size;
            this.annotations = annotations;
            this.meta = meta;
        }

        @Override
        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        @Override
        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        @Override
        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public String description() {
            return description;
        }

        public void description(String description) {
            this.description = description;
        }

        @Override
        public String mimeType() {
            return mimeType;
        }

        public void mimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public Long size() {
            return size;
        }

        public void size(Long size) {
            this.size = size;
        }

        @Override
        public Annotations annotations() {
            return annotations;
        }

        public void annotations(Annotations annotations) {
            this.annotations = annotations;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setName(String name) {
            this.name(name);
        }

        public void setTitle(String title) {
            this.title(title);
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setDescription(String description) {
            this.description(description);
        }

        public void setMimeType(String mimeType) {
            this.mimeType(mimeType);
        }

        public void setSize(Long size) {
            this.size(size);
        }

        @Override
        public Annotations getAnnotations() {
            return this.annotations();
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations(annotations);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ResourceLink{" +
                    "name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    ", uri='" + uri + '\'' +
                    ", description='" + description + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", size=" + size +
                    ", annotations=" + annotations +
                    ", meta=" + meta +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private String title;
            private String uri;
            private String description;
            private String mimeType;
            private Annotations annotations;
            private Long size;
            private Map<String, Object> meta;

            private Builder() {
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder uri(String uri) {
                this.uri = uri;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder mimeType(String mimeType) {
                this.mimeType = mimeType;
                return this;
            }

            public Builder annotations(Annotations annotations) {
                this.annotations = annotations;
                return this;
            }

            public Builder size(Long size) {
                this.size = size;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public ResourceLink build() {
                Assert.hasText(uri, "uri must not be empty");
                Assert.hasText(name, "name must not be empty");
                return new ResourceLink(name, title, uri, description, mimeType, size, annotations, meta);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextResourceContents implements ResourceContents {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("mimeType")
        private String mimeType;

        @JsonProperty("text")
        private String text;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        TextResourceContents() {
        }

        public TextResourceContents(String uri, String mimeType, String text) {
            this(uri, mimeType, text, null);
        }

        public TextResourceContents(String uri, String mimeType, String text, Map<String, Object> meta) {
            this.uri = uri;
            this.mimeType = mimeType;
            this.text = text;
            this.meta = meta;
        }

        @Override
        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public String mimeType() {
            return mimeType;
        }

        public void mimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String text() {
            return text;
        }

        public void text(String text) {
            this.text = text;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setMimeType(String mimeType) {
            this.mimeType(mimeType);
        }

        public String getText() {
            return this.text();
        }

        public void setText(String text) {
            this.text(text);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "TextResourceContents{" +
                    "uri='" + uri + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", text='" + text + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlobResourceContents implements ResourceContents {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("mimeType")
        private String mimeType;

        @JsonProperty("blob")
        private String blob;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        BlobResourceContents() {
        }

        public BlobResourceContents(String uri, String mimeType, String blob) {
            this(uri, mimeType, blob, null);
        }

        public BlobResourceContents(String uri, String mimeType, String blob, Map<String, Object> meta) {
            this.uri = uri;
            this.mimeType = mimeType;
            this.blob = blob;
            this.meta = meta;
        }

        @Override
        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public String mimeType() {
            return mimeType;
        }

        public void mimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String blob() {
            return blob;
        }

        public void blob(String blob) {
            this.blob = blob;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setMimeType(String mimeType) {
            this.mimeType(mimeType);
        }

        public String getBlob() {
            return this.blob();
        }

        public void setBlob(String blob) {
            this.blob(blob);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "BlobResourceContents{" +
                    "uri='" + uri + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", blob='" + blob + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Implementation implements BaseMetadata {
        @JsonProperty("name")
        private String name;

        @Nullable
        @JsonProperty("title")
        private String title;

        @JsonProperty("version")
        private String version;

        Implementation() {
        }

        public Implementation(String name, String version) {
            this(name, null, version);
        }

        public Implementation(String name, String title, String version) {
            this.name = name;
            this.title = title;
            this.version = version;
        }

        @Override
        public String name() {
            return name;
        }

        public void name(String name) {
            this.name = name;
        }

        @Override
        public String title() {
            return title;
        }

        public void title(String title) {
            this.title = title;
        }

        public String version() {
            return version;
        }

        public void version(String version) {
            this.version = version;
        }

        public void setName(String name) {
            this.name(name);
        }

        public void setTitle(String title) {
            this.title(title);
        }

        public String getVersion() {
            return this.version();
        }

        public void setVersion(String version) {
            this.version(version);
        }

        @Override
        public String toString() {
            return "Implementation{" +
                    "name='" + name + '\'' +
                    ", title='" + title + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientCapabilities implements Serializable {
        @JsonProperty("experimental")
        private Map<String, Object> experimental;

        @JsonProperty("roots")
        private RootCapabilities roots;

        @JsonProperty("sampling")
        private Sampling sampling;

        @JsonProperty("elicitation")
        private Elicitation elicitation;

        ClientCapabilities() {
        }

        public ClientCapabilities(Map<String, Object> experimental,
                                  RootCapabilities roots, Sampling sampling,
                                  Elicitation elicitation) {
            this.experimental = experimental;
            this.roots = roots;
            this.sampling = sampling;
            this.elicitation = elicitation;
        }

        public Elicitation elicitation() {
            return elicitation;
        }

        public void elicitation(Elicitation elicitation) {
            this.elicitation = elicitation;
        }

        public Sampling sampling() {
            return sampling;
        }

        public void sampling(Sampling sampling) {
            this.sampling = sampling;
        }

        public RootCapabilities roots() {
            return roots;
        }

        public void roots(RootCapabilities roots) {
            this.roots = roots;
        }

        public Map<String, Object> experimental() {
            return experimental;
        }

        public void experimental(Map<String, Object> experimental) {
            this.experimental = experimental;
        }

        public Map<String, Object> getExperimental() {
            return this.experimental();
        }

        public void setExperimental(Map<String, Object> experimental) {
            this.experimental(experimental);
        }

        public RootCapabilities getRoots() {
            return this.roots();
        }

        public void setRoots(RootCapabilities roots) {
            this.roots(roots);
        }

        public Sampling getSampling() {
            return this.sampling();
        }

        public void setSampling(Sampling sampling) {
            this.sampling(sampling);
        }

        public Elicitation getElicitation() {
            return this.elicitation();
        }

        public void setElicitation(Elicitation elicitation) {
            this.elicitation(elicitation);
        }

        @Override
        public String toString() {
            return "ClientCapabilities{" +
                    "experimental=" + experimental +
                    ", roots=" + roots +
                    ", sampling=" + sampling +
                    ", elicitation=" + elicitation +
                    '}';
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RootCapabilities implements Serializable {
            @JsonProperty("listChanged")
            private Boolean listChanged;

            public RootCapabilities(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean listChanged() {
                return listChanged;
            }

            public void listChanged(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean getListChanged() {
                return this.listChanged();
            }

            public void setListChanged(Boolean listChanged) {
                this.listChanged(listChanged);
            }

            @Override
            public String toString() {
                return "RootCapabilities{" +
                        "listChanged=" + listChanged +
                        '}';
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class Sampling implements Serializable {
            @Override
            public String toString() {
                return "Sampling{}";
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class Elicitation implements Serializable {
            @Override
            public String toString() {
                return "Elicitation{}";
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Map<String, Object> experimental;
            private RootCapabilities roots;
            private Sampling sampling;
            private Elicitation elicitation;

            public Builder experimental(Map<String, Object> experimental) {
                this.experimental = experimental;
                return this;
            }

            public Builder roots(Boolean listChanged) {
                this.roots = new RootCapabilities(listChanged);
                return this;
            }

            public Builder sampling() {
                this.sampling = new Sampling();
                return this;
            }

            public Builder elicitation() {
                this.elicitation = new Elicitation();
                return this;
            }

            public ClientCapabilities build() {
                return new ClientCapabilities(experimental, roots, sampling, elicitation);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServerCapabilities implements Serializable {
        @JsonProperty("completions")
        private CompletionCapabilities completions;

        @JsonProperty("experimental")
        private Map<String, Object> experimental;

        @JsonProperty("logging")
        private LoggingCapabilities logging;

        @JsonProperty("prompts")
        private PromptCapabilities prompts;

        @JsonProperty("resources")
        private ResourceCapabilities resources;

        @JsonProperty("tools")
        private ToolCapabilities tools;

        ServerCapabilities() {
        }

        public ServerCapabilities(CompletionCapabilities completions,
                                  Map<String, Object> experimental,
                                  LoggingCapabilities logging,
                                  PromptCapabilities prompts,
                                  ResourceCapabilities resources,
                                  ToolCapabilities tools) {
            this.completions = completions;
            this.experimental = experimental;
            this.logging = logging;
            this.prompts = prompts;
            this.resources = resources;
            this.tools = tools;
        }

        public CompletionCapabilities completions() {
            return completions;
        }

        public void completions(CompletionCapabilities completions) {
            this.completions = completions;
        }

        public Map<String, Object> experimental() {
            return experimental;
        }

        public void experimental(Map<String, Object> experimental) {
            this.experimental = experimental;
        }

        public LoggingCapabilities logging() {
            return logging;
        }

        public void logging(LoggingCapabilities logging) {
            this.logging = logging;
        }

        public PromptCapabilities prompts() {
            return prompts;
        }

        public void prompts(PromptCapabilities prompts) {
            this.prompts = prompts;
        }

        public ResourceCapabilities resources() {
            return resources;
        }

        public void resources(ResourceCapabilities resources) {
            this.resources = resources;
        }

        public ToolCapabilities tools() {
            return tools;
        }

        public void tools(ToolCapabilities tools) {
            this.tools = tools;
        }

        public ToolCapabilities getTools() {
            return this.tools();
        }

        public void setTools(ToolCapabilities tools) {
            this.tools(tools);
        }

        public ResourceCapabilities getResources() {
            return this.resources();
        }

        public void setResources(ResourceCapabilities resources) {
            this.resources(resources);
        }

        public PromptCapabilities getPrompts() {
            return this.prompts();
        }

        public void setPrompts(PromptCapabilities prompts) {
            this.prompts(prompts);
        }

        public LoggingCapabilities getLogging() {
            return this.logging();
        }

        public void setLogging(LoggingCapabilities logging) {
            this.logging(logging);
        }

        public Map<String, Object> getExperimental() {
            return this.experimental();
        }

        public void setExperimental(Map<String, Object> experimental) {
            this.experimental(experimental);
        }

        public CompletionCapabilities getCompletions() {
            return this.completions();
        }

        public void setCompletions(CompletionCapabilities completions) {
            this.completions(completions);
        }

        @Override
        public String toString() {
            return "ServerCapabilities{" +
                    "completions=" + completions +
                    ", experimental=" + experimental +
                    ", logging=" + logging +
                    ", prompts=" + prompts +
                    ", resources=" + resources +
                    ", tools=" + tools +
                    '}';
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class CompletionCapabilities implements Serializable {
            @Override
            public String toString() {
                return "CompletionCapabilities{}";
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class LoggingCapabilities implements Serializable {
            @Override
            public String toString() {
                return "LoggingCapabilities{}";
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class PromptCapabilities implements Serializable {
            @JsonProperty("listChanged")
            private Boolean listChanged;

            PromptCapabilities() {
            }

            public PromptCapabilities(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean listChanged() {
                return listChanged;
            }

            public void listChanged(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean getListChanged() {
                return this.listChanged();
            }

            public void setListChanged(Boolean listChanged) {
                this.listChanged(listChanged);
            }

            @Override
            public String toString() {
                return "PromptCapabilities{" +
                        "listChanged=" + listChanged +
                        '}';
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class ResourceCapabilities implements Serializable {
            @JsonProperty("subscribe")
            private Boolean subscribe;

            @JsonProperty("listChanged")
            private Boolean listChanged;

            ResourceCapabilities() {
            }

            public ResourceCapabilities(Boolean subscribe, Boolean listChanged) {
                this.subscribe = subscribe;
                this.listChanged = listChanged;
            }

            public Boolean subscribe() {
                return subscribe;
            }

            public void subscribe(Boolean subscribe) {
                this.subscribe = subscribe;
            }

            public Boolean listChanged() {
                return listChanged;
            }

            public void listChanged(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean getSubscribe() {
                return this.subscribe();
            }

            public void setSubscribe(Boolean subscribe) {
                this.subscribe(subscribe);
            }

            public Boolean getListChanged() {
                return this.listChanged();
            }

            public void setListChanged(Boolean listChanged) {
                this.listChanged(listChanged);
            }

            @Override
            public String toString() {
                return "ResourceCapabilities{" +
                        "subscribe=" + subscribe +
                        ", listChanged=" + listChanged +
                        '}';
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class ToolCapabilities implements Serializable {
            @JsonProperty("listChanged")
            private Boolean listChanged;

            ToolCapabilities() {
            }

            public ToolCapabilities(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean listChanged() {
                return listChanged;
            }

            public void listChanged(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean getListChanged() {
                return this.listChanged();
            }

            public void setListChanged(Boolean listChanged) {
                this.listChanged(listChanged);
            }

            @Override
            public String toString() {
                return "ToolCapabilities{" +
                        "listChanged=" + listChanged +
                        '}';
            }
        }

        public Builder mutate() {
            Builder builder = new Builder();
            builder.completions = this.completions;
            builder.experimental = this.experimental;
            builder.logging = this.logging;
            builder.prompts = this.prompts;
            builder.resources = this.resources;
            builder.tools = this.tools;
            return builder;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CompletionCapabilities completions;
            private Map<String, Object> experimental;
            private LoggingCapabilities logging;
            private PromptCapabilities prompts;
            private ResourceCapabilities resources;
            private ToolCapabilities tools;

            public Builder completions() {
                this.completions = new CompletionCapabilities();
                return this;
            }

            public Builder experimental(Map<String, Object> experimental) {
                this.experimental = experimental;
                return this;
            }

            public Builder logging() {
                this.logging = new LoggingCapabilities();
                return this;
            }

            public Builder prompts(Boolean listChanged) {
                this.prompts = new PromptCapabilities(listChanged);
                return this;
            }

            public Builder resources(Boolean subscribe, Boolean listChanged) {
                this.resources = new ResourceCapabilities(subscribe, listChanged);
                return this;
            }

            public Builder tools(Boolean listChanged) {
                this.tools = new ToolCapabilities(listChanged);
                return this;
            }

            public ServerCapabilities build() {
                return new ServerCapabilities(completions, experimental, logging, prompts, resources, tools);
            }
        }
    }

    /* Notification */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgressNotification implements Notification {
        @JsonProperty("progressToken")
        private String progressToken;

        @JsonProperty("progress")
        private Double progress;

        @JsonProperty("total")
        private Double total;

        @JsonProperty("message")
        private String message;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ProgressNotification() {
        }

        public ProgressNotification(String progressToken, Double progress, Double total, String message) {
            this(progressToken, progress, total, message, null);
        }

        public ProgressNotification(String progressToken, Double progress, Double total, String message, Map<String, Object> meta) {
            this.progressToken = progressToken;
            this.progress = progress;
            this.total = total;
            this.message = message;
            this.meta = meta;
        }

        public String progressToken() {
            return progressToken;
        }

        public void progressToken(String progressToken) {
            this.progressToken = progressToken;
        }

        public Double progress() {
            return progress;
        }

        public void progress(Double progress) {
            this.progress = progress;
        }

        public Double total() {
            return total;
        }

        public void total(Double total) {
            this.total = total;
        }

        public String message() {
            return message;
        }

        public void message(String message) {
            this.message = message;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getProgressToken() {
            return this.progressToken();
        }

        public void setProgressToken(String progressToken) {
            this.progressToken(progressToken);
        }

        public Double getProgress() {
            return this.progress();
        }

        public void setProgress(Double progress) {
            this.progress(progress);
        }

        public Double getTotal() {
            return this.total();
        }

        public void setTotal(Double total) {
            this.total(total);
        }

        public String getMessage() {
            return this.message();
        }

        public void setMessage(String message) {
            this.message(message);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ProgressNotification{" +
                    "progressToken='" + progressToken + '\'' +
                    ", progress=" + progress +
                    ", total=" + total +
                    ", message='" + message + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourcesUpdatedNotification implements Notification {
        @JsonProperty("uri")
        private String uri;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        ResourcesUpdatedNotification() {
        }

        public ResourcesUpdatedNotification(String uri) {
            this(uri, null);
        }

        public ResourcesUpdatedNotification(String uri, Map<String, Object> meta) {
            this.uri = uri;
            this.meta = meta;
        }

        public String uri() {
            return uri;
        }

        public void uri(String uri) {
            this.uri = uri;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public String getUri() {
            return this.uri();
        }

        public void setUri(String uri) {
            this.uri(uri);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "ResourcesUpdatedNotification{" +
                    "uri='" + uri + '\'' +
                    ", meta=" + meta +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoggingMessageNotification implements Notification {
        @JsonProperty("level")
        private LoggingLevel level;

        @JsonProperty("logger")
        private String logger;

        @JsonProperty("data")
        private String data;

        @Nullable
        @JsonProperty("_meta")
        private Map<String, Object> meta;

        LoggingMessageNotification() {
        }

        public LoggingMessageNotification(LoggingLevel level, String logger, String data) {
            this(level, logger, data, null);
        }

        public LoggingMessageNotification(LoggingLevel level, String logger, String data, Map<String, Object> meta) {
            this.level = level;
            this.logger = logger;
            this.data = data;
            this.meta = meta;
        }

        public LoggingLevel level() {
            return level;
        }

        public void level(LoggingLevel level) {
            this.level = level;
        }

        public String logger() {
            return logger;
        }

        public void logger(String logger) {
            this.logger = logger;
        }

        public String data() {
            return data;
        }

        public void data(String data) {
            this.data = data;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public void meta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public LoggingLevel getLevel() {
            return this.level();
        }

        public void setLevel(LoggingLevel level) {
            this.level(level);
        }

        public String getLogger() {
            return this.logger();
        }

        public void setLogger(String logger) {
            this.logger(logger);
        }

        public String getData() {
            return this.data();
        }

        public void setData(String data) {
            this.data(data);
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta(meta);
        }

        @Override
        public String toString() {
            return "LoggingMessageNotification{" +
                    "level=" + level +
                    ", logger='" + logger + '\'' +
                    ", data='" + data + '\'' +
                    ", meta=" + meta +
                    '}';
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private LoggingLevel level = LoggingLevel.INFO;
            private String logger = "server";
            private String data;
            private Map<String, Object> meta;

            public Builder level(LoggingLevel level) {
                this.level = level;
                return this;
            }

            public Builder logger(String logger) {
                this.logger = logger;
                return this;
            }

            public Builder data(String data) {
                this.data = data;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public LoggingMessageNotification build() {
                return new LoggingMessageNotification(level, logger, data, meta);
            }
        }
    }

    public static final class ErrorCodes {
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;

        private ErrorCodes() {
        }

        public static String getMethodNotFoundMessage(String method) {
            return "Method not found: " + method;
        }
    }
}
