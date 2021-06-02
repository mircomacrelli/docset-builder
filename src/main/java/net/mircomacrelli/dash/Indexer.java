package net.mircomacrelli.dash;

import org.jsoup.*;
import org.sqlite.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

import static java.util.stream.Collectors.*;

final class Indexer {

    private static final String CREATE_TABLE = "CREATE TABLE searchIndex(id INTEGER PRIMARY KEY, name TEXT, type TEXT, path TEXT);";

    private static final String CREATE_INDEX = "CREATE UNIQUE INDEX anchor ON searchIndex (name, type, path);";

    private static final String INSERT = "INSERT OR IGNORE INTO searchIndex(name, type, path) VALUES (?, 'Guide', ?);";

    private final Docset docset;

    private final SQLiteDataSource dataSource;

    Indexer(Docset docset) {
        this.docset = Objects.requireNonNull(docset);

        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + docset.resourcesDirectory().resolve("docSet.dsidx"));
    }

    private void createDatabase() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            try (var createTable = connection.prepareStatement(CREATE_TABLE)) {
                createTable.execute();
            }
            try (var createIndex = connection.prepareStatement(CREATE_INDEX)) {
                createIndex.execute();
            }
        }
    }

    private String getRelativePath(Path path) {
        return path.toString().substring(docset.documentsDirectory().toString().length());
    }

    private void insertPage(Path path, String title) throws SQLException {
        try (var connection = dataSource.getConnection();
             var insert = connection.prepareStatement(INSERT)) {
            insert.setString(1, title);
            var relativePath = getRelativePath(path);
            insert.setString(2, relativePath);
            insert.execute();
            System.out.println("INDEX: " + relativePath + " -> " + title);
        }
    }

    private void indexPage(Path path) throws IOException, SQLException {
        var doc = Jsoup.parse(path.toFile(), StandardCharsets.UTF_8.toString());
        var node = doc.selectFirst(docset.titleSelector());

        if (node == null) {
            System.err.println("NOT FOUND: " + docset.titleSelector() + " -> " + getRelativePath(path));
            return;
        }

        var title = node.text();
        if (title.isBlank()) {
            System.err.println("EMPTY: " + docset.titleSelector() + " -> " + getRelativePath(path));
            return;
        }

        insertPage(path, title);
    }

    public void indexPages() throws SQLException, IOException {
        createDatabase();

        try (var files = Files.walk(docset.documentsDirectory())) {
            var pages = files.filter(Files::isRegularFile)
                             .filter(Utils::isPage)
                             .filter(path -> !path.endsWith(docset.index()))
                             .collect(toList());
            for (var page : pages) {
                indexPage(page);
            }
        }
    }

}
