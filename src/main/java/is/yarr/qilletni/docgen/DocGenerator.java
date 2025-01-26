package is.yarr.qilletni.docgen;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.docgen.pages.dialects.function.FunctionSignatureAttributeTagProcessor;
import is.yarr.qilletni.docgen.pages.dialects.utility.TypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DocGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocGenerator.class);

    /**
     * Generates documentation for the given Qilletni project input path.
     *
     * @param outputPath  The path to output the generated documentation to
     * @param cachePath   The directory containing documentation cache
     * @param inputPath   The qilletni-src directory of the Qilletni project being documented
     * @param libraryName The name of the library to generate documentation for
     */
    public void generateDocs(Path outputPath, Path cachePath, Path inputPath, String libraryName) throws IOException {
        LOGGER.info("Generating docs for library: {}", libraryName);

        processLibrary(libraryName, inputPath, outputPath, cachePath).stream()
                .filter(docItem -> docItem instanceof DocumentedItem(DocumentedTypeFunction _, FunctionDoc _))
                .collect(Collectors.groupingBy(TypeUtility::getOnStatus)).forEach((onStatusInfo, items) -> {
                    var cachedLibraryName = onStatusInfo.libraryName();
                    
                    if (cachedLibraryName.isEmpty()) return;

                    LOGGER.info("  {}", cachedLibraryName);

                    var cachedLibrary = cachePath.resolve("%s.cache".formatted(cachedLibraryName));
                    if (Files.notExists(cachedLibrary)) {
                        LOGGER.debug("Skipping adding functions for library {} as it has no cache file", cachedLibraryName);
                        return;
                    }
                    
                    items.forEach(item -> {
                        var documentedFunction = (DocumentedTypeFunction) item.itemBeingDocumented();

                        LOGGER.info("    -{}", FunctionSignatureAttributeTagProcessor.getFunctionSignature(documentedFunction));
                    });

                    try {
                        processCachedLibrary(cachedLibraryName, outputPath, cachePath, items);
                    } catch (IOException e) {
                        LOGGER.error("Failed to process cached library: {}", cachedLibraryName, e);
                    }
                });
    }

    private static List<DocumentedItem> processLibrary(String libraryName, Path libraryPath, Path outputPath, Path cachePath) throws IOException {
        var docParser = DocParserFactory.createDocParser(libraryName, libraryPath, outputPath, cachePath);

        docParser.createIndexFile();
        docParser.createEntityFiles();
        docParser.writeToCache();
        docParser.createSearchIndex();

        return docParser.getOnExtensionDocs();
    }

    private static void processCachedLibrary(String libraryName, Path outputPath, Path cachePath, List<DocumentedItem> onExtensionsDocs) throws IOException {
        var docParserOptional = DocParserFactory.createDocParserFromCache(libraryName, outputPath, cachePath);
        if (docParserOptional.isEmpty()) {
            LOGGER.debug("No cached doc parser found for library: {}", libraryName);
            return;
        }
        
        var docParser = docParserOptional.get();

        docParser.createIndexFile();
        docParser.addExtendedFunctions(onExtensionsDocs);
        docParser.createEntityFiles();
        docParser.writeToCache();
    }
}
