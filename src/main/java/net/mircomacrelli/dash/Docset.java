package net.mircomacrelli.dash;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.regex.Pattern;

import static java.util.Objects.*;

final class Docset {
    private final Path basePath;

    private final String docsetFamily;
    private final String identifier;
    private final String name;

    private final URI baseUri;
    private final String index;

    private final String titleSelector;

    private final Pattern dontIndex;

    Docset(String basePath, String docsetFamily, String identifier, String name, String baseUri, String index, String titleSelector, String dontIndex) {
        this.basePath = Paths.get(requireNonNull(basePath));
        this.docsetFamily = requireNonNull(docsetFamily);
        this.identifier = requireNonNull(identifier);
        this.name = requireNonNull(name);
        this.baseUri = URI.create(requireNonNull(baseUri));
        this.index = requireNonNull(index);
        this.titleSelector = requireNonNull(titleSelector);
        this.dontIndex = dontIndex != null ? Pattern.compile(dontIndex) : null;
    }

    Path docsetDirectory() {
        return basePath.resolve(name + ".docset");
    }

    Path contentsDirectory() {
        return docsetDirectory().resolve("Contents");
    }

    Path resourcesDirectory() {
        return contentsDirectory().resolve("Resources");
    }

    Path documentsDirectory() {
        return resourcesDirectory().resolve("Documents");
    }

    Path resolvePath(Path path) {
        return documentsDirectory().resolve(path);
    }

    URI resolveURI(Path path) {
        return baseUri.resolve(path.toString());
    }

    URI baseUri() {
        return baseUri;
    }

    Path index() {
        return Paths.get(index);
    }

    public String titleSelector() {
        return titleSelector;
    }

    public boolean shouldIndex(Path fileName) {
        if (dontIndex != null) {
            return !dontIndex.matcher(fileName.toString()).find();
        }
        return true;
    }

    public void createInfoPlist() throws IOException {
        try (var os = new FileOutputStream(contentsDirectory().resolve("Info.plist").toFile())) {
            os.write("""
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
    <dict>
        <key>CFBundleIdentifier</key>
        <string>%s</string>
        <key>CFBundleName</key>
        <string>%s</string>
        <key>DocSetPlatformFamily</key>
        <string>%s</string>
        <key>isDashDocset</key>
        <true/>
        <key>dashIndexFilePath</key>
        <string>%s</string>
        <key>DashDocSetFallbackURL</key>
        <string>%s</string>
        <key>isJavaScriptEnabled</key>
        <true/>
    </dict>
</plist>""".formatted(identifier, name, docsetFamily, index, baseUri)
           .getBytes(StandardCharsets.UTF_8));

        }
    }
}
