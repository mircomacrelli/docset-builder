package net.mircomacrelli.dash;

import org.jsoup.*;
import org.jsoup.nodes.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import static java.net.HttpURLConnection.*;
import static java.net.http.HttpClient.Redirect.*;
import static java.net.http.HttpClient.Version.*;
import static java.text.MessageFormat.*;
import static java.time.temporal.ChronoUnit.*;
import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static net.mircomacrelli.dash.Utils.*;

final class Crawler {

    private static final Pattern IMPORT = Pattern.compile("@import\\s*(?:url\\()?(?<quote>['\"])(?<file>[^\\\\1]+?)\\k<quote>\\)?;");

    private final Docset docset;

    private final Set<Path> downloaded;

    Crawler(Docset docset) {
        this.docset = requireNonNull(docset);
        downloaded = new HashSet<>(100);
    }

    private static String substringUntil(String s, char c) {
        var i = s.indexOf(c);
        return (i < 0) ? s : s.substring(0, i);
    }

    private static URI uriWithoutQueryStringOrComponent(String uri) {
        return URI.create(substringUntil(substringUntil(uri, '?'), '#'));
    }

    private static boolean isCss(Path path) {
        return path.getFileName().toString().endsWith(".css");
    }

    private boolean isInDocsetDirectory(URI uri) {
        var baseUri = docset.baseUri();
        return uri.getScheme().startsWith("http") &&
                (uri.getHost() != null) &&
                uri.getHost().equals(baseUri.getHost()) &&
                uri.getPath().startsWith(baseUri.getPath());
    }

    private Path toRelativePath(URI uri) {
        return Paths.get(uri.getPath().substring(docset.baseUri().getPath().length()));
    }

    private boolean isMissing(Path path) {
        return !downloaded.contains(path);
    }

    private void createParentDirectoryIfMissing(Path path) {
        var parent = path.getParent();
        if (!Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalStateException(format("Could not create the directory: " + parent, e));
            }
        }
    }

    private Stream<String> getUrlsFromAttributes(Document doc, String attributeName) {
        return doc.select('[' + attributeName + ']')
                  .stream()
                  .map(node -> node.absUrl(attributeName));
    }

    private Stream<String> getHrefs(Document doc) {
        return getUrlsFromAttributes(doc, "href");
    }

    private Stream<String> getSrcs(Document doc) {
        return getUrlsFromAttributes(doc, "src");
    }

    private Stream<String> getCssImports(Document doc) {
        return doc.getElementsByTag("style")
                  .stream()
                  .map(Element::html)
                  .flatMap(html -> IMPORT.matcher(html).results())
                  .map(match -> match.group(2))
                  .filter(uri -> !uri.startsWith("http"))
                  .map(uri -> docset.resolveURI(Paths.get(uri)))
                  .map(URI::toString);
    }

    private Set<Path> findMissingResources(Path path, Path file) {
        var uri = docset.resolveURI(path);
        try {
            var doc = Jsoup.parse(file.toFile(), StandardCharsets.UTF_8.toString(), uri.toString());
            return StreamUtils.concat(getSrcs(doc), getHrefs(doc), getCssImports(doc))
                              .map(Crawler::uriWithoutQueryStringOrComponent)
                              .filter(this::isInDocsetDirectory)
                              .map(this::toRelativePath)
                              .filter(this::isMissing)
                              .collect(toSet());
        } catch (IOException e) {
            System.err.println(format("Could not parse the file ''{0}'': {1}", file, e.getMessage()));
            return Collections.emptySet();
        }
    }

    private Set<Path> cssImports(Path path) throws IOException {
        try (var lines = Files.lines(docset.resolvePath(path))) {
            return lines.flatMap(line -> IMPORT.matcher(line).results())
                        .map(match -> match.group(2))
                        .filter(uri -> !uri.startsWith("http"))
                        .map(uri -> path.getParent().resolve(uri))
                        .collect(toSet());
        }
    }

    private void downloadMissingResources(Path page, Path file) throws IOException {
        var paths = findMissingResources(page, file);
        for (var missing : paths) {
            if (!downloaded.contains(missing)) {
                if (isPage(missing)) {
                    downloadPage(missing);
                } else {
                    downloadFile(missing);
                    if (isCss(missing)) {
                        var imports = cssImports(missing);
                        for (var css : imports) {
                            downloadFile(css);
                        }
                    }
                }
            }
        }
    }

    private Optional<Path> downloadFile(URI from, Path to) {
        if (from.getPath().endsWith("/")) {
            return Optional.empty();
        }
        var client =
                HttpClient.newBuilder()
                          .followRedirects(NEVER)
                          .connectTimeout(Duration.of(10, SECONDS))
                          .build();
        var request =
                HttpRequest.newBuilder()
                           .timeout(Duration.of(60, SECONDS))
                           .version(HTTP_1_1)
                           .GET()
                           .uri(from)
                           .build();
        try {
            var response = client.send(request, BodyHandlers.ofFile(to));
            if (response.statusCode() == HTTP_OK) {
                System.out.println("OK: " + from);
                return Optional.of(response.body());
            }
            System.err.println("NOT FOUND: " + from);
            try {
                Files.delete(to);
            } catch (IOException e) {
                System.err.println(format("Could not delete the temp file ''{0}'': {1}", to, e.getMessage()));
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(format("Could not download the URI ''{0}'': {1}", from, e.getMessage()));
        }
        return Optional.empty();
    }

    private Optional<Path> downloadFile(Path path) {
        var file = docset.resolvePath(path);
        createParentDirectoryIfMissing(file);
        var result = downloadFile(docset.resolveURI(path), file);
        downloaded.add(path);
        return result;
    }

    private void downloadPage(Path page) throws IOException {
        var file = downloadFile(page);
        if (file.isPresent()) {
            downloadMissingResources(page, file.get());
        }
    }

    public void download() throws IOException {
        downloadPage(docset.index());
    }
}
