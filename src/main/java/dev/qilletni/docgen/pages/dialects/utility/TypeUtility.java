package dev.qilletni.docgen.pages.dialects.utility;

import dev.qilletni.api.lang.docs.structure.DocumentedItem;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TypeUtility {

    public static final List<String> NATIVE_QILLETNI_TYPES = List.of("int", "double", "string", "boolean", "collection", "song", "album", "list", "java", "function");

    private static final Map<String, String> NATIVE_TYPE_DESCRIPTIONS = Map.of(
            "int", "An integer value, up to 64 bits.",
            "double", "A double precision floating point value, up to 64 bits.",
            "string", "A character string.",
            "boolean", "A true/false value.",
            "collection", "A playlist or collection of songs",
            "song", "A provider-agnostic piece of music that can be interacted with through service providers.",
            "album", "A provider-agnostic musical album that can be interacted with through service providers.",
            "list", "A dynamic collection of types.",
            "java", "A reference to a Java object, for interacting with native methods."
    );

    public static boolean isNativeType(String qilletniType) {
        return NATIVE_QILLETNI_TYPES.contains(qilletniType);
    }
    
    public static String getNativeTypeDescription(String nativeType) {
        return NATIVE_TYPE_DESCRIPTIONS.getOrDefault(nativeType, "A Qilletni native type.");
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
