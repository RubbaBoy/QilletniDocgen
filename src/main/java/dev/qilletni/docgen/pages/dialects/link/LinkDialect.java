package dev.qilletni.docgen.pages.dialects.link;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class LinkDialect extends AbstractProcessorDialect {

    public LinkDialect() {
        super("LinkDialect", "link", 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new HtmlSuffixAttributeTagProcessor(dialectPrefix),
                new HtmlIndexAttributeTagProcessor(dialectPrefix));
    }
}

