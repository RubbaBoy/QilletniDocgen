package dev.qilletni.docgen.pages.dialects.entity;

import dev.qilletni.api.lang.docs.structure.item.DocumentedTypeEntity;
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
 * Creates a link to an entity page.
 */
public class EntityHrefAttributeTagProcessor extends AbstractAttributeTagProcessor {

    private static final String ATTR_NAME = "link";
    private static final int PRECEDENCE = 10000;
    private final String libraryName;

    protected EntityHrefAttributeTagProcessor(String dialectPrefix, String libraryName) {
        super(
                TemplateMode.HTML, // This processor will apply only to HTML mode
                dialectPrefix,     // Prefix to be applied to name for matching
                null,               // No tag name: match any tag name
                false,             // No prefix to be applied to tag name
                ATTR_NAME,         // Name of the attribute that will be matched
                true,              // Apply dialect prefix to attribute name
                PRECEDENCE,        // Precedence (inside dialect's precedence)
                true);             // Remove the matched attribute afterwards
        this.libraryName = libraryName;
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
                             AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler) {

        var expressionParser = StandardExpressions.getExpressionParser(context.getConfiguration());
        var expr = expressionParser.parseExpression(context, attributeValue);

        var executed = expr.execute(context);
        if (!(executed instanceof DocumentedTypeEntity documentedEntity)) {
            throw new RuntimeException("Expected a DocumentedTypeEntity, got " + executed.getClass().getCanonicalName());
        }
        
        var linkText = getEntityUrl(libraryName, documentedEntity.name());
        
        structureHandler.setAttribute("href", linkText);
    }
    
    public static String getLibraryUrl(String libraryName) {
        return "/library/%s/".formatted(URLEncoder.encode(libraryName, Charset.defaultCharset()));
    }
    
    public static String getEntityUrl(String libraryName, String entityName) {
        return "/library/%s/entity/%s".formatted(URLEncoder.encode(libraryName, Charset.defaultCharset()), URLEncoder.encode(entityName, Charset.defaultCharset()));
    }
}
