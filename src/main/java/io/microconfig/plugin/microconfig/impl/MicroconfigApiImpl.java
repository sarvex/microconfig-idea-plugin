package io.microconfig.plugin.microconfig.impl;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.provider.Include;
import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.resolver.PropertyResolver;
import io.microconfig.core.properties.resolver.PropertyResolverHolder;
import io.microconfig.core.properties.resolver.placeholder.Placeholder;
import io.microconfig.core.properties.resolver.placeholder.PlaceholderResolver;
import io.microconfig.core.properties.sources.FileSource;
import io.microconfig.factory.ConfigType;
import io.microconfig.factory.MicroconfigFactory;
import io.microconfig.factory.configtypes.StandardConfigTypes;
import io.microconfig.plugin.microconfig.ConfigOutput;
import io.microconfig.plugin.microconfig.FilePosition;
import io.microconfig.plugin.microconfig.MicroconfigApi;
import io.microconfig.plugin.microconfig.MicroconfigInitializer;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static io.microconfig.core.environments.Component.bySourceFile;
import static io.microconfig.core.properties.Property.parse;
import static io.microconfig.core.properties.io.ioservice.selector.FileFormat.PROPERTIES;
import static io.microconfig.core.properties.io.ioservice.selector.FileFormat.YAML;
import static io.microconfig.core.properties.sources.FileSource.fileSource;
import static io.microconfig.factory.configtypes.StandardConfigTypes.APPLICATION;
import static io.microconfig.plugin.microconfig.FilePosition.positionFromFileSource;
import static java.lang.Math.max;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

public class MicroconfigApiImpl implements MicroconfigApi {
    private final MicroconfigInitializer initializer = new MicroconfigInitializerImpl();

    @Override
    public File findIncludeSource(String includeLine, int currentColumn, File currentFile, File projectDir) {
        Supplier<Include> parseInclude = () -> {
            List<Include> includes = Include.parse(includeLine, "");
            if (includes.size() == 1) return includes.get(0);

            int position = currentColumn;
            int componentIndex = -1;
            do {
                ++componentIndex;
                position = includeLine.lastIndexOf(',', max(0, position - 1));
            } while (position > 0);

            return includes.get(componentIndex);
        };

        Include include = parseInclude.get();
        return findSourceFile(include.getComponent(), include.getEnv(), currentFile, projectDir);
    }

    @Override
    public FilePosition findPlaceholderSource(String placeholderValue, File currentFile, File projectDir) {
        MicroconfigFactory factory = initializer.getMicroconfigFactory(projectDir);

        Supplier<Placeholder> parsePlaceholder = () -> {
            Placeholder p = Placeholder.parse(placeholderValue, detectEnvOr(currentFile, anyEnv(factory)));
            return p.isSelfReferenced() ? p.changeComponent(currentFile.getParentFile().getName()) : p;
        };

        Placeholder placeholder = parsePlaceholder.get();
        ConfigType configType = chooseConfigType(placeholder, currentFile, projectDir);
        PlaceholderResolver resolver = factory.newPlaceholderResolver(factory.newFileBasedProvider(configType), configType);
        Optional<Property> resolved = resolver.resolveToProperty(placeholder);

        if (resolved.isPresent() && resolved.get().getSource() instanceof FileSource) {
            return positionFromFileSource((FileSource) resolved.get().getSource());
        }

        return new FilePosition(findSourceFile(placeholder.getComponent(), detectEnvOr(currentFile, () -> ""), currentFile, projectDir), 0);
    }

    private ConfigType chooseConfigType(Placeholder placeholder, File currentFile, File projectDir) {
        Function<String, ConfigType> choosePlaceholderType = configType ->
                of(StandardConfigTypes.values())
                        .map(StandardConfigTypes::getType)
                        .filter(t -> t.getType().equals(configType))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unsupported config type: " + configType));

        return placeholder.getConfigType()
                .map(choosePlaceholderType)
                .orElseGet(() -> initializer.detectConfigType(currentFile, projectDir));
    }

    @Override
    public Map<String, String> resolvePlaceholderForEachEnv(String placeholderValue, File currentFile, File projectDir) {
        return resolveFullLineForEachEnv("key=" + placeholderValue, currentFile, projectDir);
    }

