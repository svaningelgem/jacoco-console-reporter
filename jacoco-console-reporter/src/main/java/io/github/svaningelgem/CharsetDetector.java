package io.github.svaningelgem;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CharsetDetector {
    // JNA binding to the Win32 GetConsoleOutputCP API
    public interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int GetConsoleOutputCP();
    }

    public static @NotNull Charset run() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // ask the Windows console what code page it’s using
            int cp = Kernel32.INSTANCE.GetConsoleOutputCP();
            if (cp == 65001) return StandardCharsets.UTF_8;

            try {
                return Charset.forName("CP" + cp);
            } catch (Exception ignored) {
            }
        }

        // on non-Windows, assume UTF-8 (most Linux/macOS terminals use UTF-8)
        return StandardCharsets.UTF_8;
    }
}
