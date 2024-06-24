package is.yarr.qilletni.docgen.pages.dialects.function;

import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;

import java.net.URLEncoder;
import java.nio.charset.Charset;

public class FunctionAnchorFactory {
    
    public static String createAnchorForFunction(DocumentedTypeFunction documentedFunction) {
        var body = "%sfun %s(%s)%s".formatted(
                documentedFunction.isNative() ? "native " : "",
                documentedFunction.name(),
                String.join(", ", documentedFunction.params()),
                documentedFunction.onOptional().map(" on %s"::formatted).orElse("")
        );

        return URLEncoder.encode(body, Charset.defaultCharset());
    }
    
}
