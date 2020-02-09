package io.microconfig.plugin.actions.resolve;

import io.microconfig.plugin.actions.handler.ActionHandler;
import io.microconfig.plugin.microconfig.MicroconfigApi;
import io.microconfig.plugin.microconfig.PluginContext;
import io.microconfig.plugin.microconfig.server.MicroconfigServer;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static io.microconfig.plugin.actions.resolve.Hints.showHint;

@RequiredArgsConstructor
public class ResolvePlaceholder implements ActionHandler {
    private final String value;

    @Override
    public void onAction(PluginContext context, MicroconfigApi api) {
        if (value.startsWith("${VAULT")) {
            resolveVaultSecret(context);
        }  else {
            resolveFromLocal(context, api);
        }
    }

    private void resolveVaultSecret(PluginContext context) {
        MicroconfigServer server = new MicroconfigServer();
        server.resolveVaultSecret(context, value);
    }

    private void resolveFromLocal(PluginContext context, MicroconfigApi api) {
        Map<String, String> values = api.resolvePlaceholderForEachEnv(value, context.currentFile(), context.projectDir());
        showHint(value, values, context);
    }
}
