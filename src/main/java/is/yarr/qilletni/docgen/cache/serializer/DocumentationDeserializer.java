package is.yarr.qilletni.docgen.cache.serializer;

import is.yarr.qilletni.api.lang.docs.structure.DocFieldType;
import is.yarr.qilletni.api.lang.docs.structure.DocumentedFile;
import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedType;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.DocDescription;
import is.yarr.qilletni.api.lang.docs.structure.text.DocErrors;
import is.yarr.qilletni.api.lang.docs.structure.text.DocOnLine;
import is.yarr.qilletni.api.lang.docs.structure.text.ParamDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.ReturnDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.InnerDoc;
import is.yarr.qilletni.docgen.cache.serializer.DocumentationSerializer.NilPlaceholder;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DocumentationDeserializer implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationDeserializer.class);
    
    private final MessageUnpacker unpacker;
    private final UnpackerHandler unpackerHandler;
    
    public DocumentationDeserializer(InputStream inputStream) throws NoSuchFieldException, IllegalAccessException {
        unpacker = MessagePack.newDefaultUnpacker(inputStream);
        unpackerHandler = new UnpackerHandler(unpacker);
    }
    
    public List<DocumentedFile> deserializeDocumentedFileList() throws IOException {
        var size = unpacker.unpackArrayHeader();
        
        var documentedFiles = new ArrayList<DocumentedFile>();
        for (int i = 0; i < size; i++) {
            documentedFiles.add(deserializeDocumentedFile());
        }
        
        return documentedFiles;
    }
    
    public DocumentedFile deserializeDocumentedFile() throws IOException {
        var fileName = unpacker.unpackString();
        var documentedItemsSize = unpacker.unpackArrayHeader();

        LOGGER.debug("Deserializing file: {} with {} items", fileName, documentedItemsSize);
        
        var documentedItems = new ArrayList<DocumentedItem>();
        for (int i = 0; i < documentedItemsSize; i++) {
            var documentedItem = deserializeDocumentedItem();
            documentedItems.add(documentedItem);
        }
        
        return new DocumentedFile(fileName, documentedItems);
    }
    
    public DocumentedItem deserializeDocumentedItem() throws IOException {
        var documentedType = deserializeDocumentedType();
        var innerDoc = deserializeInnerDoc();
        
        return new DocumentedItem(documentedType, innerDoc);
    }
    
    public DocDescription.DescriptionItem deserializeDescriptionItem() throws IOException {
        var index = unpacker.unpackInt();
        var string = unpacker.unpackString();
        
        return switch (index) {
            case 0 -> new DocDescription.DocText(string);
            case 1 -> new DocDescription.JavaRef(string);
            case 2 -> new DocDescription.ParamRef(string);
            case 3 -> new DocDescription.TypeRef(string);
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        };
    }
    
    public DocumentedType deserializeDocumentedType() throws IOException {
        var index = unpacker.unpackInt();
        var libraryName = unpacker.unpackString();
        var importPath = unpacker.unpackString();
        
        return switch (index) {
            case 0 -> new DocumentedTypeEntity(libraryName, importPath, unpacker.unpackString());
            case 1 -> {
                var name = unpacker.unpackString();
                var params = deserializeParamNames();
                
                yield new DocumentedTypeEntityConstructor(libraryName, importPath, name, params);
            }
            case 2 -> new DocumentedTypeField(libraryName, importPath, unpacker.unpackString(), unpacker.unpackString());
            case 3 -> {
                var name = unpacker.unpackString();
                var params = deserializeParamNames();
                var isNative = unpacker.unpackBoolean();
                var isStatic = unpacker.unpackBoolean();

                var onType = Optional.<String>empty();
                
                if (!hasNilPlaceholderNext(NilPlaceholder.NO_ON_TYPE)) {
                    onType = Optional.of(unpacker.unpackString());
                }

                yield new DocumentedTypeFunction(libraryName, importPath, name, params, isNative, isStatic, onType);
            }
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        };
    }

    public InnerDoc deserializeInnerDoc() throws IOException {
        var innerDocType = unpacker.unpackInt();
        return switch (innerDocType) {
            case 0 -> { // ConstructorDoc
                var docDescription = deserializeDocDescription();
                var params = deserializeParamDocList();

                yield new ConstructorDoc(docDescription, params);
            }
            case 1 -> { // EntityDoc
                var docDescription = deserializeDocDescription();

                var containedItems = deserializeDocumentedItemList();
                var onExtensionFunctions = deserializeDocumentedItemList();

                yield new EntityDoc(docDescription, containedItems, onExtensionFunctions);
            }
            case 2 -> new FieldDoc(deserializeDocDescription(), deserializeDocFieldType());
            case 3 -> { // FunctionDoc
                var docDescription = deserializeDocDescription();

                var params = deserializeParamDocList();
                var returnDoc = deserializeReturnDoc();
                var docOnLine = deserializeDocOnLine();
                var docErrors = deserializeDocErrors();
                
                yield new FunctionDoc(docDescription, params, returnDoc, docOnLine, docErrors);
            }
            default -> throw new IllegalArgumentException("Invalid InnerDoc type: " + innerDocType);
        };
    }

    public DocErrors deserializeDocErrors() throws IOException {
        if (hasNilPlaceholderNext(NilPlaceholder.NO_ERRORS)) {
            return null;
        }
        
        return new DocErrors(deserializeDocDescription());
    }

    public DocOnLine deserializeDocOnLine() throws IOException {
        if (hasNilPlaceholderNext(NilPlaceholder.NO_ON_LINE)) {
            return null;
        }
        
        return new DocOnLine(deserializeDocFieldType(), deserializeDocDescription());
    }

    public ReturnDoc deserializeReturnDoc() throws IOException {
        if (hasNilPlaceholderNext(NilPlaceholder.NO_RETURN)) {
            return null;
        }
        
        return new ReturnDoc(deserializeDocFieldType(), deserializeDocDescription());
    }
    
    public DocFieldType deserializeDocFieldType() throws IOException {
        if (hasNilPlaceholderNext(NilPlaceholder.NO_FIELD_TYPE)) {
            return null;
        }

        var fieldType = DocFieldType.FieldType.values()[unpacker.unpackInt()];
        var identifier = unpacker.unpackString();
        
        return new DocFieldType(fieldType, identifier);
    }
    
    public List<DocumentedItem> deserializeDocumentedItemList() throws IOException {
        var size = unpacker.unpackArrayHeader();
        
        var documentedItems = new ArrayList<DocumentedItem>();
        for (int i = 0; i < size; i++) {
            documentedItems.add(deserializeDocumentedItem());
        }
        
        return documentedItems;
    }
    
    public List<ParamDoc> deserializeParamDocList() throws IOException {
        var paramSize = unpacker.unpackArrayHeader();

        var params = new ArrayList<ParamDoc>();
        for (int i = 0; i < paramSize; i++) {
            var paramDoc = deserializeParamDoc();
            params.add(paramDoc);
        }
        
        return params;
    }
    
    public List<String> deserializeParamNames() throws IOException {
        var size = unpacker.unpackArrayHeader();
        
        var params = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            params.add(unpacker.unpackString());
        }
        
        return params;
    }
    
    public ParamDoc deserializeParamDoc() throws IOException {
        var name = unpacker.unpackString();
        var docFieldType = deserializeDocFieldType();
        var description = deserializeDocDescription();
        
        return new ParamDoc(name, docFieldType, description);
    }

    public DocDescription deserializeDocDescription() throws IOException {
        if (hasNilPlaceholderNext(NilPlaceholder.NO_DESCRIPTION)) {
            return null;
        }
        
        var size = unpacker.unpackArrayHeader();
        var descriptionItems = new ArrayList<DocDescription.DescriptionItem>(size);

        for (var i = 0; i < size; i++) {
            descriptionItems.add(deserializeDescriptionItem());
        }

        return new DocDescription(descriptionItems);
    }
    
    private boolean hasNilPlaceholderNext(NilPlaceholder nilPlaceholder) throws IOException {
        if (unpacker.getNextFormat().getValueType().isNilType()) {
            var ordinalByte = unpackerHandler.getMessageBuffer().getByte(unpackerHandler.getUnpackerPosition() + 1); // Get not this, but the next byte
            if (ordinalByte == nilPlaceholder.ordinal()) {
                unpacker.unpackNil(); // Nil byte
                unpacker.unpackInt(); // Ordinal byte
                return true;
            }
        }

        return false;
    }

    @Override
    public void close() throws Exception {
        unpacker.close();
    }
}
