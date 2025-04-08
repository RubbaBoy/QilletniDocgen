package dev.qilletni.docgen.pages.dialects.file;

import dev.qilletni.api.lang.docs.structure.DocumentedFile;
import dev.qilletni.docgen.pages.dialects.utility.AnchorFactory;
import dev.qilletni.docgen.pages.filetree.FileNode;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.net.URLEncoder;
import java.nio.charset.Charset;

public class FileHrefAttributeTagProcessor extends AbstractAttributeTagProcessor {

    private static final String ATTR_NAME = "link";
    private static final int PRECEDENCE = 10000;
    private final String libraryName;

    protected FileHrefAttributeTagProcessor(String dialectPrefix, String libraryName) {
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
        if (!(executed instanceof FileNode fileNode)) {
            throw new RuntimeException("Expected a FileNode, got " + executed);
        }

        var linkText = getSourceFileUrl(libraryName, AnchorFactory.createAnchorForSourceFile(fileNode));

        structureHandler.setAttribute("href", linkText);
    }

    public static String getSourceFileUrl(String libraryName, String fileHref) {
        return "/library/%s/file/%s".formatted(URLEncoder.encode(libraryName, Charset.defaultCharset()), URLEncoder.encode(fileHref, Charset.defaultCharset()));
    }
}
