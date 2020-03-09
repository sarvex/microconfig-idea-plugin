package io.microconfig.plugin.microconfig.impl;

import io.microconfig.factory.ConfigType;
import io.microconfig.factory.MicroconfigFactory;
import io.microconfig.plugin.microconfig.MicroconfigInitializer;

import java.io.File;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.core.properties.io.tree.CachedComponentTree.COMPONENTS_DIR;
import static io.microconfig.factory.MicroconfigFactory.ENV_DIR;
import static io.microconfig.factory.configtypes.CompositeConfigTypeProvider.compositeProvider;
import static io.microconfig.plugin.utils.FileUtil.findDir;
import static java.util.Arrays.stream;

public class MicroconfigInitializerImpl implements MicroconfigInitializer {
    @Override
    public MicroconfigFactory getMicroconfigFactory(File projectDir) {
        File configRoot = findConfigRootDir(projectDir);
        MicroconfigFactory factory = MicroconfigFactory.init(
                configRoot,
                new File(projectDir, "build/output"),
                new VirtualFileReader()
        );
        supportedConfigTypes(projectDir).forEach(factory::newConfigProvider);
        return factory;
    }

    @Override
    public ConfigType detectConfigType(File file, File projectDir) {
        String ext = getExtension(file);

        File configRoot = findConfigRootDir(projectDir);
        return supportedConfigTypes(configRoot)
                .filter(ct -> ct.getSourceExtensions().stream().anyMatch(e -> e.equals(ext)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find ConfigType for extension " + ext));
    }

    @Override
    public ConfigType configType(String configType, File projectDir) {
        File configDir = findConfigRootDir(projectDir);
        return supportedConfigTypes(configDir)
                .filter(t -> t.getType().equals(configType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported config type: " + configType));
    }

    @Override
    public File findConfigRootDir(File projectDir) {
        Predicate<File> containsMicroconfigDirs = dir -> {
            File[] files = dir.listFiles();
            return files != null && stream(files)
                    .filter(f -> f.getName().equals(ENV_DIR) || f.getName().equals(COMPONENTS_DIR))
                    .count() == 2;
        };

        return findDir(projectDir, containsMicroconfigDirs)
                .orElseThrow(() -> new IllegalStateException("Can't find 'components' and 'envs' folders on the same level"));
    }

    private Stream<ConfigType> supportedConfigTypes(File configRoot) {
        return compositeProvider().getConfigTypes(configRoot).stream();
    }

    private String getExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            throw new IllegalArgumentException("File " + file + " doesn't have an extension. Unable to resolve component type.");
        }
        return name.substring(lastDot);
    }
}
