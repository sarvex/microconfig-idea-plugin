package io.microconfig.plugin.actions.placeholders;

import io.microconfig.plugin.actions.common.ActionHandler;
import io.microconfig.plugin.actions.common.PluginContext;
import io.microconfig.plugin.microconfig.MicroconfigApi;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static io.microconfig.plugin.actions.placeholders.PlaceholderUtils.printValues;

@RequiredArgsConstructor
public class ResolvePropertyLine implements ActionHandler {
    private final MicroconfigApi api;
    private final PluginContext context;

    @Override
    public void onAction() {
        String currentLine = context.currentLine();
        Map<String, String> values = api.resolvePropertyValueForEachEnv(currentLine, context.currentFile(), context.projectDir());
        printValues(currentLine, values, context);
    }
}