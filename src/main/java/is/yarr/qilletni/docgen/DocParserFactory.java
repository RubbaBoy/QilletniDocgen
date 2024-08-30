package is.yarr.qilletni.docgen;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedFile;
import is.yarr.qilletni.docgen.cache.CachedDocHandler;
import is.yarr.qilletni.lang.docs.DefaultDocumentationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

public class DocParserFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocParserFactory.class);

    public static Optional<DocParser> createDocParserFromCache(String libraryName, Path outputPath, Path cachePath) {
        var cachedDocParserHandler = new CachedDocHandler(outputPath, cachePath);
        return cachedDocParserHandler.getCachedLibrayDocParser(libraryName);
    }

    public static DocParser createDocParser(String libraryName, Path input, Path outputPath, Path cachePath) {
        try {
            if (Files.notExists(cachePath)) {
                Files.createDirectories(cachePath);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to create cache directory: {}", cachePath, e);
            throw new UncheckedIOException(e);
        }

        var cachedDocParserHandler = new CachedDocHandler(outputPath, cachePath);

        var documentedFiles = new ArrayList<DocumentedFile>();

        try (var walk = Files.walk(input)) {
            walk.forEach(file -> {
                if (Files.isDirectory(file) || !file.getFileName().toString().endsWith(".ql")) return;

                try {
                    LOGGER.debug("Parsing file: {}", file.getFileName());

                    var parser = new DefaultDocumentationParser(libraryName);

                    var documentedFile = parser.parseDocsFromPath(file, input.relativize(file).toString().replace("\\", "/"));
                    documentedFiles.add(documentedFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOGGER.debug("Documented files:");

        for (DocumentedFile documentedFile : documentedFiles) {
            System.out.println(documentedFile.fileName() + ":");
            System.out.println(documentedFile + "\n");
        }

        return DocParser.createInitializedParser(cachedDocParserHandler, libraryName, outputPath, documentedFiles);
    }
}
