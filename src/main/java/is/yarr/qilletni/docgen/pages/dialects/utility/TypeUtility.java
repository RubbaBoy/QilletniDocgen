package is.yarr.qilletni.docgen.pages.dialects.utility;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;

import java.util.List;
import java.util.Objects;

public class TypeUtility {

    public static final List<String> NATIVE_QILLETNI_TYPES = List.of("int", "string", "boolean", "collection", "song", "album", "list", "java");

    public static boolean isNativeType(String qilletniType) {
        return NATIVE_QILLETNI_TYPES.contains(qilletniType);
    }
    
    public static OnStatusInfo getOnStatus(DocumentedItem documentedFunctionItem) {
        if (documentedFunctionItem instanceof DocumentedItem(
                DocumentedTypeFunction documentedTypeFunction, FunctionDoc functionDoc
        )) {
            if (functionDoc.docOnLine() != null && functionDoc.docOnLine().docFieldType() != null) {
                var split = functionDoc.docOnLine().docFieldType().identifier().split("\\.");
                return new OnStatusInfo(OnStatus.ON_ENTITY, split[0], split[1]);
            }

            var nativeType = documentedTypeFunction.onOptional().orElse("");
            if (TypeUtility.isNativeType(nativeType)) { // Keep if on native library
                return new OnStatusInfo(OnStatus.ON_NATIVE, "std", nativeType);
            }
        }
        
        return new OnStatusInfo(OnStatus.NONE);
    }
    
    public record OnStatusInfo(OnStatus onStatus, String libraryName, String entityName) {
        public OnStatusInfo(OnStatus onStatus) {
            this(onStatus, "", "");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OnStatusInfo that = (OnStatusInfo) o;
            return Objects.equals(libraryName, that.libraryName);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(libraryName);
        }
    }
    
    public enum OnStatus {
        ON_ENTITY,
        ON_NATIVE,
        NONE
    }
    
}
