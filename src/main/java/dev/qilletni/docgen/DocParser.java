package dev.qilletni.docgen;

import dev.qilletni.api.lang.docs.structure.DocumentedFile;
import dev.qilletni.api.lang.docs.structure.DocumentedItem;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.api.lang.docs.structure.text.DocDescription;
import dev.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import dev.qilletni.docgen.cache.BasicQllData;
import dev.qilletni.docgen.cache.CachedDocHandler;
import dev.qilletni.docgen.index.DescriptionFormatter;
import dev.qilletni.docgen.index.SearchIndexGenerator;
import dev.qilletni.docgen.pages.dialects.constructor.ConstructorDialect;
import dev.qilletni.docgen.pages.dialects.description.FormattedDocDialect;
import dev.qilletni.docgen.pages.dialects.entity.EntityDialect;
import dev.qilletni.docgen.pages.dialects.field.FieldDialect;
import dev.qilletni.docgen.pages.dialects.file.FileDialect;
import dev.qilletni.docgen.pages.dialects.function.FunctionDialect;
import dev.qilletni.docgen.pages.dialects.function.FunctionSignatureAttributeTagProcessor;
import dev.qilletni.docgen.pages.dialects.link.LinkDialect;
import dev.qilletni.docgen.pages.dialects.utility.AnchorFactory;
import dev.qilletni.docgen.pages.dialects.utility.TypeUtility;
import dev.qilletni.docgen.pages.filetree.FileNode;
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
    private final BasicQllData basicQllData;
    private final boolean isStd;

    private final Path outputPath;
    private final List<DocumentedFile> documentedFiles;
    private final List<DocumentedItem> entityDocs;
    private final List<DocumentedItem> functionDocs;
    private final List<DocumentedItem> fieldDocs;
    private final Map<String, List<DocumentedItem>> entityConstructors;
    private final DescriptionFormatter descriptionFormatter;
    
    private final List<DocumentedItem> onExtensionDocs;
    private final Path fullIndexPath;
    private final String relativeIndexPath;

    public DocParser(CachedDocHandler cachedDocHandler, BasicQllData basicQllData, Path outputPath, List<DocumentedFile> documentedFiles) {
        this.cachedDocHandler = cachedDocHandler;
        this.libraryName = basicQllData.name();
        this.basicQllData = basicQllData;
        this.isStd = libraryName.equals("std");
        this.outputPath = outputPath;
        this.documentedFiles = documentedFiles;
        this.entityDocs = new ArrayList<>();
        this.functionDocs = new ArrayList<>();
        this.fieldDocs = new ArrayList<>();
        this.entityConstructors = new HashMap<>();
        this.onExtensionDocs = new ArrayList<>();
        this.descriptionFormatter = new DescriptionFormatter();
        
        this.fullIndexPath = outputPath.resolve(getBasePath()).resolve("index.json");
        this.relativeIndexPath = "/" + getBasePath().resolve("index.json").toString().replace("\\", "/");
    }
    
    public static DocParser createInitializedParser(CachedDocHandler cachedDocHandler, BasicQllData basicQllData, Path outputPath, List<DocumentedFile> documentedFiles) {
        var docParser = new DocParser(cachedDocHandler, basicQllData, outputPath, documentedFiles);
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
        templateEngine.addDialect(new FieldDialect());
        templateEngine.addDialect(new EntityDialect(libraryName));
        templateEngine.addDialect(new FormattedDocDialect());
        templateEngine.addDialect(new ConstructorDialect());
        templateEngine.addDialect(new FileDialect(libraryName));
        templateEngine.addDialect(new LinkDialect());
        
        return templateEngine;
    }
    
    private void processAndWrite(String templatePath, Path outputPath, TemplateEngine templateEngine, Context context) throws IOException {
        String output = templateEngine.process(templatePath, context);
        
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            writer.write(output);
        }
    }
    
    public void createLibraryFilesPage() throws IOException {
        var context = new Context();
        context.setVariable("libraryName", libraryName);
        context.setVariable("library", basicQllData);

        var fileList = documentedFiles.stream().map(DocumentedFile::importPath).toList();

        List<FileNode> fileTree = buildFileTree(fileList);
        context.setVariable("fileTree", fileTree);

        var templateEngine = createTemplateEngine();

        var outputDir = Files.createDirectories(outputPath.resolve(getBasePath()));

        processAndWrite("templates/file_tree.html", outputDir.resolve("files.html"), templateEngine, context);
    }

    private List<FileNode> buildFileTree(List<Path> filePaths) {
        // Create a map to hold folder nodes by their path string.
        var roots = new ArrayList<FileNode>();

        for (Path path : filePaths) {
            var parts = path.toString().replace("\\", "/").split("/");

            List<FileNode> currentLevel = roots;
            var currentPath = "";
            for (int i = 0; i < parts.length; i++) {
                var part = parts[i];
                var isLast = (i == parts.length - 1);
                currentPath = currentPath.isEmpty() ? part : currentPath + "/" + part;
                
                // Check if a node for this folder/file already exists at currentLevel
                var node = currentLevel.stream()
                        .filter(n -> n.name().equals(part))
                        .findFirst()
                        .orElse(null);
                
                if (node == null) {
                    // Determine if this part is a directory (if it is not the last part, or if the file system tells you so)
                    boolean isDirectory = !isLast;
                    node = new FileNode(part, currentPath, isDirectory);
                    currentLevel.add(node);
                }
                
                // For a directory, set the current level to its children.
                if (!isLast) {
                    currentLevel = node.children();
                }
            }
        }
        
        return roots;
    }
    
    public void createLibraryIndexPage() throws IOException {
//        initDocumentedItems();
        
        var context = new Context();
        context.setVariable("libraryName", libraryName);
        context.setVariable("sourceUrl", normalizeSourceUrl(basicQllData.sourceUrl()));
        context.setVariable("library", basicQllData);
        context.setVariable("entityDocs", entityDocs);
        context.setVariable("functionDocs", functionDocs);
        context.setVariable("fieldDocs", fieldDocs);
        context.setVariable("onExtensionDocs", onExtensionDocs);
        context.setVariable("searchIndexPath", relativeIndexPath);
        context.setVariable("descriptionFormatter", descriptionFormatter);

        var templateEngine = createTemplateEngine();

        var outputDir = Files.createDirectories(outputPath.resolve(getBasePath()));

        processAndWrite("templates/library.html", outputDir.resolve("index.html"), templateEngine, context);
    }
    
    private String normalizeSourceUrl(String sourceUrl) {
        if (sourceUrl.endsWith("/")) {
            return sourceUrl.substring(0, sourceUrl.length() - 1);
        } else {
            return sourceUrl;
        }
    }

    public void createEntityPages() throws IOException {
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
            context.setVariable("library", basicQllData);
            context.setVariable("fileName", documentedType.importPath());
            context.setVariable("name", documentedType.name());
            context.setVariable("description", entityDoc.description());

            var entityFields = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeField).toList();
            var entityFunctions = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeFunction).toList();
            var entityExtensionFunctions = entityDoc.onExtensionFunctions(); // always only functions
            var entityConstructors = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeEntityConstructor).toList();

            context.setVariable("fields", entityFields);
            context.setVariable("functions", entityFunctions);
            context.setVariable("extensionFunctions", entityExtensionFunctions);
            context.setVariable("constructors", entityConstructors);
            
            context.setVariable("allFunctions", Stream.of(entityFunctions, entityExtensionFunctions).flatMap(List::stream).toList());

            context.setVariable("searchIndexPath", relativeIndexPath);
            context.setVariable("descriptionFormatter", descriptionFormatter);

            var templateEngine = createTemplateEngine();

            processAndWrite("templates/entity.html", path, templateEngine, context);
        }
    }
    
    public void createSourceFilePages() throws IOException {
        var outputDir = Files.createDirectories(outputPath.resolve(getBasePath())).resolve("file");
        Files.createDirectories(outputDir);
        
        for (DocumentedFile documentedFile : documentedFiles) {
            var documentedEntities = documentedFile.documentedItems().stream().filter(documentedItem -> documentedItem.itemBeingDocumented() instanceof DocumentedTypeEntity).toList();
            var documentedFields = documentedFile.documentedItems().stream().filter(documentedItem -> documentedItem.itemBeingDocumented() instanceof DocumentedTypeField).toList();
            var documentedFunctions = documentedFile.documentedItems().stream().filter(documentedItem -> documentedItem.itemBeingDocumented() instanceof DocumentedTypeFunction).toList();

            var outputFilePath = outputDir.resolve(AnchorFactory.createHrefForSourceFile(documentedFile) + ".html");
            
            var context = new Context();
            context.setVariable("libraryName", libraryName);
            context.setVariable("library", basicQllData);
            context.setVariable("fileName", documentedFile.fileName());
            context.setVariable("filePath", documentedFile.importPath().toString().replace("\\", "/"));
            context.setVariable("descriptionFormatter", descriptionFormatter);
            
            context.setVariable("fields", documentedFields);
            context.setVariable("functions", documentedFunctions);
            context.setVariable("entities", documentedEntities);

            var templateEngine = createTemplateEngine();

            processAndWrite("templates/file.html", outputFilePath, templateEngine, context);
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
                        case FieldDoc _ -> fieldDocs.add(documentedItem);
                        case EntityDoc _ -> entityDocs.add(documentedItem);
                        case FunctionDoc _ -> {
                            var documentedTypeFunction = (DocumentedTypeFunction) documentedItem.itemBeingDocumented();
                            if (documentedTypeFunction.onOptional().isPresent()) {
                                onExtensionDocs.add(documentedItem);
                            } else {
                                functionDocs.add(documentedItem);
                            }
                        }
                        case ConstructorDoc _ -> { // Ignored, there will be none on a file-level (only entity level)
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
                var entityDoc = new EntityDoc(new DocDescription(List.of(new DocDescription.DocText(TypeUtility.getNativeTypeDescription(type)))));
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
    
    public void createSearchIndex() {
        try {
            var indexGenerator = new SearchIndexGenerator(entityDocs, onExtensionDocs, functionDocs, libraryName);
            indexGenerator.generateSearchIndex(outputPath.resolve(getBasePath()).resolve("index.json"));
        } catch (IOException e) {
            LOGGER.error("An error occurred while generating the search index.", e);
        }
    }

    public void writeToCache() {
        cachedDocHandler.writeLibraryCache(this);
    }

    public BasicQllData getBasicQll() {
        return basicQllData;
    }

    public List<DocumentedItem> getOnExtensionDocs() {
        return onExtensionDocs;
    }
}
