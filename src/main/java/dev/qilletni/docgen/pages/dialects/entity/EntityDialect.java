package dev.qilletni.docgen.pages.dialects.entity;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class EntityDialect extends AbstractProcessorDialect {

    private final String libraryName;

    public EntityDialect(String libraryName) {
        super("EntityDialect", "entity", 1000);
        this.libraryName = libraryName;
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new EntityHrefAttributeTagProcessor(dialectPrefix, libraryName));
    }
}
