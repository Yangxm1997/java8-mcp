package top.yangxm.ai.mcp.org.springframework.ai.chat.messages;

import top.yangxm.ai.mcp.org.springframework.ai.content.Content;

@SuppressWarnings("unused")
public interface Message extends Content {
    MessageType getMessageType();
}
