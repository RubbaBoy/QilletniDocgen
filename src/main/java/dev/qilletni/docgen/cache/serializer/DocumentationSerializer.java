package dev.qilletni.docgen.cache.serializer;

import dev.qilletni.api.lang.docs.structure.DocFieldType;
import dev.qilletni.api.lang.docs.structure.DocumentedFile;
import dev.qilletni.api.lang.docs.structure.DocumentedItem;
import dev.qilletni.api.lang.docs.structure.item.DocumentedType;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.api.lang.docs.structure.text.DocDescription;
import dev.qilletni.api.lang.docs.structure.text.DocErrors;
import dev.qilletni.api.lang.docs.structure.text.DocOnLine;
import dev.qilletni.api.lang.docs.structure.text.ParamDoc;
import dev.qilletni.api.lang.docs.structure.text.ReturnDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.InnerDoc;
import dev.qilletni.docgen.cache.BasicQllData;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class DocumentationSerializer implements AutoCloseable {
    
    private final MessagePacker packer;
    
    public DocumentationSerializer(OutputStream outputStream) {
        packer = MessagePack.newDefaultPacker(outputStream);
    }
    
    public void serializeLibrary(BasicQllData basicQllData, List<DocumentedFile> documentedFiles) throws IOException {
        serializeBasicQllData(basicQllData);
        serializeDocumentedFileList(documentedFiles);
    }
    
    public void serializeDocumentedFileList(List<DocumentedFile> documentedFiles) throws IOException {
        packer.packArrayHeader(documentedFiles.size());
        for (var documentedFile : documentedFiles) {
            serializeDocumentedFile(documentedFile);
        }
    }

    public void serializeDocumentedFile(DocumentedFile documentedFile) throws IOException {
        packer.packString(documentedFile.fileName());
        packer.packString(documentedFile.importPath().toString());
        packer.packArrayHeader(documentedFile.documentedItems().size());
        for (var documentedItem : documentedFile.documentedItems()) {
            serializeDocumentedItem(documentedItem);
        }
    }
    
    public void serializeDocumentedItem(DocumentedItem documentedItem) throws IOException {
        serializeDocumentedType(documentedItem.itemBeingDocumented());
        serializeInnerDoc(documentedItem.innerDoc());
    }

    public void serializeDocumentedType(DocumentedType documentedType) throws IOException {
        packer.packInt(SerializationUtility.getDocumentedTypeIndex(documentedType));
        
        packer.packString(documentedType.libraryName());
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
                packer.packBoolean(documentedTypeFunction.isStatic());
                
                if (documentedTypeFunction.onOptional().isPresent()) {
                    packer.packString(documentedTypeFunction.onOptional().get());
                } else {
                    packNilPlaceholder(NilPlaceholder.NO_ON_TYPE);
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

                serializeDocumentedItemList(entityDoc.containedItems());
                serializeDocumentedItemList(entityDoc.onExtensionFunctions());
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
        if (docErrors == null) {
            packNilPlaceholder(NilPlaceholder.NO_ERRORS);
            return;
        }
        
        serializeDocDescription(docErrors.description());
    }

    public void serializeDocOnLine(DocOnLine docOnLine) throws IOException {
        if (docOnLine == null) {
            packNilPlaceholder(NilPlaceholder.NO_ON_LINE);
            return;
        }
        
        serializeDocFieldType(docOnLine.docFieldType());
        serializeDocDescription(docOnLine.description());
    }

    public void serializeReturnDoc(ReturnDoc returnDoc) throws IOException {
        if (returnDoc == null) {
            packNilPlaceholder(NilPlaceholder.NO_RETURN);
            return;
        }

        serializeDocFieldType(returnDoc.docFieldType());
        serializeDocDescription(returnDoc.description());
    }

    public void serializeDocFieldType(DocFieldType docFieldType) throws IOException {
        if (docFieldType == null) {
            packNilPlaceholder(NilPlaceholder.NO_FIELD_TYPE);
            return;
        }

        packer.packInt(docFieldType.fieldType().ordinal());
        packer.packString(docFieldType.identifier());
    }
    
    public void serializeDocumentedItemList(List<DocumentedItem> documentedItems) throws IOException {
        packer.packArrayHeader(documentedItems.size());
        for (var documentedItem : documentedItems) {
            serializeDocumentedItem(documentedItem);
        }
    }

    public void serializeParamDocList(List<ParamDoc> paramDocs) throws IOException {
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
        if (docDescription == null) {
            packNilPlaceholder(NilPlaceholder.NO_DESCRIPTION);
            return;
        }
        
        packer.packArrayHeader(docDescription.descriptionItems().size());

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

    public long getTotalWrittenBytes() {
        return packer.getTotalWrittenBytes();
    }
    
    private void packNilPlaceholder(NilPlaceholder nilPlaceholder) throws IOException {
        packer.packNil();
        packer.packInt(nilPlaceholder.ordinal());
    }
    
    public void serializeBasicQllData(BasicQllData basicQllData) throws IOException {
        packer.packString(basicQllData.name());
        packer.packString(basicQllData.version());
        packer.packString(basicQllData.author());
        packer.packString(basicQllData.description());
    }

    public enum NilPlaceholder {
        NO_ERRORS,
        NO_ON_LINE,
        NO_ON_TYPE,
        NO_RETURN,
        NO_FIELD_TYPE,
        NO_DESCRIPTION;
    }

    @Override
    public void close() throws Exception {
        packer.flush();
        packer.close();
    }
}
