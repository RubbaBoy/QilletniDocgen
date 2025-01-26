package is.yarr.qilletni.docgen.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedType;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.InnerDoc;
import is.yarr.qilletni.docgen.pages.dialects.constructor.ConstructorSignatureAttributeTagProcessor;
import is.yarr.qilletni.docgen.pages.dialects.description.MessageCreator;
import is.yarr.qilletni.docgen.pages.dialects.entity.EntityHrefAttributeTagProcessor;
import is.yarr.qilletni.docgen.pages.dialects.function.FunctionSignatureAttributeTagProcessor;
import is.yarr.qilletni.docgen.pages.dialects.utility.AnchorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SearchIndexGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexGenerator.class);
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    
    private final List<DocumentedItem> entityDocs;
    private final List<DocumentedItem> onExtensionDocs;
    private final List<DocumentedItem> globalFunctionDocs;
    private final String libraryName;
    private final Parser parser;

    public SearchIndexGenerator(List<DocumentedItem> entityDocs, List<DocumentedItem> onExtensionDocs, List<DocumentedItem> globalFunctionDocs, String libraryName) {
        this.entityDocs = entityDocs;
        this.onExtensionDocs = onExtensionDocs;
        this.globalFunctionDocs = globalFunctionDocs;
        this.libraryName = libraryName;

        this.parser = Parser.builder().build();
    }

    public void generateSearchIndex(Path outputFile) throws IOException {
        LOGGER.info("Generating search index at {}", outputFile);
        
        var indexEntries = createIndexEntries();
        writeIndexEntries(indexEntries, outputFile);
    }
    
    private List<IndexEntry> createIndexEntries() {
        var indexEntries = new ArrayList<IndexEntry>();

        for (DocumentedItem documentedItem : entityDocs) {
            var entityDocumentedType = ((DocumentedTypeEntity) documentedItem.itemBeingDocumented());
            var entityDoc = (EntityDoc) documentedItem.innerDoc();

            indexEntries.add(createIndexEntry(documentedItem));

            for (DocumentedItem containedItem : entityDoc.containedItems()) {
                // Skipping fields as they aren't displayed yet. This index code still supports fields, though.
                if (containedItem.itemBeingDocumented() instanceof DocumentedTypeField) continue;

                indexEntries.add(createIndexEntry(containedItem, entityDocumentedType.name()));
            }
        }

        indexEntries.addAll(Stream.concat(onExtensionDocs.stream(), globalFunctionDocs.stream())
                .parallel()
                .map(this::createIndexEntry)
                .toList());
        
        return indexEntries;
    }
    
    private IndexEntry createIndexEntry(DocumentedItem documentedItem) {
        return createIndexEntry(documentedItem, null);
    }
    
    private IndexEntry createIndexEntry(DocumentedItem documentedItem, String parentOverride) {
        var documentedType = documentedItem.itemBeingDocumented();

        var id = generateId(documentedType);
        var indexType = getIndexType(documentedType);
        var parent = getParent(documentedType);
        
        if (parent == null) {
            parent = parentOverride;
        }
        
        var title = createTitle(documentedType);
        var url = createUrl(documentedType, parentOverride);
        var description = getPlainDescription(documentedItem.innerDoc());
        return new IndexEntry(id, indexType, parent, title, documentedType.importPath(), url, documentedType.libraryName(), description);
    }
    
    private String getParent(DocumentedType documentedType) {
        if (!(documentedType instanceof DocumentedTypeFunction documentedTypeFunction)) {
            return null;
        }
        
        return documentedTypeFunction.onOptional().orElse(null);
    }
    
    private String getIndexType(DocumentedType documentedType) {
        return switch (documentedType) {
            case DocumentedTypeEntity _ -> "entity";
            case DocumentedTypeEntityConstructor _ -> "constructor";
            case DocumentedTypeField _ -> "field";
            case DocumentedTypeFunction _ -> "function";
        };
    }

    /**
     * Gets a string description of the given {@link InnerDoc}, without any markdown formatting. If no doc is found,
     * return an empty string.
     * 
     * @param innerDoc The documentation to strip markdown from
     * @return The string doc
     */
    private String getPlainDescription(InnerDoc innerDoc) {
        var docDescription = switch (innerDoc) {
            case ConstructorDoc constructorDoc -> constructorDoc.description();
            case EntityDoc entityDoc -> entityDoc.description();
            case FieldDoc fieldDoc -> fieldDoc.description();
            case FunctionDoc functionDoc -> functionDoc.description();
        };
        
        if (docDescription == null) {
            return "";
        }
        
        var markdownString = MessageCreator.convertDescriptionItemsToString(docDescription.descriptionItems());

        var node = parser.parse(markdownString);
        var textVisitor = new TextCollectingVisitor();
        return textVisitor.collectAndGetText(node);
    }

    private String generateId(DocumentedType documentedType) {
        return switch (documentedType) {
            case DocumentedTypeEntity documentedTypeEntity -> "%s-%s".formatted(documentedTypeEntity.importPath(), documentedTypeEntity.name());
            case DocumentedTypeEntityConstructor documentedTypeEntityConstructor -> "%s-%s-constructor".formatted(documentedTypeEntityConstructor.importPath(), documentedTypeEntityConstructor.name());
            case DocumentedTypeField documentedTypeField -> "%s-%s-%s".formatted(documentedTypeField.importPath(), documentedTypeField.type(), documentedTypeField.name());
            case DocumentedTypeFunction documentedTypeFunction -> "%s-%s-%s".formatted(documentedTypeFunction.importPath(), documentedTypeFunction.name(), String.join(",", documentedTypeFunction.params()));
        };
    }

    private String createTitle(DocumentedType documentedType) {
        return switch (documentedType) {
            case DocumentedTypeEntity documentedTypeEntity -> documentedTypeEntity.name();
            case DocumentedTypeEntityConstructor documentedTypeEntityConstructor -> ConstructorSignatureAttributeTagProcessor.getConstructorSignature(documentedTypeEntityConstructor);
            case DocumentedTypeField documentedTypeField -> "%s %s".formatted(documentedTypeField.type(), documentedTypeField.name());
            case DocumentedTypeFunction documentedTypeFunction -> FunctionSignatureAttributeTagProcessor.getFunctionSignature(documentedTypeFunction);
        };
    }
    
    private String createUrl(DocumentedType documentedType, String parentEntity) {
        var baseUrl = "";
        
        if (parentEntity != null) {
            baseUrl = EntityHrefAttributeTagProcessor.getEntityUrl(libraryName, parentEntity);
        } else {
            baseUrl = EntityHrefAttributeTagProcessor.getLibraryUrl(libraryName);
        }
        
        return switch (documentedType) {
            case DocumentedTypeEntity documentedTypeEntity -> "%sentity/%s".formatted(baseUrl, documentedTypeEntity.name());
            case DocumentedTypeEntityConstructor documentedTypeEntityConstructor -> "%s#%s".formatted(baseUrl, AnchorFactory.createAnchorForConstructor(documentedTypeEntityConstructor));
            case DocumentedTypeField documentedTypeField -> baseUrl; // Not fully supported yet
            case DocumentedTypeFunction documentedTypeFunction -> "%s#%s".formatted(baseUrl, AnchorFactory.createAnchorForFunction(documentedTypeFunction));
        };
    }
    
    private void writeIndexEntries(List<IndexEntry> indexEntries, Path outputFile) throws IOException {
        var jsonString = gson.toJson(indexEntries);
        Files.writeString(outputFile, jsonString);
    }
}
