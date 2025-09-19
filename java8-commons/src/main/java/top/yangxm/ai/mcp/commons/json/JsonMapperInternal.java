package top.yangxm.ai.mcp.commons.json;

import top.yangxm.ai.mcp.commons.util.InterfaceUtils;

final class JsonMapperInternal {
    private JsonMapperInternal() {
    }

    private static JsonMapper defaultJsonMapper = null;

    static JsonMapper getDefaultMapper() {
        if (defaultJsonMapper == null) {
            defaultJsonMapper = createDefaultMapper();
        }
        return defaultJsonMapper;
    }

    private static JsonMapper createDefaultMapper() {
        return InterfaceUtils.createDefaultInterface(JsonMapper.class, JsonMapperSupplier.class, null);
    }
}
