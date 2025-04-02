package dev.qilletni.docgen.pages.dialects.constructor;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class ConstructorDialect extends AbstractProcessorDialect {
    
    public ConstructorDialect() {
        super("ConstructorDialect", "constructor", 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new ConstructorSignatureAttributeTagProcessor(dialectPrefix),
                new ConstructorAnchorAttributeTagProcessor(dialectPrefix),
                new ConstructorHrefAttributeTagProcessor(dialectPrefix));
    }
}
