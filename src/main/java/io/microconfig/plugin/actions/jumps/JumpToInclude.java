package io.microconfig.plugin.actions.jumps;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import io.microconfig.plugin.actions.common.ActionHandler;
import io.microconfig.plugin.actions.common.PluginContext;
import io.microconfig.plugin.microconfig.MicroconfigApi;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.plugin.utils.FileUtil.toPsiFile;
import static io.microconfig.plugin.utils.FileUtil.toVirtualFile;

@RequiredArgsConstructor
public class JumpToInclude implements ActionHandler {
    @Override
    public void onAction(PluginContext context, MicroconfigApi api) {
        File source = api.findIncludeSource(context.currentLine(), context.currentColumn(), context.currentFile(), context.projectDir());

        toPsiFile(context.getProject(), toVirtualFile(source))
                .navigate(true);
    }
}
