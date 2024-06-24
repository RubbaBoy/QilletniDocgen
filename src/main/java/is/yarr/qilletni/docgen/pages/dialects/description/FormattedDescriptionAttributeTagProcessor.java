package is.yarr.qilletni.docgen.pages.dialects.description;

import is.yarr.qilletni.api.lang.docs.structure.text.DocDescription;
import is.yarr.qilletni.docgen.pages.dialects.utility.LinkFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

public class FormattedDescriptionAttributeTagProcessor extends AbstractAttributeTagProcessor {


    private static final String ATTR_NAME = "desc";
    private static final int PRECEDENCE = 10000;


    protected FormattedDescriptionAttributeTagProcessor(String dialectPrefix) {
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
        
        if (executed == null) return;
        
        if (!(executed instanceof DocDescription docDescription)) {
            throw new RuntimeException("Expected a DocDescription, got " + executed);
        }

        var modelFactory = context.getModelFactory();
        var model = modelFactory.createModel();
        
        for (DocDescription.DescriptionItem descriptionItem : docDescription.descriptionItems()) {
            LinkFactory.createDescriptionLinkToType(modelFactory, model, descriptionItem);
        }

        structureHandler.setBody(model, false);
    }
    
    
}
