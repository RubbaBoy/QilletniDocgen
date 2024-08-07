package is.yarr.qilletni.docgen;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws IOException {
        var docParser = DocParserFactory.createDocParser("spotify", Paths.get("E:\\Qilletni\\qilletni-lib-std\\qilletni-src"), Paths.get("doc-cache"));
        
        docParser.createIndexFile();
        docParser.createEntityFiles();
        docParser.writeToCache();
        
//        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
//        templateResolver.setSuffix(".html");
//
//        TemplateEngine templateEngine = new TemplateEngine();
//        templateEngine.setTemplateResolver(templateResolver);
//
//        Context context = new Context();
//        context.setVariable("message", "Hello from static site generator!");
//
//        String output = templateEngine.process("templates/index", context);
//        Files.createDirectories(Paths.get("output"));
//        try (FileWriter writer = new FileWriter("output/index.html")) {
//            writer.write(output);
//        }
    }
}
