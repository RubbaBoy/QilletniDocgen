package dev.qilletni.docgen.pages.dialects.description;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class FormattedDocDialect extends AbstractProcessorDialect {

    public FormattedDocDialect() {
        super("FormattedDocDialect", "formatteddoc", 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new FormattedDescriptionAttributeTagProcessor(dialectPrefix));
    }
}
