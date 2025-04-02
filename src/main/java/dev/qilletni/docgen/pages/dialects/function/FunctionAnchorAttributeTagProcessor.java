package dev.qilletni.docgen.pages.dialects.function;

import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import dev.qilletni.docgen.pages.dialects.utility.AnchorFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Creates a link to a function page.
 */
public class FunctionAnchorAttributeTagProcessor extends AbstractAttributeTagProcessor {

    private static final String ATTR_NAME = "anchor";
    private static final int PRECEDENCE = 10000;

    protected FunctionAnchorAttributeTagProcessor(String dialectPrefix) {
        super(
                TemplateMode.HTML, // This processor will apply only to HTML mode
                dialectPrefix,     // Prefix to be applied to name for matching
                null,               // No tag name: match any tag name
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
            throw new RuntimeException("Expected a DocumentedTypeFunction, got " + executed);
        }
        
        structureHandler.setAttribute("id", AnchorFactory.createAnchorForFunction(documentedFunction));
    }
}
