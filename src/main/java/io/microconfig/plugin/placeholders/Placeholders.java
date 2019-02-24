package io.microconfig.plugin.placeholders;

import io.microconfig.plugin.MicroconfigComponent;
import io.microconfig.plugin.PluginContext;
import io.microconfig.plugin.microconfig.MicroconfigApi;
import io.microconfig.plugin.microconfig.MicroconfigApiImpl;

import java.util.Optional;

import static io.microconfig.plugin.utils.ContextUtils.currentLine;
import static io.microconfig.plugin.utils.PlaceholderUtils.hasPlaceholder;
import static io.microconfig.plugin.utils.PlaceholderUtils.insidePlaceholderBrackets;
import static java.util.Optional.empty;

public class Placeholders {
    private final MicroconfigApi api = new MicroconfigApiImpl();

    public Optional<MicroconfigComponent> componentFrom(PluginContext context) {
        String currentLine = currentLine(context);
        if (!hasPlaceholder(currentLine)) return empty();

        if (insidePlaceholderBrackets(currentLine, context.caret.getLogicalPosition().column)) {
            return Optional.of(new ResolvePlaceholderValue(api, context, currentLine));
        }

        return Optional.of(new ResolvePlaceholderLine(api, context, currentLine));
    }
}
