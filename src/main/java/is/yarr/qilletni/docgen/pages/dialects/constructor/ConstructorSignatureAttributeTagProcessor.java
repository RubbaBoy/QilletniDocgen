package is.yarr.qilletni.docgen.pages.dialects.constructor;

import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeEntityConstructor;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;
import org.unbescape.html.HtmlEscape;

/**
 * Creates an unformatted text representation of a function signature.
 */
public class ConstructorSignatureAttributeTagProcessor extends AbstractAttributeTagProcessor {

    private static final String ATTR_NAME = "signature";
    private static final int PRECEDENCE = 10000;

    protected ConstructorSignatureAttributeTagProcessor(String dialectPrefix) {
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
        if (!(executed instanceof DocumentedTypeEntityConstructor documentedEntityConstructor)) {
            throw new RuntimeException("Expected a DocumentedTypeFunction, got " + executed.getClass().getCanonicalName());
        }

        var body = "%s(%s)".formatted(
                documentedEntityConstructor.name(),
                String.join(", ", documentedEntityConstructor.params())
        );
        
        structureHandler.setBody(HtmlEscape.escapeHtml5(body), false);
    }
}
