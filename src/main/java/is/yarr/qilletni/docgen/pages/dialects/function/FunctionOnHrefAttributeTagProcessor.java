package is.yarr.qilletni.docgen.pages.dialects.function;

import is.yarr.qilletni.api.lang.docs.structure.DocumentedItem;
import is.yarr.qilletni.api.lang.docs.structure.item.DocumentedTypeFunction;
import is.yarr.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import is.yarr.qilletni.docgen.pages.dialects.utility.AnchorFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * Creates a link to a function page.
 */
public class FunctionOnHrefAttributeTagProcessor extends AbstractAttributeTagProcessor {

    private static final String ATTR_NAME = "onlink";
    private static final int PRECEDENCE = 10000;

    protected FunctionOnHrefAttributeTagProcessor(String dialectPrefix) {
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
        if (!(executed instanceof DocumentedItem(DocumentedTypeFunction documentedFunction, FunctionDoc functionDoc))) {
            throw new RuntimeException("Expected a DocumentedTypeFunction, got " + executed);
        }

        if (functionDoc.docOnLine() == null) {
            // TODO: Lookup entity in all libraries if no on type ( @on[@type ..] ) is specified. Currently, just ignore and set # link
            structureHandler.setAttribute("href", "#");
            return;
        }

        var splitIdentifier = functionDoc.docOnLine().docFieldType().identifier().split("\\.");
        var libraryName = splitIdentifier[0];
        var entityName = splitIdentifier[1];

        var pageLinkText = "/library/%s/entity/%s.html".formatted(URLEncoder.encode(libraryName, Charset.defaultCharset()), URLEncoder.encode(entityName, Charset.defaultCharset()));

        structureHandler.setAttribute("href", "%s#%s".formatted(pageLinkText, AnchorFactory.createAnchorForFunction(documentedFunction)));
    }
}
