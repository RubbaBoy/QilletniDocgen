package dev.qilletni.docgen.cache.serializer;

import dev.qilletni.api.lang.docs.structure.item.DocumentedType;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.api.lang.docs.structure.text.DocDescription;
import dev.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.InnerDoc;

public class SerializationUtility {

    public static int getDescriptionItemIndex(DocDescription.DescriptionItem descriptionItem) {
        return switch (descriptionItem) {
            case DocDescription.DocText $ -> 0;
            case DocDescription.JavaRef $ -> 1;
            case DocDescription.ParamRef $ -> 2;
            case DocDescription.TypeRef $ -> 3;
        };
    }

    public static int getDocumentedTypeIndex(DocumentedType documentedType) {
        return switch (documentedType) {
            case DocumentedTypeEntity $ -> 0;
            case DocumentedTypeEntityConstructor $ -> 1;
            case DocumentedTypeField $ -> 2;
            case DocumentedTypeFunction $ -> 3;
        };
    }

    public static int getInnerDocIndex(InnerDoc innerDoc) {
        return switch (innerDoc) {
            case ConstructorDoc $ -> 0;
            case EntityDoc $ -> 1;
            case FieldDoc $ -> 2;
            case FunctionDoc $ -> 3;
        };
    }
}
