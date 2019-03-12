package io.microconfig.plugin.actions.resolve;

import io.microconfig.plugin.actions.common.PluginContext;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

class Hints {
    private static final String DEFAULT_ENV_KEY = "";

    public static void showHint(String line, Map<String, String> envToValue, PluginContext context) {
        if (envToValue.isEmpty()) {
            context.showErrorHint("Can't resolve '" + line + "'");
            return;
        }

        StringBuilder hint = new StringBuilder();
        hint.append(line)
                .append(LINES_SEPARATOR)
                .append(LINES_SEPARATOR);

        addDefaultValue(envToValue, hint);

        Map<String, List<String>> valueToEnv = groupByValue(envToValue);
        addValues(valueToEnv, hint);

        context.showInfoHint(hint.toString());
    }

    private static void addDefaultValue(Map<String, String> envToValue, StringBuilder hint) {
        if (!envToValue.containsKey(DEFAULT_ENV_KEY)) return;

        String value = envToValue.remove(DEFAULT_ENV_KEY);
        hint.append("Default: ")
                .append(value)
                .append(LINES_SEPARATOR)
                .append(LINES_SEPARATOR);

        envToValue.entrySet().removeIf(entry -> entry.getValue().equals(value));
    }

    private static Map<String, List<String>> groupByValue(Map<String, String> envToValue) {
        return envToValue
                .entrySet()
                .stream()
                .collect(groupingBy(Entry::getValue, TreeMap::new, mapping(Entry::getKey, toList())));
    }

    private static void addValues(Map<String, List<String>> valueToEnv, StringBuilder hint) {
        valueToEnv.forEach((v, envs)-> hint.append("Value: ")
                .append(v)
                .append(LINES_SEPARATOR)
                .append("For: ")
                .append(envs)
                .append(LINES_SEPARATOR)
                .append(LINES_SEPARATOR));
    }
}