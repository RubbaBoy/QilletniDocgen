package dev.qilletni.docgen.pages.dialects.utility;

import dev.qilletni.api.lang.docs.structure.DocumentedFile;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeField;
import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.docgen.pages.filetree.FileNode;

import java.net.URLEncoder;
import java.nio.charset.Charset;

public class AnchorFactory {

    public static String createAnchorForFunction(DocumentedTypeFunction documentedFunction) {
        var body = "%s%sfun %s(%s)%s".formatted(
                documentedFunction.isNative() ? "native " : "",
                documentedFunction.isStatic() ? "static " : "",
                documentedFunction.name(),
                String.join(", ", documentedFunction.params()),
                documentedFunction.onOptional().map(" on %s"::formatted).orElse("")
        );

        return URLEncoder.encode(body, Charset.defaultCharset());
    }

    public static String createAnchorForConstructor(DocumentedTypeEntityConstructor documentedEntityConstructor) {
        var body = "%s(%s)".formatted(
                documentedEntityConstructor.name(),
                String.join(", ", documentedEntityConstructor.params())
        );

        return URLEncoder.encode(body, Charset.defaultCharset());
    }

    public static String createAnchorForField(DocumentedTypeField documentedField) {
        var body = "%s %s".formatted(
                documentedField.type(),
                documentedField.name()
        );

        return URLEncoder.encode(body, Charset.defaultCharset());
    }
    
    public static String createAnchorForSourceFile(DocumentedFile documentedFile) {
        return "%s".formatted(documentedFile.importPath().toString().replace("\\", "/").replace("/", "_"));
    }
    
    public static String createAnchorForSourceFile(FileNode fileNode) {
        return "%s".formatted(fileNode.currentPath().replace("\\", "/").replace("/", "_"));
    }
    
}
