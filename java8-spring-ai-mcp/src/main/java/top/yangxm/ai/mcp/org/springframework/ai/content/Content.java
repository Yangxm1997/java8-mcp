package top.yangxm.ai.mcp.org.springframework.ai.content;

import java.util.Map;

@SuppressWarnings("unused")
public interface Content {
    String getText();

    Map<String, Object> getMetadata();
}
