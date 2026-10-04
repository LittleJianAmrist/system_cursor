package com.amrist.systemcursor;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class SystemCursorConfig {

    private static final String CATEGORY_GENERAL = Configuration.CATEGORY_GENERAL;
    private static final String ALLOW_CURSOR_CHANGES = "Allow Cursor Changes";
    private static Configuration configuration;
    private static boolean allowCursorChanges = true;

    private SystemCursorConfig() {
    }

    public static void initialize(File file) {
        configuration = new Configuration(file);
        configuration.load();
        sync();
    }

    public static void sync() {
        if (configuration == null) {
            return;
        }

        allowCursorChanges = configuration.getBoolean(
                ALLOW_CURSOR_CHANGES,
                CATEGORY_GENERAL,
                true,
                "Enable or disable all cursor changes made by this mod."
        );
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static boolean isAllowCursorChanges() {
        return allowCursorChanges;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }
}
