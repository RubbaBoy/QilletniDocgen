package is.yarr.qilletni.docgen.cache.serializer;

import is.yarr.qilletni.api.lang.docs.structure.DocFieldType;
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
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentationDeserializer implements AutoCloseable {
    
    private final MessageUnpacker unpacker;
    
    public DocumentationDeserializer(ByteArrayInputStream inputStream) {
        unpacker = MessagePack.newDefaultUnpacker(inputStream);
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
        var importPath = unpacker.unpackString();
        
        return switch (index) {
            case 0 -> new DocumentedTypeEntity(importPath, unpacker.unpackString());
            case 1 -> {
                var name = unpacker.unpackString();
                var params = deserializeParamNames();
                
                yield new DocumentedTypeEntityConstructor(importPath, name, params);
            }
            case 2 -> new DocumentedTypeField(importPath, unpacker.unpackString(), unpacker.unpackString());
            case 3 -> {
                var name = unpacker.unpackString();
                var params = deserializeParamNames();
                var isNative = unpacker.unpackBoolean();

                var onType = Optional.<String>empty();
                
                if (unpacker.getNextFormat().getValueType().isNilType()) {
                    unpacker.unpackNil();
                } else {
                    onType = Optional.of(unpacker.unpackString());
                }

                yield new DocumentedTypeFunction(importPath, name, params, isNative, onType);
            }
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        };
    }

    public InnerDoc deserializeInnerDoc() throws IOException {
        var innerDocType = unpacker.unpackInt();
        return switch (innerDocType) {
            case 0 -> { // ConstructorDoc
                var docDescription = deserializeDocDescription();
                var params = deserializeParams();

                yield new ConstructorDoc(docDescription, params);
            }
            case 1 -> { // EntityDoc
                var docDescription = deserializeDocDescription();
                var containedItemsSize = unpacker.unpackArrayHeader();

                var containedItems = new ArrayList<DocumentedItem>();
                for (int i = 0; i < containedItemsSize; i++) {
                    containedItems.add(deserializeDocumentedItem());
                }

                yield new EntityDoc(docDescription, containedItems);
            }
            case 2 -> new FieldDoc(deserializeDocDescription(), deserializeDocFieldType());
            case 3 -> { // FunctionDoc
                var docDescription = deserializeDocDescription();
                var params = deserializeParams();
                var returnDoc = deserializeReturnDoc();
                var docOnLine = deserializeDocOnLine();
                var docErrors = deserializeDocErrors();
                
                yield new FunctionDoc(docDescription, params, returnDoc, docOnLine, docErrors);
            }
            default -> throw new IllegalArgumentException("Invalid InnerDoc type: " + innerDocType);
        };
    }

    public DocErrors deserializeDocErrors() throws IOException {
        return new DocErrors(deserializeDocDescription());
    }

    public DocOnLine deserializeDocOnLine() throws IOException {
        return new DocOnLine(deserializeDocDescription());
    }

    public ReturnDoc deserializeReturnDoc() throws IOException {
        return new ReturnDoc(deserializeDocFieldType(), deserializeDocDescription());
    }
    
    public DocFieldType deserializeDocFieldType() throws IOException {
        var fieldType = DocFieldType.FieldType.values()[unpacker.unpackInt()];
        var identifier = unpacker.unpackString();
        
        return new DocFieldType(fieldType, identifier);
    }
    
    private List<ParamDoc> deserializeParams() throws IOException {
        var paramSize = unpacker.unpackArrayHeader();

        var params = new ArrayList<ParamDoc>();
        for (int i = 0; i < paramSize; i++) {
            params.add(deserializeParamDoc());
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
        var size = unpacker.unpackInt();
        var descriptionItems = new ArrayList<DocDescription.DescriptionItem>(size);

        for (var i = 0; i < size; i++) {
            descriptionItems.add(deserializeDescriptionItem());
        }

        return new DocDescription(descriptionItems);
    }

    @Override
    public void close() throws Exception {
        unpacker.close();
    }
}
