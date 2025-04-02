package dev.qilletni.docgen.cache;

import dev.qilletni.api.lang.docs.structure.DocumentedItem;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;

/**
 * A function a part of a class due to it being "on" it. This may come from another file or library, so this object
 * holds that info along with the typical doc info.
 */
public class ReferencedOnFunction {

    private final String library;
    private final String filePath;
    private final DocumentedTypeFunction documentedFunction;
    private final FunctionDoc functionDoc;

    public ReferencedOnFunction(String library, String filePath, DocumentedItem documentedItem) {
        if (!(documentedItem instanceof DocumentedItem(DocumentedTypeFunction documentedFunctionItem, FunctionDoc functionDocItem))) {
            throw new IllegalArgumentException("DocumentedItem must be representing a function");
        }
        
        this.library = library;
        this.filePath = filePath;
        this.documentedFunction = documentedFunctionItem;
        this.functionDoc = functionDocItem;
    }
    
    public static boolean isReferencedOnFunction(DocumentedItem documentedItem) {
        return documentedItem instanceof DocumentedItem(DocumentedTypeFunction $, FunctionDoc $$);
    }

    public String getLibrary() {
        return library;
    }

    public String getFilePath() {
        return filePath;
    }

    public DocumentedTypeFunction getDocumentedFunction() {
        return documentedFunction;
    }

    public FunctionDoc getFunctionDoc() {
        return functionDoc;
    }

    @Override
    public String toString() {
        return "ReferencedOnFunction{" +
                "library='" + library + '\'' +
                ", filePath='" + filePath + '\'' +
                ", documentedFunction=" + documentedFunction +
                ", functionDoc=" + functionDoc +
                '}';
    }
}
