package dev.qilletni.docgen.pages;

import dev.qilletni.docgen.cache.BasicQllData;
import dev.qilletni.docgen.cache.serializer.DocumentationDeserializer;
import dev.qilletni.docgen.pages.dialects.link.LinkDialect;
import dev.qilletni.docgen.pages.dialects.utility.AnchorFactory;
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

        var context = new Context();
        context.setVariable("libraries", cachedLibraries);
        context.setVariable("HTML_SUFFIX", AnchorFactory.HTML_SUFFIX);

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
        templateEngine.addDialect(new LinkDialect());
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }
}
