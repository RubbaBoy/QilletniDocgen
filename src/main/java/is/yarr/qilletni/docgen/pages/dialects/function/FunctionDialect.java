package is.yarr.qilletni.docgen.pages.dialects.function;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.Set;

public class FunctionDialect extends AbstractProcessorDialect {
    
    public FunctionDialect() {
        super("FunctionDialect", "function", 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new FunctionSignatureAttributeTagProcessor(dialectPrefix),
                new FunctionHrefAttributeTagProcessor(dialectPrefix),
                new FunctionFromExtensionAttributeTagProcessor(dialectPrefix),
                new FunctionOnHrefAttributeTagProcessor(dialectPrefix),
                new FunctionParamTypeAttributeTagProcessor(dialectPrefix),
                new FunctionAnchorAttributeTagProcessor(dialectPrefix));
    }
}
