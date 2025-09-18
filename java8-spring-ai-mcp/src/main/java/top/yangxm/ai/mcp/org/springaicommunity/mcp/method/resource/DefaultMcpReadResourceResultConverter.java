package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.resource;

import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.BlobResourceContents;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceContents;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.TextResourceContents;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.method.resource.AbstractMcpResourceMethodCallback.ContentType;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class DefaultMcpReadResourceResultConverter implements McpReadResourceResultConverter {
    private static final String DEFAULT_MIME_TYPE = "text/plain";

    @Override
    public ReadResourceResult convertToReadResourceResult(
            Object result, String requestUri,
            String mimeType, ContentType contentType) {
        if (result == null) {
            return new ReadResourceResult(Lists.of());
        }

        if (result instanceof ReadResourceResult) {
            return (ReadResourceResult) result;
        }

        mimeType = (mimeType != null && !mimeType.isEmpty()) ? mimeType : DEFAULT_MIME_TYPE;
        contentType = contentType != null ? contentType : isTextMimeType(mimeType) ? ContentType.TEXT : ContentType.BLOB;

        List<ResourceContents> contents;
        if (result instanceof List<?>) {
            contents = convertListResult((List<?>) result, requestUri, contentType, mimeType);
        } else if (result instanceof ResourceContents) {
            contents = Lists.of((ResourceContents) result);
        } else if (result instanceof String) {
            contents = convertStringResult((String) result, requestUri, contentType, mimeType);
        } else {
            throw new IllegalArgumentException("Unsupported return type: " + result.getClass().getName());
        }
        return new ReadResourceResult(contents);
    }

    private boolean isTextMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        if (mimeType.startsWith("text/")) {
            return true;
        }

        return mimeType.equals("application/json")
                || mimeType.equals("application/xml")
                || mimeType.equals("application/javascript")
                || mimeType.equals("application/ecmascript")
                || mimeType.equals("application/x-httpd-php")
                || mimeType.equals("application/xhtml+xml")
                || mimeType.endsWith("+json")
                || mimeType.endsWith("+xml");
    }

    @SuppressWarnings("unchecked")
    private List<ResourceContents> convertListResult(List<?> list, String requestUri,
                                                     ContentType contentType, String mimeType) {
        if (list.isEmpty()) {
            return Lists.of();
        }

        Object firstItem = list.get(0);

        if (firstItem instanceof ResourceContents) {
            return (List<ResourceContents>) list;
        } else if (firstItem instanceof String) {
            List<String> stringList = (List<String>) list;
            List<ResourceContents> result = new ArrayList<>(stringList.size());

            if (contentType == ContentType.TEXT) {
                for (String text : stringList) {
                    result.add(new TextResourceContents(requestUri, mimeType, text));
                }
            } else {
                for (String blob : stringList) {
                    result.add(new BlobResourceContents(requestUri, mimeType, blob));
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported list item type: " + firstItem.getClass().getName() + ". Expected String or ResourceContents."
            );
        }
    }

    private List<ResourceContents> convertStringResult(String stringResult, String requestUri,
                                                       ContentType contentType, String mimeType) {
        if (contentType == ContentType.TEXT) {
            return Lists.of(new TextResourceContents(requestUri, mimeType, stringResult));
        } else {
            return Lists.of(new BlobResourceContents(requestUri, mimeType, stringResult));
        }
    }
}
