package is.yarr.qilletni.docgen.pages.dialects.description;

import is.yarr.qilletni.api.lang.docs.structure.text.DocDescription;
import is.yarr.qilletni.docgen.markdown.MarkdownParser;
import is.yarr.qilletni.docgen.pages.dialects.utility.LinkFactory;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class MessageCreator {

    private final MarkdownParser markdownParser;
    
    private final IModelFactory modelFactory;
    private final IModel model;
    private final List<DocDescription.DescriptionItem> descriptionItems;

    public MessageCreator(IModelFactory modelFactory, IModel model, List<DocDescription.DescriptionItem> descriptionItems) {
        this.modelFactory = modelFactory;
        this.model = model;
        this.descriptionItems = descriptionItems;
        
        this.markdownParser = MarkdownParser.createMarkdownParser();
    }
    
    public void processMessage() {
        var stringMessage = convertDescriptionItemsToString(descriptionItems);
        var renderedHtml = markdownParser.renderMarkdown(stringMessage);
        model.add(modelFactory.createText(renderedHtml));
    }

    /**
     * Converts the message to string, so it can be fully handled by the markdown processor.
     * 
     * @return The string message
     */
    public static String convertDescriptionItemsToString(List<DocDescription.DescriptionItem> descriptionItems) {
        return descriptionItems.stream().map(descriptionItem ->
                switch (descriptionItem) {
                    case DocDescription.DocText docText -> removeSingleNewlines(docText.text());
                    case DocDescription.JavaRef javaRef ->
                            " [%s](%s) ".formatted(LinkFactory.getNameFromClass(javaRef.javaName()), LinkFactory.generateJavadocLink(javaRef.javaName()));
                    case DocDescription.ParamRef paramRef ->
                            " `%s` ".formatted(URLEncoder.encode(paramRef.paramName(), Charset.defaultCharset()));
                    case DocDescription.TypeRef typeRef ->
                            " [%s](%s) ".formatted(URLEncoder.encode(LinkFactory.getNameFromQilletniType(typeRef.typeName()), Charset.defaultCharset()), LinkFactory.generateQilletniTypeLink(typeRef.typeName()));
                }).collect(Collectors.joining());
    }

    private static String removeSingleNewlines(String input) {
        return input.replaceAll("(?<!\\n)\\n(?!\\n)", " ");
    }
}
