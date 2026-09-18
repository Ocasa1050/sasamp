package com.russia.launcher.storage;

import static com.russia.launcher.config.Config.NATIVE_SETTINGS_FILE_PATH;

import android.content.Context;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class NativeStorage {

    private static final String CLIENT_SECTION_NAME = "client";
    private static final Object LOCK = new Object();

    public static void addClientProperty(String propertyName, String value, Context context) {
        synchronized (LOCK) {
            try {
                File settingsFile = getSettingsFile(context);
                File parent = settingsFile.getParentFile();

                if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
                    throw new IOException("Unable to create settings directory: " + parent);
                }

                // The launcher can save the nickname before the native game has
                // created settings.ini. Create it instead of silently dropping
                // the user's first nickname.
                if (!settingsFile.exists() && !settingsFile.createNewFile()) {
                    throw new IOException("Unable to create settings file: " + settingsFile);
                }

                Wini settings = new Wini(settingsFile);
                ensureNativeDefaults(settings);
                settings.put(CLIENT_SECTION_NAME, propertyName, value);
                settings.store();
            } catch (IOException e) {
                throw new RuntimeException("Unable to save native settings", e);
            }
        }
    }

    public static String getClientProperty(String property, Context context) {
        synchronized (LOCK) {
            File settingsFile = getSettingsFile(context);
            if (!settingsFile.exists()) {
                return null;
            }

            try {
                Wini settings = new Wini(settingsFile);
                return settings.get(CLIENT_SECTION_NAME, property);
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    private static File getSettingsFile(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            throw new IllegalStateException("External files directory is unavailable");
        }

        String relativePath = NATIVE_SETTINGS_FILE_PATH.startsWith("/")
                ? NATIVE_SETTINGS_FILE_PATH.substring(1)
                : NATIVE_SETTINGS_FILE_PATH;
        return new File(externalFilesDir, relativePath);
    }

    private static void ensureNativeDefaults(Wini settings) {
        putIfMissing(settings, "client", "name", "");
        putIfMissing(settings, "client", "ip", "0.0.0.0");
        putIfMissing(settings, "client", "port", "7777");
        putIfMissing(settings, "client", "password", "");
        putIfMissing(settings, "client", "player_password", "");
        putIfMissing(settings, "client", "autologin", "0");
        putIfMissing(settings, "client", "server", "0");
        putIfMissing(settings, "client", "debug", "0");
        putIfMissing(settings, "client", "headmove", "0");
        putIfMissing(settings, "client", "dl", "0");
        putIfMissing(settings, "client", "timestamp", "0");
        putIfMissing(settings, "client", "test", "0");

        putIfMissing(settings, "gui", "Font", "visby-round-cf-extra-bold.ttf");
        putIfMissing(settings, "gui", "FontSize", "30.0");
        putIfMissing(settings, "gui", "FontOutline", "2");
        putIfMissing(settings, "gui", "fps", "60");
        putIfMissing(settings, "gui", "ChatFontSize", "-1");
        putIfMissing(settings, "gui", "ChatMaxMessages", "-1");
        putIfMissing(settings, "gui", "androidKeyboard", "0");
        putIfMissing(settings, "gui", "outfit", "1");
        putIfMissing(settings, "gui", "hparmourtext", "0");
        putIfMissing(settings, "gui", "damageinformer", "1");
        putIfMissing(settings, "gui", "text3dinveh", "1");
    }

    private static void putIfMissing(Wini settings, String section, String property, String value) {
        if (settings.get(section, property) == null) {
            settings.put(section, property, value);
        }
    }
}
