package dev.qilletni.docgen.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.qilletni.api.lang.docs.structure.DocumentedItem;
import dev.qilletni.api.lang.docs.structure.item.DocumentedType;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import dev.qilletni.docgen.pages.dialects.constructor.ConstructorSignatureAttributeTagProcessor;
import dev.qilletni.docgen.pages.dialects.entity.EntityHrefAttributeTagProcessor;
import dev.qilletni.docgen.pages.dialects.function.FunctionSignatureAttributeTagProcessor;
import dev.qilletni.docgen.pages.dialects.utility.AnchorFactory;
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
    private final DescriptionFormatter descriptionFormatter;

    public SearchIndexGenerator(List<DocumentedItem> entityDocs, List<DocumentedItem> onExtensionDocs, List<DocumentedItem> globalFunctionDocs, String libraryName) {
        this.entityDocs = entityDocs;
        this.onExtensionDocs = onExtensionDocs;
        this.globalFunctionDocs = globalFunctionDocs;
        this.libraryName = libraryName;
        this.descriptionFormatter = new DescriptionFormatter();
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
        var description = descriptionFormatter.getPlainDescription(documentedItem.innerDoc());
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
