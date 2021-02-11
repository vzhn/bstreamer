package me.vzhilin.bstreamer.util;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

public final class AppRuntime {
    public final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    public final static boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    public final static File APP_PATH = getApplcatonPath();

    private AppRuntime() { }

    private static File getApplcatonPath() {
        CodeSource codeSource = AppRuntime.class.getProtectionDomain().getCodeSource();

        try {
            File rootPath = new File(codeSource.getLocation().toURI().getPath());
            return rootPath.getParentFile().getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
