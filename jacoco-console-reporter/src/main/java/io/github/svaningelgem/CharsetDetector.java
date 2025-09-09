package io.github.svaningelgem;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class CharsetDetector {
    static CharsetDetector instance = null;

    private CharsetDetector() {
    }

    public static CharsetDetector getInstance() {
        if (instance == null) {
            instance = new CharsetDetector();
        }
        return instance;
    }

    public interface Kernel32 extends Library {
        int GetConsoleOutputCP();
    }

    @Generated("not covered on linux")
    static class Kernel32Holder {
        static final Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
    }

    @Generated("not covered on linux")
    Kernel32 getKernel32Instance() {
        return Kernel32Holder.INSTANCE;
    }

    @NotNull String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

    int getConsoleCP() {
        return getKernel32Instance().GetConsoleOutputCP();
    }

    Charset getCharsetForCodePage(int cp) {
        if (cp == 65001) return StandardCharsets.UTF_8;

        try {
            return Charset.forName("CP" + cp);
        } catch (Exception ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    @NotNull Charset getCharset() {
        if (getOsName().contains("win")) {
            int cp = getConsoleCP();
            return getCharsetForCodePage(cp);
        }

        // on non-Windows, assume UTF-8 (most Linux/macOS terminals use UTF-8)
        return StandardCharsets.UTF_8;
    }
}