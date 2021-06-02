package net.mircomacrelli.dash;

import java.nio.file.*;
import java.util.regex.*;

final class Utils {
    private static final Pattern HTML_FILE = Pattern.compile("\\.html?$", Pattern.CASE_INSENSITIVE);

    public static boolean isPage(Path path) {
        return HTML_FILE.matcher(path.getFileName().toString()).find();
    }

    private Utils() {
        throw new AssertionError("do not create instances of this class");
    }

}
