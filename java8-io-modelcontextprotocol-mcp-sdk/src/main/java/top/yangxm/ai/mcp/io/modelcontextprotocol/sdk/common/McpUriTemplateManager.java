package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common;

import top.yangxm.ai.mcp.commons.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public interface McpUriTemplateManager {
    List<String> getVariableNames();

    Map<String, String> extractVariableValues(String uri);

    boolean matches(String uri);

    boolean isUriTemplate(String uri);

    interface Factory {
        McpUriTemplateManager create(String uriTemplate);
    }

    Pattern DEFAULT_URI_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    Factory DEFAULT_FACTORY = uriTemplate -> {
        Assert.hasText(uriTemplate, "URI template must not be null or empty");
        return new McpUriTemplateManager() {
            @Override
            public List<String> getVariableNames() {
                List<String> variables = new ArrayList<>();
                Matcher matcher = DEFAULT_URI_VARIABLE_PATTERN.matcher(uriTemplate);

                while (matcher.find()) {
                    String variableName = matcher.group(1);
                    if (variables.contains(variableName)) {
                        throw new IllegalArgumentException("Duplicate URI variable name in template: " + variableName);
                    }
                    variables.add(variableName);
                }
                return variables;
            }

            @Override
            public Map<String, String> extractVariableValues(String uri) {
                Map<String, String> variableValues = new HashMap<>();
                List<String> uriVariables = this.getVariableNames();

                if (uri == null || uriVariables.isEmpty()) {
                    return variableValues;
                }

                try {
                    StringBuilder patternBuilder = new StringBuilder("^");

                    Matcher variableMatcher = DEFAULT_URI_VARIABLE_PATTERN.matcher(uriTemplate);
                    int lastEnd = 0;

                    while (variableMatcher.find()) {
                        String textBefore = uriTemplate.substring(lastEnd, variableMatcher.start());
                        patternBuilder.append(Pattern.quote(textBefore));
                        patternBuilder.append("([^/]+)");
                        lastEnd = variableMatcher.end();
                    }

                    if (lastEnd < uriTemplate.length()) {
                        patternBuilder.append(Pattern.quote(uriTemplate.substring(lastEnd)));
                    }
                    patternBuilder.append("$");

                    Pattern pattern = Pattern.compile(patternBuilder.toString());
                    Matcher matcher = pattern.matcher(uri);

                    if (matcher.find() && matcher.groupCount() == uriVariables.size()) {
                        for (int i = 0; i < uriVariables.size(); i++) {
                            String value = matcher.group(i + 1);
                            if (value == null || value.isEmpty()) {
                                throw new IllegalArgumentException("Empty value for URI variable '" + uriVariables.get(i) + "' in URI: " + uri);
                            }
                            variableValues.put(uriVariables.get(i), value);
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error parsing URI template: " + uriTemplate + " for URI: " + uri, e);
                }
                return variableValues;
            }

            @Override
            public boolean matches(String uri) {
                if (!this.isUriTemplate(uriTemplate)) {
                    return uri.equals(uriTemplate);
                }
                String regex = uriTemplate.replaceAll("\\{[^/]+?\\}", "([^/]+?)");
                regex = regex.replace("/", "\\/");
                return Pattern.compile(regex).matcher(uri).matches();
            }

            @Override
            public boolean isUriTemplate(String uri) {
                return DEFAULT_URI_VARIABLE_PATTERN.matcher(uri).find();
            }
        };
    };
}
