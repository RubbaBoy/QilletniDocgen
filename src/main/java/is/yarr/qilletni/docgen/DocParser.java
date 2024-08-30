package is.yarr.qilletni.docgen;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedFile;
import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.DocDescription;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.docgen.cache.CachedDocHandler;
import is.yarr.qilletni.docgen.pages.dialects.constructor.ConstructorDialect;
import is.yarr.qilletni.docgen.pages.dialects.description.FormattedDocDialect;
import is.yarr.qilletni.docgen.pages.dialects.entity.EntityDialect;
import is.yarr.qilletni.docgen.pages.dialects.function.FunctionDialect;
import is.yarr.qilletni.docgen.pages.dialects.function.FunctionSignatureAttributeTagProcessor;
import is.yarr.qilletni.docgen.pages.dialects.utility.TypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Creates weg pages for a single library.
 * The overall index page is displayed differently
 */
public class DocParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocParser.class);

    private final CachedDocHandler cachedDocHandler;
    private final String libraryName;
    private final boolean isStd;

    private final Path outputPath;
    private final List<DocumentedFile> documentedFiles;
    private final List<DocumentedItem> entityDocs;
    private final List<DocumentedItem> functionDocs;
    private final List<DocumentedItem> fieldDocs;
    private final Map<String, List<DocumentedItem>> entityConstructors;
    
    private final List<DocumentedItem> onExtensionDocs;

    public DocParser(CachedDocHandler cachedDocHandler, String libraryName, Path outputPath, List<DocumentedFile> documentedFiles) {
        this.cachedDocHandler = cachedDocHandler;
        this.libraryName = libraryName;
        this.isStd = libraryName.equals("std");
        this.outputPath = outputPath;
        this.documentedFiles = documentedFiles;
        this.entityDocs = new ArrayList<>();
        this.functionDocs = new ArrayList<>();
        this.fieldDocs = new ArrayList<>();
        this.entityConstructors = new HashMap<>();
        this.onExtensionDocs = new ArrayList<>();
    }
    
    public static DocParser createInitializedParser(CachedDocHandler cachedDocHandler, String libraryName, Path outputPath, List<DocumentedFile> documentedFiles) {
        var docParser = new DocParser(cachedDocHandler, libraryName, outputPath, documentedFiles);
        docParser.initDocumentedItems();
        
        return docParser;
    }
    
    public Path getBasePath() {
        return Paths.get("library", libraryName);
    }

    public String getLibraryName() {
        return libraryName;
    }

    public List<DocumentedFile> getDocumentedFiles() {
        return documentedFiles;
    }

    private TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.addDialect(new FunctionDialect());
        templateEngine.addDialect(new EntityDialect(libraryName));
        templateEngine.addDialect(new FormattedDocDialect());
        templateEngine.addDialect(new ConstructorDialect());
        
        return templateEngine;
    }
    
    private void processAndWrite(String templatePath, Path outputPath, TemplateEngine templateEngine, Context context) throws IOException {
        String output = templateEngine.process(templatePath, context);
        
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            writer.write(output);
        }
    }
    
    public void createIndexFile() throws IOException {
//        initDocumentedItems();
        
        var context = new Context();
        context.setVariable("libraryName", libraryName);
        context.setVariable("entityDocs", entityDocs);
        context.setVariable("functionDocs", functionDocs);
        context.setVariable("fieldDocs", fieldDocs);
        context.setVariable("onExtensionDocs", onExtensionDocs);

        var templateEngine = createTemplateEngine();

        var outputDir = Files.createDirectories(outputPath.resolve(getBasePath()));

        processAndWrite("templates/library.html", outputDir.resolve("index.html"), templateEngine, context);
    }

    public void createEntityFiles() throws IOException {
        var outputDir = Files.createDirectories(outputPath.resolve(getBasePath())).resolve("entity");
        Files.createDirectories(outputDir);

        for (DocumentedItem documentedItem : entityDocs) {
            var documentedType = (DocumentedTypeEntity) documentedItem.itemBeingDocumented();
            var entityDoc = (EntityDoc) documentedItem.innerDoc();
            
            var path = outputDir.resolve("%s.html".formatted(documentedType.name()));
            
            if (documentedType.name().contains("string")) {
                LOGGER.debug("STRING");
                LOGGER.debug("entityDoc = {}", entityDoc);
            }
            
            var context = new Context();
            context.setVariable("libraryName", libraryName);
            context.setVariable("fileName", documentedType.importPath());
            context.setVariable("name", documentedType.name());
            context.setVariable("description", entityDoc.description());
            context.setVariable("currentPath", path.toString());

            var entityFields = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeField).toList();
            var entityFunctions = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeFunction).toList();
            var entityExtensionFunctions = entityDoc.onExtensionFunctions(); // always only functions
            var entityConstructors = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeEntityConstructor).toList();

            context.setVariable("fields", entityFields);
            context.setVariable("functions", entityFunctions);
            context.setVariable("extensionFunctions", entityExtensionFunctions);
            context.setVariable("constructors", entityConstructors);
            
            context.setVariable("allFunctions", Stream.of(entityFunctions, entityExtensionFunctions).flatMap(List::stream).toList());

            var templateEngine = createTemplateEngine();

            processAndWrite("templates/entity.html", path, templateEngine, context);
        }
    }

    public void addExtendedFunctions(List<DocumentedItem> addingOnExtensionsDocs) {
        for (DocumentedItem addingDocItem : addingOnExtensionsDocs) {
            
            var functionDoc = (FunctionDoc) addingDocItem.innerDoc();
            var documentedTypeFunction = (DocumentedTypeFunction) addingDocItem.itemBeingDocumented();

            var onStatusInfo = TypeUtility.getOnStatus(addingDocItem);

            if (onStatusInfo.onStatus() == TypeUtility.OnStatus.NONE) { // Should never happen, but just to check
                LOGGER.warn("Got an on status of NONE, something bad is being included in addExtensionFunctions");
                continue;
            }
            
            var entityDocOptional = entityDocs.stream().filter(docItem -> {
                var documentedTypeEntity = (DocumentedTypeEntity) docItem.itemBeingDocumented();
                return documentedTypeEntity.name().equals(onStatusInfo.entityName());
            }).findFirst();
            
            if (entityDocOptional.isEmpty()) {
                LOGGER.debug("Could not find entity for function: {}", FunctionSignatureAttributeTagProcessor.getFunctionSignature(documentedTypeFunction));
                continue;
            }
            
            var entityDoc = (EntityDoc) entityDocOptional.get().innerDoc();

            if (entityDoc.onExtensionFunctions().contains(addingDocItem)) {
                LOGGER.debug("Found duplicate in {}:  {}", onStatusInfo.entityName(), FunctionSignatureAttributeTagProcessor.getFunctionSignature(documentedTypeFunction));
                continue;
            }
            
            entityDoc.addOnExtension(addingDocItem);
        }
    }
    
    private void initDocumentedItems() {
        documentedFiles.stream()
                .filter(documentedFile -> documentedFile.documentedItems() != null)
                .flatMap(documentedFile -> documentedFile.documentedItems().stream()).forEach(documentedItem -> {
//                    System.out.println("documentedItem = " + documentedItem);
                });
        
        documentedFiles.stream()
                .filter(documentedFile -> documentedFile.documentedItems() != null)
                .flatMap(documentedFile -> documentedFile.documentedItems().stream())
                .forEach(documentedItem -> {
                    switch (documentedItem.innerDoc()) {
                        case FieldDoc $ -> fieldDocs.add(documentedItem);
                        case EntityDoc $ -> entityDocs.add(documentedItem);
                        case FunctionDoc $ -> {
                            var documentedTypeFunction = (DocumentedTypeFunction) documentedItem.itemBeingDocumented();
                            if (documentedTypeFunction.onOptional().isPresent()) {
                                onExtensionDocs.add(documentedItem);
                            } else {
                                functionDocs.add(documentedItem);
                            }
                        }
                        case ConstructorDoc $ -> { // Ignored, there will be none on a file-level (only entity level)
//                            System.out.println("got const doc " + documentedItem.itemBeingDocumented());
//                            entityConstructors.merge(((DocumentedTypeEntity) documentedItem.itemBeingDocumented()).name(), List.of(documentedItem), (a, b) -> {
//                                var newList = new ArrayList<>(a);
//                                newList.addAll(b);
//                                return newList;
//                            });
                        }
                    }
                });

        onExtensionDocs.sort((a, b) -> {
            return Comparator.comparing((DocumentedItem item) -> ((DocumentedTypeFunction) item.itemBeingDocumented()).onOptional().orElseThrow())
                    .thenComparing(item -> ((DocumentedTypeFunction) item.itemBeingDocumented()).name())
                    .compare(a, b);
            
//            var aDocumentedTypeFunction = ((DocumentedTypeFunction) a.itemBeingDocumented()).onOptional().orElseThrow();
//            var bDocumentedTypeFunction = ((DocumentedTypeFunction) b.itemBeingDocumented()).onOptional().orElseThrow();
//            
//            return aDocumentedTypeFunction.compareTo(bDocumentedTypeFunction);
        });
        
        functionDocs.sort((a, b) -> Comparator.comparing((DocumentedItem item) -> ((DocumentedTypeFunction) item.itemBeingDocumented()).onOptional().isPresent())
                .thenComparing(item -> ((DocumentedTypeFunction) item.itemBeingDocumented()).name())
                .compare(a, b));

        if (isStd) {
            TypeUtility.NATIVE_QILLETNI_TYPES.forEach(type -> {
                var entityDoc = new EntityDoc(new DocDescription(List.of(new DocDescription.DocText("This is a native Qilletni type. TODO: Add native type examples/descriptions")))); // TODO: Add native type examples/descriptions
                entityDocs.add(new DocumentedItem(new DocumentedTypeEntity(libraryName, "std", type), entityDoc));
            });
        }

        entityDocs.sort((a, b) -> {
            DocumentedTypeEntity entityA = (DocumentedTypeEntity) a.itemBeingDocumented();
            DocumentedTypeEntity entityB = (DocumentedTypeEntity) b.itemBeingDocumented();

            boolean isANative = TypeUtility.NATIVE_QILLETNI_TYPES.contains(entityA.name());
            boolean isBNative = TypeUtility.NATIVE_QILLETNI_TYPES.contains(entityB.name());

            if (isANative && isBNative) {
                return Integer.compare(TypeUtility.NATIVE_QILLETNI_TYPES.indexOf(entityA.name()), TypeUtility.NATIVE_QILLETNI_TYPES.indexOf(entityB.name()));
            } else if (isANative) {
                return -1;
            } else if (isBNative) {
                return 1;
            } else {
                return entityA.name().compareTo(entityB.name());
            }
        });

        addExtendedFunctions(onExtensionDocs);
    }

    public void writeToCache() {
        cachedDocHandler.writeLibraryCache(this);
    }

    public List<DocumentedItem> getOnExtensionDocs() {
        return onExtensionDocs;
    }
}
