package io.microconfig.plugin.microconfig;

import io.microconfig.commands.buildconfig.factory.MicroconfigFactory;

import java.io.File;

public interface MicroconfigInitializer {
    MicroconfigFactory getMicroconfigFactory(File projectDir);
}
