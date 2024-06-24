package is.yarr.qilletni.docgen;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedFile;
import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedType;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.docgen.pages.dialects.description.FormattedDocDialect;
import is.yarr.qilletni.docgen.pages.dialects.entity.EntityDialect;
import is.yarr.qilletni.docgen.pages.dialects.function.FunctionDialect;
import is.yarr.qilletni.lang.docs.DefaultDocumentationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DocParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocParser.class);
    
    private String libraryName;
    
    private final List<DocumentedFile> documentedFiles;
    private final List<DocumentedItem> entityDocs;
    private final List<DocumentedItem> functionDocs;
    private final List<DocumentedItem> fieldDocs;
    
    private final List<DocumentedItem> onExtensionDocs;

    public DocParser(String libraryName, List<DocumentedFile> documentedFiles) {
        this.libraryName = libraryName;
        this.documentedFiles = documentedFiles;
        this.entityDocs = new ArrayList<>();
        this.functionDocs = new ArrayList<>();
        this.fieldDocs = new ArrayList<>();
        this.onExtensionDocs = new ArrayList<>();
    }

    public static DocParser createDocParser(String libraryName, Path input) {
        var documentedFiles = new ArrayList<DocumentedFile>();

        try (var walk = Files.walk(input)) {
            walk.forEach(file -> {
                if (Files.isDirectory(file) || !file.getFileName().toString().endsWith(".ql")) return;

                try {
                    LOGGER.debug("Parsing file: {}", file.getFileName());

                    var parser = new DefaultDocumentationParser();

                    var documentedFile = parser.parseDocsFromPath(file);
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
        
        return new DocParser(libraryName, documentedFiles);
    }
    
    public Path getBasePath() {
        return Paths.get("library", libraryName);
    }
    
    private TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.addDialect(new FunctionDialect());
        templateEngine.addDialect(new EntityDialect(libraryName));
        templateEngine.addDialect(new FormattedDocDialect());
        
        return templateEngine;
    }
    
    private void processAndWrite(String templatePath, Path outputPath, TemplateEngine templateEngine, Context context) throws IOException {
        String output = templateEngine.process(templatePath, context);
        
        
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            writer.write(output);
        }
    }
    
    public void createIndexFile() throws IOException {
        initDocumentedItems();
        
        var context = new Context();
        context.setVariable("libraryName", libraryName);
        context.setVariable("entityDocs", entityDocs);
        context.setVariable("functionDocs", functionDocs);
        context.setVariable("fieldDocs", fieldDocs);
        context.setVariable("onExtensionDocs", onExtensionDocs);

        var templateEngine = createTemplateEngine();

        var outputDir = Files.createDirectories(Paths.get("output").resolve(getBasePath()));

        processAndWrite("templates/index.html", outputDir.resolve("index.html"), templateEngine, context);
    }
    
    public void createEntityFiles() throws IOException {
        var outputDir = Files.createDirectories(Paths.get("output").resolve(getBasePath())).resolve("entity");
        Files.createDirectories(outputDir);

//        for (DocumentedItem documentedItem : entityDocs.stream().filter(documentedItem -> ((DocumentedTypeEntity) documentedItem.itemBeingDocumented()).name().equals("Map")).toList()) {
        for (DocumentedItem documentedItem : entityDocs) {
            var documentedType = (DocumentedTypeEntity) documentedItem.itemBeingDocumented();
            var entityDoc = (EntityDoc) documentedItem.innerDoc();
            
            var path = outputDir.resolve("%s.html".formatted(documentedType.name()));
            
            var context = new Context();
            context.setVariable("libraryName", libraryName);
            context.setVariable("name", documentedType.name());
            context.setVariable("description", entityDoc.description());
            context.setVariable("currentPath", path.toString());

            var entityFields = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeField).toList();
            var entityFunctions = entityDoc.containedItems().stream().filter(item -> item.itemBeingDocumented() instanceof DocumentedTypeFunction).toList();

            context.setVariable("fields", entityFields);
            context.setVariable("functions", entityFunctions);
//        context.setVariable("fieldDocs", fieldDocs);

            var templateEngine = createTemplateEngine();

            processAndWrite("templates/entity.html", path, templateEngine, context);
        }
    }
    
    private void initDocumentedItems() {
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
    }
}
