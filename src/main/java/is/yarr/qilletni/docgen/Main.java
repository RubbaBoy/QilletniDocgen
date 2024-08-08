package is.yarr.qilletni.docgen;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.docgen.pages.dialects.function.FunctionSignatureAttributeTagProcessor;
import is.yarr.qilletni.docgen.pages.dialects.utility.LinkFactory;
import is.yarr.qilletni.docgen.pages.dialects.utility.TypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        var cachePath = Paths.get("doc-cache");

        var allOnExtensionDocs = Stream.of(processLibrary("std", Paths.get("E:\\Qilletni\\qilletni-lib-std\\qilletni-src"), cachePath),
                        processLibrary("spotify", Paths.get("E:\\Qilletni\\qilletni-spotify\\qilletni-src"), cachePath),
                        processLibrary("demo", Paths.get("E:\\QilletniLibraries\\DemoLibrary\\qilletni-src"), cachePath))
                .flatMap(List::stream)
                .filter(docItem -> docItem instanceof DocumentedItem(
                        DocumentedTypeFunction $, FunctionDoc $$
                ))
                .toList();

        LOGGER.info("Extensions:");

//        allOnExtensionDocs.stream().collect(Collectors.groupingBy(documentedItem -> documentedItem.itemBeingDocumented().libraryName()))
//                .forEach((libraryName, items) -> {
//
//                    LOGGER.info("  {}", libraryName);
//
//                    items.stream().collect(Collectors.groupingBy(docItem -> {
//                                if (docItem.innerDoc() instanceof FunctionDoc func && func.docOnLine() != null && func.docOnLine().docFieldType() != null) {
//                                    return func.docOnLine().docFieldType().identifier();
//                                }
//
//                                return ((DocumentedTypeFunction) docItem.itemBeingDocumented()).onOptional().orElse("null");
//                            }))
//                            .forEach((importPath, items2) -> {
//                                LOGGER.info("    ({})", importPath);
//                                items2.forEach(item -> {
//                                    var documentedFunction = (DocumentedTypeFunction) item.itemBeingDocumented();
//
//                                    LOGGER.info("       {}", FunctionSignatureAttributeTagProcessor.getFunctionSignature(documentedFunction));
//                                });
//                            });
//                });

        allOnExtensionDocs.stream()
//                .filter(docItem -> !TypeUtility.isNativeType(((DocumentedTypeFunction) docItem.itemBeingDocumented()).onOptional().orElse(""))) // Ensure a non-native Entity on type
                .collect(Collectors.groupingBy(TypeUtility::getOnStatus)).forEach((onStatusInfo, items) -> {
                    var libraryName = onStatusInfo.libraryName();
                    System.out.println("libraryName = " + libraryName);
                    if (libraryName.isEmpty()) return;
                    
                    LOGGER.info("  {}", libraryName);
                    
                    var cachedLibrary = cachePath.resolve("%s.cache".formatted(libraryName));
                    if (Files.notExists(cachedLibrary)) {
                        LOGGER.debug("Skipping adding functions for library {} as it has no cache file", libraryName);
                        return;
                    }

//                    System.out.println("cachedLibrary = " + cachedLibrary);

                    items.forEach(item -> {
                        var documentedFunction = (DocumentedTypeFunction) item.itemBeingDocumented();

                        LOGGER.info("    {}", FunctionSignatureAttributeTagProcessor.getFunctionSignature(documentedFunction));
                    });

                    try {
                        processCachedLibrary(libraryName, cachePath, items);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static List<DocumentedItem> processLibrary(String libraryName, Path libraryPath, Path cachePath) throws IOException {
        var docParser = DocParserFactory.createDocParser(libraryName, libraryPath, cachePath);

        docParser.createIndexFile();
        docParser.createEntityFiles();
        docParser.writeToCache();

        return docParser.getOnExtensionDocs();
    }

    private static void processCachedLibrary(String libraryName, Path cachePath, List<DocumentedItem> onExtensionsDocs) throws IOException {
        var docParserOptional = DocParserFactory.createDocParserFromCache(libraryName, cachePath);
        if (docParserOptional.isEmpty()) {
            return;
        }

        var docParser = docParserOptional.get();

        docParser.createIndexFile();
        docParser.addExtendedFunctions(onExtensionsDocs);
        docParser.createEntityFiles();
        docParser.writeToCache();
    }
}
