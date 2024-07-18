package is.yarr.qilletni.docgen.cache.serializer;

import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedType;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.DocDescription;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.InnerDoc;

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
