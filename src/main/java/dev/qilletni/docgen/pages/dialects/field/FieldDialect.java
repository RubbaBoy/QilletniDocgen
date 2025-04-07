package dev.qilletni.docgen.pages.dialects.field;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class FieldDialect  extends AbstractProcessorDialect {

    public FieldDialect() {
        super("FieldDialect", "field", 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new FieldHrefAttributeTagProcessor(dialectPrefix),
                new FieldSignatureAttributeTagProcessor(dialectPrefix),
                new FieldAnchorAttributeTagProcessor(dialectPrefix));
    }
}
