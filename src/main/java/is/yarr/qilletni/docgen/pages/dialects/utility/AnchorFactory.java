package is.yarr.qilletni.docgen.pages.dialects.utility;

import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;

import java.net.URLEncoder;
import java.nio.charset.Charset;

public class AnchorFactory {

    public static String createAnchorForFunction(DocumentedTypeFunction documentedFunction) {
        var body = "%sfun %s(%s)%s".formatted(
                documentedFunction.isNative() ? "native " : "",
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
    
}
