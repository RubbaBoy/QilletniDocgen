package is.yarr.qilletni.docgen.pages.dialects.function;

import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;
import org.unbescape.html.HtmlEscape;

/**
 * Creates an unformatted text representation of a function signature.
 */
public class FunctionSignatureAttributeTagProcessor extends AbstractAttributeTagProcessor {

    private static final String ATTR_NAME = "signature";
    private static final int PRECEDENCE = 10000;

    protected FunctionSignatureAttributeTagProcessor(String dialectPrefix) {
        super(
                TemplateMode.HTML, // This processor will apply only to HTML mode
                dialectPrefix,     // Prefix to be applied to name for matching
                null,              // No tag name: match any tag name
                false,             // No prefix to be applied to tag name
                ATTR_NAME,         // Name of the attribute that will be matched
                true,              // Apply dialect prefix to attribute name
                PRECEDENCE,        // Precedence (inside dialect's precedence)
                true);             // Remove the matched attribute afterwards
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
                             AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler) {

        var expressionParser = StandardExpressions.getExpressionParser(context.getConfiguration());
        var expr = expressionParser.parseExpression(context, attributeValue);
        
        var executed = expr.execute(context);
        if (!(executed instanceof DocumentedTypeFunction documentedFunction)) {
            throw new RuntimeException("Expected a DocumentedTypeFunction, got " + executed.getClass().getCanonicalName());
        }
        
        structureHandler.setBody(HtmlEscape.escapeHtml5(getFunctionSignature(documentedFunction)), false);
    }
    
    // TODO: Move out?
    public static String getFunctionSignature(DocumentedTypeFunction documentedFunction) {
        return "%s%sfun %s(%s)%s".formatted(
                documentedFunction.isNative() ? "native " : "",
                documentedFunction.isStatic() ? "static " : "",
                documentedFunction.name(),
                String.join(", ", documentedFunction.params()),
                documentedFunction.onOptional().map(" on %s"::formatted).orElse("")
        );
    }
}
