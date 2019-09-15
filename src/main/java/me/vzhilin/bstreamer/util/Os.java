package me.vzhilin.bstreamer.util;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

public final class Os {
    public final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    public final static String APP_PATH = getApplcatonPath();

    private Os() { }

    private static String getApplcatonPath() {
        CodeSource codeSource = Os.class.getProtectionDomain().getCodeSource();
        File rootPath = null;
        try {
            rootPath = new File(codeSource.getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return rootPath.getParentFile().getParentFile().getPath();
    }
}