    @Override
    public Map<String, String> resolveFullLineForEachEnv(String currentLine, File currentFile, File projectDir) {
        ConfigType configType = initializer.detectConfigType(currentFile, projectDir);
        MicroconfigFactory factory = initializer.getMicroconfigFactory(projectDir);
        PropertyResolver propertyResolver = ((PropertyResolverHolder) factory.newConfigProvider(configType)).getResolver();

        Property property = parse(currentLine, "", fileSource(currentFile, 0, false));
        UnaryOperator<String> resolveProperty = env -> {
            try {
                Property p = property.withNewEnv(env);
                return propertyResolver.resolve(p, new EnvComponent(p.getSource().getComponent(), p.getEnvContext()));
            } catch (RuntimeException e) {
                return "ERROR";
            }
        };

        return envs(currentLine, currentFile, factory)
                .collect(toMap(identity(), resolveProperty, (k1, k2) -> k1, TreeMap::new));
    }

    @Override
    public File findAnyComponentFile(String component, String env, File projectDir) {
        try {
            return findFile(component, env, containsConfigTypeExtension(APPLICATION.getType()), projectDir);
        } catch (RuntimeException e) {
            return findFile(component, env, f -> true, projectDir);
        }
    }

    @Override
    public ConfigOutput buildConfigsForService(File currentFile, File projectDir, String env) {
        MicroconfigFactory factory = initializer.getMicroconfigFactory(projectDir);

        ConfigType configType = initializer.detectConfigType(currentFile, projectDir);
        Collection<Property> properties = factory
                .newConfigProvider(configType)
                .getProperties(bySourceFile(currentFile), env)
                .values();

        File resultFile = factory.getFilenameGenerator(configType)
                .fileFor(currentFile.getParentFile().getName(), env, properties);
        String output = factory
                .getConfigIoService()
                .writeTo(resultFile)
                .serialize(properties);
        return new ConfigOutput(resultFile.getName().endsWith(YAML.extension()) ? YAML : PROPERTIES, output);
    }

    @Override
    public MicroconfigInitializer getMicroconfigInitializer() {
        return initializer;
    }

    private Stream<String> envs(String currentLine, File currentFile, MicroconfigFactory factory) {
        if (!Placeholder.matcher(currentLine).find()) return of("");

        if (currentFile.getName().indexOf('.') != currentFile.getName().lastIndexOf('.')) {
            String[] parts = currentFile
                    .getName()
                    .split("\\.");

            return stream(parts)
                    .skip(1)
                    .limit(parts.length - 2);
        }

        return concat(of(""),
                factory.getEnvironmentProvider()
                        .getEnvironmentNames()
                        .stream()
        );
    }

    private File findSourceFile(String component, String env, File currentFile, File projectDir) {
        return findFile(component, env, containsConfigTypeExtension(initializer.detectConfigType(currentFile, projectDir)), projectDir);
    }

    private Predicate<File> containsConfigTypeExtension(ConfigType configType) {
        return file -> configType.getSourceExtensions()
                .stream()
                .anyMatch(ext -> file.getName().endsWith(ext));
    }

    private File findFile(String component, String env, Predicate<File> predicate, File projectDir) {
        Supplier<Comparator<File>> priorityByEnv = () -> {
            Comparator<File> comparator = comparing(f1 -> f1.getName().contains(env + ".") ? 0 : 1);
            return comparator.thenComparing(f -> f.getName().length());
        };

        return initializer.getMicroconfigFactory(projectDir)
                .getComponentTree()
                .getConfigFiles(component, predicate)
                .min(priorityByEnv.get())
                .orElseThrow(() -> new IllegalStateException("Component not found: " + component));
    }

    @Override
    public String detectEnvOr(File currentFile, Supplier<String> defaultEnv) {
        String name = currentFile.getName();
        int start = name.indexOf('.');
        int end = name.indexOf('.', start + 1);
        if (end > 0) {
            return name.substring(start + 1, end);
        }

        return defaultEnv.get();
    }

    private Supplier<String> anyEnv(MicroconfigFactory factory) {
        return () -> {
            return factory.getEnvironmentProvider()
                    .getEnvironmentNames()
                    .stream()
                    .findFirst()
                    .orElse(""); //otherwise will fail for env-specific prop
        };
    }

    @Override
    public Set<String> getEnvs(File projectDir) {
        return initializer.getMicroconfigFactory(projectDir)
                .getEnvironmentProvider()
                .getEnvironmentNames();
    }
}