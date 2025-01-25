package is.yarr.qilletni.docgen.markdown;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import is.yarr.qilletni.docgen.pages.dialects.utility.LinkFactory;

import java.util.Collections;
import java.util.Set;

public class MarkdownParser {
    
    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownParser(Parser parser, HtmlRenderer renderer) {
        this.parser = parser;
        this.renderer = renderer;
    }

    public static MarkdownParser createMarkdownParser() {
        var options = new MutableDataSet();

        options.set(Parser.HEADING_PARSER, false); // Disables headers like # Header
        options.set(HtmlRenderer.ESCAPE_HTML_BLOCKS, true);
        options.set(HtmlRenderer.ESCAPE_INLINE_HTML, true);
        options.set(HtmlRenderer.HARD_BREAK, "<br/>");
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>");

        var parser = Parser.builder(options).build();
        var renderer = HtmlRenderer.builder(options)
                .nodeRendererFactory(_ -> new TargetBlankLinkRenderer())
                .build();
        
        return new MarkdownParser(parser, renderer);
    }
    
    public String renderMarkdown(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    static class TargetBlankLinkRenderer implements NodeRenderer {
        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            return Collections.singleton(new NodeRenderingHandler<>(Link.class, this::renderLink));
        }

        private void renderLink(Link node, NodeRendererContext context, HtmlWriter html) {
            var url = node.getUrl().toString();

            // Add target="_blank" only for only javadoc links
            if (url.contains(LinkFactory.JAVA_DOC_BASE_URL)) {
                html.attr("href", url);
                html.attr("target", "_blank"); // Add target="_blank"
                html.srcPos(node.getChars()).withAttr().tag("a");
                context.renderChildren(node); // Render the link text
                html.tag("/a");
            } else {
                // Default rendering for other links
                context.delegateRender();
            }
        }
    }
    
}
