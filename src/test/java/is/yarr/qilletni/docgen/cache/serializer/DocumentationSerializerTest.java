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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentationSerializerTest {

    static Stream<DocumentedItem> documentedItemProvider() {
        return Stream.of(
                new DocumentedItem(
                        new DocumentedTypeEntity("EntityExample", "com.example.EntityExample"),
                        new EntityDoc(new DocDescription(Collections.emptyList()), Collections.emptyList())
                ),
                new DocumentedItem(
                        new DocumentedTypeEntityConstructor("ConstructorExample", "com.example.ConstructorExample", List.of("param1", "param2")),
                        new ConstructorDoc(new DocDescription(Collections.singletonList(new DocDescription.DocText("Constructor description"))), Collections.emptyList())
                ),
                new DocumentedItem(
                        new DocumentedTypeField("FieldExample", "com.example.FieldExample", "String"),
                        new FieldDoc(new DocDescription(Collections.singletonList(new DocDescription.DocText("Field description"))), new DocFieldType(DocFieldType.FieldType.JAVA, "String"))
                ),
                new DocumentedItem(
                        new DocumentedTypeEntity("EmptyEntity", "com.example.EmptyEntity"),
                        new EntityDoc(new DocDescription(Collections.emptyList()), Collections.emptyList())
                ),
                new DocumentedItem(
                        new DocumentedTypeFunction("ComplexFunction", "com.example.ComplexFunction", List.of("paramX", "paramY"), true, Optional.empty()),
                        new FunctionDoc(new DocDescription(Collections.singletonList(new DocDescription.DocText("Complex function with multiple params"))), Collections.emptyList(), new ReturnDoc(new DocFieldType(DocFieldType.FieldType.JAVA, "void"), new DocDescription(Collections.emptyList())), new DocOnLine(new DocDescription(Collections.singletonList(new DocDescription.DocText("Online doc text")))), new DocErrors(new DocDescription(Collections.singletonList(new DocDescription.DocText("Error text")))))
                )
        );
    }

    @ParameterizedTest
    @MethodSource("documentedItemProvider")
    void testSerializeDeserializeDocumentedItem(DocumentedItem originalItem) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeDocumentedItem(originalItem);
        serializer.close();

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        DocumentedItem deserializedItem = deserializer.deserializeDocumentedItem();

        assertEquals(originalItem, deserializedItem);
    }

    static Stream<DocumentedType> documentedTypeProvider() {
        return Stream.of(
                new DocumentedTypeEntity("TestEntity", "com.example.TestEntity"),
                new DocumentedTypeEntityConstructor("TestConstructor", "com.example.TestConstructor", Collections.emptyList()),
                new DocumentedTypeField("int", "testField", "com.example.TestField"),
                new DocumentedTypeFunction("testFunction", "com.example.TestFunction", Collections.emptyList(), false, Optional.empty()),
                new DocumentedTypeFunction("testFunction", "com.example.TestFunction", List.of("aa", "bb", "cc"), true, Optional.empty()),
                new DocumentedTypeFunction("testFunction", "com.example.TestFunction", List.of("aa", "bb", "cc"), true, Optional.of("Artist"))
        );
    }

    @ParameterizedTest
    @MethodSource("documentedTypeProvider")
    void testSerializeDeserializeDocumentedType(DocumentedType originalType) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeDocumentedType(originalType);
        serializer.close();

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        DocumentedType deserializedType = deserializer.deserializeDocumentedType();

        assertEquals(originalType, deserializedType);
    }

    static Stream<InnerDoc> innerDocProvider() {
        return Stream.of(
                new ConstructorDoc(new DocDescription(Collections.emptyList()), Collections.emptyList()),
                new EntityDoc(new DocDescription(Collections.emptyList()), Collections.emptyList()),
                new FieldDoc(new DocDescription(Collections.emptyList()), new DocFieldType(DocFieldType.FieldType.JAVA, "int")),
                new FunctionDoc(new DocDescription(Collections.emptyList()), Collections.emptyList(), new ReturnDoc(new DocFieldType(DocFieldType.FieldType.JAVA, "int"), new DocDescription(Collections.singletonList(new DocDescription.DocText("Return text")))), new DocOnLine(new DocDescription(Collections.singletonList(new DocDescription.DocText("On line text")))), new DocErrors(new DocDescription(Collections.singletonList(new DocDescription.DocText("Error text")))))
        );
    }

    @ParameterizedTest
    @MethodSource("innerDocProvider")
    void testSerializeDeserializeInnerDoc(InnerDoc originalInnerDoc) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeInnerDoc(originalInnerDoc);
        serializer.close();

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        InnerDoc deserializedInnerDoc = deserializer.deserializeInnerDoc();

        assertEquals(originalInnerDoc, deserializedInnerDoc);
    }

    static Stream<DocErrors> docErrorsProvider() {
        return Stream.of(
                new DocErrors(new DocDescription(Collections.singletonList(new DocDescription.DocText("Error text")))),
                new DocErrors(new DocDescription(List.of(new DocDescription.DocText("Error text"), new DocDescription.JavaRef("HashMap")))),
                new DocErrors(new DocDescription(Collections.emptyList()))
        );
    }

    @ParameterizedTest
    @MethodSource("docErrorsProvider")
    void testSerializeDeserializeDocErrors(DocErrors originalDocErrors) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeDocErrors(originalDocErrors);
        serializer.close();

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        DocErrors deserializedDocErrors = deserializer.deserializeDocErrors();

        assertEquals(originalDocErrors, deserializedDocErrors);
    }
    
    static Stream<DocOnLine> docOnLineProvider() {
        return Stream.of(
                new DocOnLine(new DocDescription(Collections.singletonList(new DocDescription.DocText("On line text")))),
                new DocOnLine(new DocDescription(List.of(new DocDescription.DocText("On line text"), new DocDescription.JavaRef("HashMap")))),
                new DocOnLine(new DocDescription(Collections.emptyList()))
        );
    }
    
    @ParameterizedTest
    @MethodSource("docOnLineProvider")
    void testSerializeDeserializeDocOnLine(DocOnLine originalDocOnLine) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeDocOnLine(originalDocOnLine);
        serializer.close();
        
        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        DocOnLine deserializedDocOnLine = deserializer.deserializeDocOnLine();
        
        assertEquals(originalDocOnLine, deserializedDocOnLine);
    }
    
    static Stream<ReturnDoc> returnDocProvider() {
        return Stream.of(
                new ReturnDoc(new DocFieldType(DocFieldType.FieldType.JAVA, "int"), new DocDescription(Collections.singletonList(new DocDescription.DocText("Return text")))),
                new ReturnDoc(new DocFieldType(DocFieldType.FieldType.QILLETNI, "int"), new DocDescription(Collections.singletonList(new DocDescription.DocText("Return text")))),
                new ReturnDoc(new DocFieldType(DocFieldType.FieldType.JAVA, "boolean"), new DocDescription(List.of(new DocDescription.DocText("Return text"), new DocDescription.JavaRef("HashMap")))),
                new ReturnDoc(new DocFieldType(DocFieldType.FieldType.QILLETNI, "boolean"), new DocDescription(List.of(new DocDescription.DocText("Return text"), new DocDescription.JavaRef("HashMap")))),
                new ReturnDoc(new DocFieldType(DocFieldType.FieldType.JAVA, "string"), new DocDescription(Collections.emptyList())),
                new ReturnDoc(new DocFieldType(DocFieldType.FieldType.QILLETNI, "string"), new DocDescription(Collections.emptyList()))
        );
    }
    
    @ParameterizedTest
    @MethodSource("returnDocProvider")
    void testSerializeDeserializeReturnDoc(ReturnDoc originalReturnDoc) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeReturnDoc(originalReturnDoc);
        serializer.close();
        
        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        ReturnDoc deserializedReturnDoc = deserializer.deserializeReturnDoc();
        
        assertEquals(originalReturnDoc, deserializedReturnDoc);
    }
    
    static Stream<DocFieldType> docFieldTypeProvider() {
        return Stream.of(
                new DocFieldType(DocFieldType.FieldType.JAVA, "int"),
                new DocFieldType(DocFieldType.FieldType.QILLETNI, "int"),
                new DocFieldType(DocFieldType.FieldType.JAVA, "boolean"),
                new DocFieldType(DocFieldType.FieldType.QILLETNI, "boolean"),
                new DocFieldType(DocFieldType.FieldType.JAVA, "string"),
                new DocFieldType(DocFieldType.FieldType.QILLETNI, "string")
        );
    }
    
    @ParameterizedTest
    @MethodSource("docFieldTypeProvider")
    void testSerializeDeserializeDocFieldType(DocFieldType originalDocFieldType) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeDocFieldType(originalDocFieldType);
        serializer.close();
        
        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        DocFieldType deserializedDocFieldType = deserializer.deserializeDocFieldType();
        
        assertEquals(originalDocFieldType, deserializedDocFieldType);
    }
    
    static Stream<ParamDoc> paramDocProvider() {
        return Stream.of(
                new ParamDoc("paramName", new DocFieldType(DocFieldType.FieldType.JAVA, "int"), new DocDescription(Collections.singletonList(new DocDescription.DocText("Param text")))),
                new ParamDoc("paramName", new DocFieldType(DocFieldType.FieldType.QILLETNI, "int"), new DocDescription(Collections.singletonList(new DocDescription.DocText("Param text")))),
                new ParamDoc("paramName", new DocFieldType(DocFieldType.FieldType.JAVA, "boolean"), new DocDescription(List.of(new DocDescription.DocText("Param text"), new DocDescription.JavaRef("HashMap")))),
                new ParamDoc("paramName", new DocFieldType(DocFieldType.FieldType.QILLETNI, "boolean"), new DocDescription(List.of(new DocDescription.DocText("Param text"), new DocDescription.JavaRef("HashMap")))),
                new ParamDoc("paramName", new DocFieldType(DocFieldType.FieldType.JAVA, "string"), new DocDescription(Collections.emptyList())),
                new ParamDoc("paramName", new DocFieldType(DocFieldType.FieldType.QILLETNI, "string"), new DocDescription(Collections.emptyList()))
        );
    }
    
    @ParameterizedTest
    @MethodSource("paramDocProvider")
    void testSerializeDeserializeParamDoc(ParamDoc originalParamDoc) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeParamDoc(originalParamDoc);
        serializer.close();
        
        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        ParamDoc deserializedParamDoc = deserializer.deserializeParamDoc();
        
        assertEquals(originalParamDoc, deserializedParamDoc);
    }

    static Stream<DocDescription.DescriptionItem> descriptionItemProvider() {
        return Stream.of(
                new DocDescription.DocText("Example text"),
                new DocDescription.JavaRef("java.lang.String"),
                new DocDescription.ParamRef("paramName"),
                new DocDescription.TypeRef("TypeName")
        );
    }

    @ParameterizedTest
    @MethodSource("descriptionItemProvider")
    void testSerializeDeserializeDescriptionItem(DocDescription.DescriptionItem originalItem) throws Exception {
        var outputStream = new ByteArrayOutputStream();
        var serializer = new DocumentationSerializer(outputStream);
        serializer.serializeDescriptionItem(originalItem);
        serializer.close();

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        var deserializer = new DocumentationDeserializer(inputStream);
        var deserializedItem = deserializer.deserializeDescriptionItem();

        assertEquals(originalItem, deserializedItem);
    }
}