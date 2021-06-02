package net.mircomacrelli.dash;

import org.yaml.snakeyaml.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static java.util.Objects.*;

final class DocsetBuilder {

    private String basePath;

    DocsetBuilder(String basePath) {
        this.basePath = requireNonNull(basePath);
    }

    private static Set<String> getSkip(Map<String,String> configuration) {
        var value = configuration.getOrDefault("skip", "");
        if (value.isBlank()) {
            return Collections.emptySet();
        }
        return Set.of(value.split("\s*,\s*"));
    }

    Docset build(Path configurationFile) throws IOException {
        try (var is = new FileInputStream(configurationFile.toFile())) {
            var yaml = new Yaml();
            Map<String,String> configuration = yaml.load(is);

            return new Docset(basePath,
                              configuration.get("family"),
                              configuration.get("identifier"),
                              configuration.get("name"),
                              configuration.get("baseUri"),
                              configuration.get("index"),
                              configuration.get("titleSelector"),
                              getSkip(configuration));
        }
    }
}
