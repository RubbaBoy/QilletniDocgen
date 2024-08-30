package is.yarr.qilletni.docgen.cache;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.docgen.DocParser;
import is.yarr.qilletni.docgen.cache.serializer.DocumentationDeserializer;
import is.yarr.qilletni.docgen.cache.serializer.DocumentationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CachedDocHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedDocHandler.class);

    private final Path outputPath;
    private final Path cachePath;
    // The key is the entity name, the value is a list of functions that reference the entity
    private final Map<String, List<ReferencedOnFunction>> entityFunctionReferences; 
    
    public CachedDocHandler(Path outputPath, Path cachePath) {
        this.outputPath = outputPath;
        this.cachePath = cachePath;
        this.entityFunctionReferences = new HashMap<>();
    }

    private static void silentlyDeleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {}
    }

    public Optional<DocParser> getCachedLibrayDocParser(String libraryName) {
        var libraryCacheOptional = getLibraryCache(libraryName);

        if (libraryCacheOptional.isEmpty()) {
            return Optional.empty();
        }

        var cache = libraryCacheOptional.get();

        LOGGER.debug("Using cache file: {}", cache.getFileName());

        try {
            return readDocParserFromCache(cache);
        } catch (Exception e) {
            LOGGER.error("Failed to read cache file: {}", cache.getFileName(), e);
            
            System.exit(-1);

            return Optional.empty();
        }
    }
    
    private Optional<DocParser> readDocParserFromCache(Path libraryCachePath) throws Exception {
        var libraryName = libraryCachePath.getFileName().toString().replaceAll("\\.cache$", "");
        
        try (var documentationDeserializer = new DocumentationDeserializer(Files.newInputStream(libraryCachePath))) {
            var documentedFiles = documentationDeserializer.deserializeDocumentedFileList();

            return Optional.of(DocParser.createInitializedParser(this, libraryName, outputPath, documentedFiles));
        }
    }

    public void writeLibraryCache(DocParser docParser) {
        writeLibraryCache(docParser, cachePath.resolve(docParser.getLibraryName() + ".cache"));
    }

    public void writeLibraryCache(DocParser docParser, Path cacheDestinationFile) {
        var libraryCacheOptional = getLibraryCache(docParser.getLibraryName());
        libraryCacheOptional.ifPresent(CachedDocHandler::silentlyDeleteIfExists);

        try (var outputStream = Files.newOutputStream(cacheDestinationFile);
             var documentationSerializer = new DocumentationSerializer(outputStream)) {
            documentationSerializer.serializeDocumentedFileList(docParser.getDocumentedFiles());

            LOGGER.debug("Wrote {} bytes to cache file: {}", documentationSerializer.getTotalWrittenBytes(), cacheDestinationFile.getFileName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeCache() {
        try (var cacheList = Files.list(cachePath)) {
            cacheList.filter(path -> path.getFileName().toString().endsWith(".cache"))
                    .forEach(cacheFile -> {
                        try {
                            readDocParserFromCache(cacheFile)
                                    .ifPresent(docParser -> docParser.getDocumentedFiles().forEach(documentedFile -> {
                                        for (var documentedItem : documentedFile.documentedItems()) {
                                            if (documentedItem instanceof DocumentedItem(DocumentedTypeFunction documentedFunctionItem, FunctionDoc functionDocItem)) {
                                                documentedFunctionItem.onOptional().ifPresent(onEntity -> {
                                                            var onFunction = new ReferencedOnFunction(docParser.getLibraryName(), documentedFile.fileName(), documentedItem);
                                                            entityFunctionReferences.computeIfAbsent(onEntity, k -> new ArrayList<>()).add(onFunction);
                                                        });
                                            }
                                        }
                                    }));
                        } catch (Exception e) {
                            LOGGER.error("Failed to read cache file: {}", cacheFile.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        LOGGER.debug("Initialized cache with {} entities", entityFunctionReferences.size());

        for (Map.Entry<String, List<ReferencedOnFunction>> stringListEntry : entityFunctionReferences.entrySet()) {
            LOGGER.debug("Entity: {}", stringListEntry.getKey());
            for (ReferencedOnFunction referencedOnFunction : stringListEntry.getValue()) {
                LOGGER.debug("  - {}", referencedOnFunction);
            }
        }
    }
    
    public List<ReferencedOnFunction> getOnFunctionsForEntity(String entityName) {
        return entityFunctionReferences.getOrDefault(entityName, Collections.emptyList());
    }

    private Optional<Path> getLibraryCache(String libraryName) {
        var resolved = cachePath.resolve(libraryName + ".cache");
        LOGGER.debug("Checking for cache file: {}", resolved);
        if (Files.exists(resolved)) {
            return Optional.of(resolved);
        }

        return Optional.empty();
    }

}
