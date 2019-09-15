package me.vzhilin.bstreamer.util;

public final class Os {
    private final static String OS = System.getProperty("os.name").toLowerCase();

    private Os() { }

    public static boolean isWindows() {
        return OS.contains("win");
    }
}
