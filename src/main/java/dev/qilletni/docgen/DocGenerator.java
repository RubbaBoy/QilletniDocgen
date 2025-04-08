package dev.qilletni.docgen;

import dev.qilletni.api.lang.docs.structure.DocumentedItem;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import dev.qilletni.api.lib.qll.QilletniInfoData;
import dev.qilletni.docgen.cache.BasicQllData;
import dev.qilletni.docgen.pages.GlobalIndexPageGenerator;
import dev.qilletni.docgen.pages.dialects.function.FunctionSignatureAttributeTagProcessor;
import dev.qilletni.docgen.pages.dialects.utility.TypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DocGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocGenerator.class);
    
    private final Path cachePath;
    private final Path outputPath;

    /**
     * Creates a {@link DocGenerator} that can generate documentation for libraries.
     * 
     * @param cachePath   The directory containing documentation cache
     * @param outputPath  The path to output the generated documentation to
     */
    public DocGenerator(Path cachePath, Path outputPath) {
        this.cachePath = cachePath;
        this.outputPath = outputPath;
    }

    /**
     * Generates documentation for the given Qilletni project input path.
     *
     * @param inputPath   The qilletni-src directory of the Qilletni project being documented
     * @param libraryQll  The library to generate documentation for
     */
    public void generateDocs(Path inputPath, QilletniInfoData libraryQll) throws IOException {
        LOGGER.info("Generating docs for library: {}", libraryQll.name());
        
        var basicQllData = new BasicQllData(libraryQll);

        processLibrary(basicQllData, inputPath, outputPath, cachePath).stream()
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
    
    public void regenerateGlobalIndex() throws IOException {
        var globalIndexPageGenerator = new GlobalIndexPageGenerator(cachePath, outputPath);
        globalIndexPageGenerator.generateIndex();
    }

    private static List<DocumentedItem> processLibrary(BasicQllData basicQllData, Path libraryPath, Path outputPath, Path cachePath) throws IOException {
        var docParser = DocParserFactory.createDocParser(basicQllData, libraryPath, outputPath, cachePath);

        docParser.createLibraryIndexPage();
        docParser.createLibraryFilesPage();
        docParser.createEntityPages();
        docParser.createSourceFilePages();
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

        docParser.createLibraryIndexPage();
        docParser.createLibraryFilesPage();
        docParser.addExtendedFunctions(onExtensionsDocs);
        docParser.createEntityPages();
        docParser.createSourceFilePages();
        docParser.writeToCache();
    }
}
