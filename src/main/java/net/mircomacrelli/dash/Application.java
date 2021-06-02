package net.mircomacrelli.dash;

import java.io.*;
import java.nio.file.*;
import java.sql.*;

public final class Application {

    public static void main(String... args) throws IOException, SQLException {
        if (args.length != 2) {
            System.err.println("<configuration.yml> <destination directory>");
        }

        var builder = new DocsetBuilder(args[1]);
        var docset = builder.build(Paths.get(args[0]));
        var crawler = new Crawler(docset);
        crawler.download();
        var indexer = new Indexer(docset);
        indexer.indexPages();

        docset.createInfoPlist();
    }
}
