package dev.qilletni.docgen.index;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import dev.qilletni.api.lang.docs.structure.text.inner.ConstructorDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.EntityDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FieldDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.FunctionDoc;
import dev.qilletni.api.lang.docs.structure.text.inner.InnerDoc;
import dev.qilletni.docgen.pages.dialects.description.MessageCreator;

public class DescriptionFormatter {
    
    private final Parser parser;

    public DescriptionFormatter() {
        this.parser = Parser.builder().build();;
    }

    /**
     * Gets a string description of the given {@link InnerDoc}, without any markdown formatting. If no doc is found,
     * return an empty string.
     *
     * @param innerDoc The documentation to strip markdown from
     * @return The string doc
     */
    public String getPlainDescription(InnerDoc innerDoc) {
        var docDescription = switch (innerDoc) {
            case ConstructorDoc constructorDoc -> constructorDoc.description();
            case EntityDoc entityDoc -> entityDoc.description();
            case FieldDoc fieldDoc -> fieldDoc.description();
            case FunctionDoc functionDoc -> functionDoc.description();
        };

        if (docDescription == null) {
            return "";
        }

        var markdownString = MessageCreator.convertDescriptionItemsToString(docDescription.descriptionItems());

        var node = parser.parse(markdownString);
        var textVisitor = new TextCollectingVisitor();
        return textVisitor.collectAndGetText(node);
    }
    
    public String getShortPlainDescription(InnerDoc innerDoc, int length) {
        var plainDescription = getPlainDescription(innerDoc);
        if (plainDescription.length() > length) {
            return plainDescription.substring(0, length) + "...";
        }
        
        return plainDescription;
    }
}
