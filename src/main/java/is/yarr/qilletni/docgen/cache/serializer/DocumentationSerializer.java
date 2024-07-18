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
import org.msgpack.core.MessagePacker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class DocumentationSerializer implements AutoCloseable {
    
    private final MessagePacker packer;
    
    public DocumentationSerializer(ByteArrayOutputStream outputStream) {
        packer = MessagePack.newDefaultPacker(outputStream);
    }
    
    public void serializeDocumentedItem(DocumentedItem documentedItem) throws IOException {
        serializeDocumentedType(documentedItem.itemBeingDocumented());
        serializeInnerDoc(documentedItem.innerDoc());
    }

    public void serializeDocumentedType(DocumentedType documentedType) throws IOException {
        packer.packInt(SerializationUtility.getDocumentedTypeIndex(documentedType));
        
        packer.packString(documentedType.importPath());

        switch (documentedType) {
            case DocumentedTypeEntity documentedTypeEntity -> {
                packer.packString(documentedTypeEntity.name());
            }
            case DocumentedTypeEntityConstructor documentedTypeEntityConstructor -> {
                packer.packString(documentedTypeEntityConstructor.name());
                
                serializeParamNameList(documentedTypeEntityConstructor.params());
            }
            case DocumentedTypeField documentedTypeField -> {
                packer.packString(documentedTypeField.type());
                packer.packString(documentedTypeField.name());
            }
            case DocumentedTypeFunction documentedTypeFunction -> {
                packer.packString(documentedTypeFunction.name());
                
                serializeParamNameList(documentedTypeFunction.params());
                
                packer.packBoolean(documentedTypeFunction.isNative());
                
                if (documentedTypeFunction.onOptional().isPresent()) {
                    packer.packString(documentedTypeFunction.onOptional().get());
                } else {
                    packer.packNil();
                }
            }
        }
    }
    
    public void serializeInnerDoc(InnerDoc innerDoc) throws IOException {
        packer.packInt(SerializationUtility.getInnerDocIndex(innerDoc));

        switch (innerDoc) {
            case ConstructorDoc constructorDoc -> {
                serializeDocDescription(constructorDoc.description());
                
                serializeParamDocList(constructorDoc.paramDocs());
            }
            case EntityDoc entityDoc -> {
                serializeDocDescription(entityDoc.description());
                
                packer.packArrayHeader(entityDoc.containedItems().size());
                for (var containedItem : entityDoc.containedItems()) {
                    serializeDocumentedItem(containedItem);
                }
            }
            case FieldDoc fieldDoc -> {
                serializeDocDescription(fieldDoc.description());

                serializeDocFieldType(fieldDoc.fieldType());
            }
            case FunctionDoc functionDoc -> {
                serializeDocDescription(functionDoc.description());
                serializeParamDocList(functionDoc.paramDocs());
                serializeReturnDoc(functionDoc.returnDoc());
                serializeDocOnLine(functionDoc.docOnLine());
                serializeDocErrors(functionDoc.docErrors());
            }
        }
    }

    public void serializeDocErrors(DocErrors docErrors) throws IOException {
        serializeDocDescription(docErrors.description());
    }

    public void serializeDocOnLine(DocOnLine docOnLine) throws IOException {
        serializeDocDescription(docOnLine.description());
    }

    public void serializeReturnDoc(ReturnDoc returnDoc) throws IOException {
        serializeDocFieldType(returnDoc.docFieldType());
        serializeDocDescription(returnDoc.description());
    }

    public void serializeDocFieldType(DocFieldType docFieldType) throws IOException {
        packer.packInt(docFieldType.fieldType().ordinal());
        packer.packString(docFieldType.identifier());
    }

    private void serializeParamDocList(List<ParamDoc> paramDocs) throws IOException {
        packer.packArrayHeader(paramDocs.size());
        for (var paramDoc : paramDocs) {
            serializeParamDoc(paramDoc);
        }
    }

    private void serializeParamNameList(List<String> params) throws IOException {
        packer.packArrayHeader(params.size());
        for (var param : params) {
            packer.packString(param);
        }
    }

    public void serializeParamDoc(ParamDoc paramDoc) throws IOException {
        packer.packString(paramDoc.name());
        serializeDocFieldType(paramDoc.docFieldType());
        serializeDocDescription(paramDoc.description());
    }

    public void serializeDocDescription(DocDescription docDescription) throws IOException {
        packer.packInt(docDescription.descriptionItems().size());

        for (var descriptionItem : docDescription.descriptionItems()) {
            serializeDescriptionItem(descriptionItem);
        }
    }

    public void serializeDescriptionItem(DocDescription.DescriptionItem descriptionItem) throws IOException {
        packer.packInt(SerializationUtility.getDescriptionItemIndex(descriptionItem));

        switch (descriptionItem) {
            case DocDescription.DocText docText -> packer.packString(docText.text());
            case DocDescription.JavaRef javaRef -> packer.packString(javaRef.javaName());
            case DocDescription.ParamRef paramRef -> packer.packString(paramRef.paramName());
            case DocDescription.TypeRef typeRef -> packer.packString(typeRef.typeName());
        }
    }

    @Override
    public void close() throws Exception {
        packer.flush();
        packer.close();
    }
}
