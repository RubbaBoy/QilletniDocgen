package is.yarr.qilletni.docgen.pages.index;

import org.thymeleaf.context.IContext;

import java.util.Locale;
import java.util.Set;

public class IndexPageContext implements IContext {
    
    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public boolean containsVariable(String name) {
        return false;
    }

    @Override
    public Set<String> getVariableNames() {
        return Set.of();
    }

    @Override
    public Object getVariable(String name) {
        return null;
    }
}
