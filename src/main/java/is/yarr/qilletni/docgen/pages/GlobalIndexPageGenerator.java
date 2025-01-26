package is.yarr.qilletni.docgen.pages;

import is.yarr.qilletni.docgen.cache.BasicQllData;
import is.yarr.qilletni.docgen.cache.serializer.DocumentationDeserializer;
import is.yarr.qilletni.docgen.pages.dialects.constructor.ConstructorDialect;
import is.yarr.qilletni.docgen.pages.dialects.description.FormattedDocDialect;
import is.yarr.qilletni.docgen.pages.dialects.entity.EntityDialect;
import is.yarr.qilletni.docgen.pages.dialects.function.FunctionDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GlobalIndexPageGenerator {


    private final Path cachePath;
    private final Path outputPath;

    public GlobalIndexPageGenerator(Path cachePath, Path outputPath) {
        this.cachePath = cachePath;
        this.outputPath = outputPath;
    }

    public void generateIndex() throws IOException {
        var cachedLibraries = new ArrayList<>(fetchCachedLibraries());
        cachedLibraries.addAll(cachedLibraries);

        var context = new Context();
        context.setVariable("libraries", cachedLibraries);

        var templateEngine = createTemplateEngine();

        String output = templateEngine.process("templates/index.html", context);

        try (var writer = new FileWriter(outputPath.resolve("index.html").toFile())) {
            writer.write(output);
        }
    }
    
    private List<BasicQllData> fetchCachedLibraries() {
        try (var walk = Files.list(cachePath)) {
            return walk.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".cache"))
                    .map(file -> {
                        try (var documentationDeserializer = new DocumentationDeserializer(Files.newInputStream(file))) {
                            return documentationDeserializer.deserializeBasicQllData();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }
}
