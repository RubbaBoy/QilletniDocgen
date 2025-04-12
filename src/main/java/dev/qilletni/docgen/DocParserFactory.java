package dev.qilletni.docgen;

import dev.qilletni.api.lang.docs.structure.DocumentedFile;
import dev.qilletni.docgen.cache.BasicQllData;
import dev.qilletni.docgen.cache.CachedDocHandler;
import dev.qilletni.impl.lang.docs.DefaultDocumentationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static DocParser createDocParser(BasicQllData basicQllData, Path input, Path outputPath, Path cachePath) {
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

                    var parser = new DefaultDocumentationParser(basicQllData.name());

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
            LOGGER.debug("{}:", documentedFile.fileName());
            LOGGER.debug("{}\n", documentedFile);
        }

        return DocParser.createInitializedParser(cachedDocParserHandler, basicQllData, outputPath, documentedFiles);
    }
}
