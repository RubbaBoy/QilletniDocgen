package dev.qilletni.docgen.pages.dialects.file;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class FileDialect extends AbstractProcessorDialect {

    private final String libraryName;

    public FileDialect(String libraryName) {
        super("FileDialect", "file", 1000);
        this.libraryName = libraryName;
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new FileHrefAttributeTagProcessor(dialectPrefix, libraryName));
    }
}

