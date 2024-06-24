package is.yarr.qilletni.docgen.pages.dialects.utility;

import is.yarr.qilletni.api.lang.docs.structure.DocFieldType;
import is.yarr.qilletni.api.lang.docs.structure.text.DocDescription;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementTagStructureHandler;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkFactory {
    
    private static final String JAVA_DOC_BASE_URL = "https://docs.oracle.com/en/java/javase/21/docs/api/";
    private static final Map<String, String> MODULE_MAP = new HashMap<>();
    private static final List<String> NATIVE_QILLETNI_TYPES = List.of("int", "string", "boolean", "collection", "song", "album", "java", "list");
    
    public static void createDescriptionLinkToType(IModelFactory modelFactory, IModel model, DocDescription.DescriptionItem descriptionItem) {
        switch (descriptionItem) {
            case DocDescription.DocText docText -> model.add(modelFactory.createText(docText.text()));
            case DocDescription.JavaRef javaRef -> {
                model.add(modelFactory.createOpenElementTag("a", Map.of("href", generateJavadocLink(javaRef.javaName()), "target", "_blank"), AttributeValueQuotes.DOUBLE, false));
                model.add(modelFactory.createText(" %s ".formatted(getNameFromClass(javaRef.javaName()))));
                model.add(modelFactory.createCloseElementTag("a"));
            }
            case DocDescription.ParamRef paramRef -> {
                model.add(modelFactory.createOpenElementTag("i"));
                model.add(modelFactory.createText(" %s ".formatted(URLEncoder.encode(paramRef.paramName(), Charset.defaultCharset()))));
                model.add(modelFactory.createCloseElementTag("i"));
            }
            case DocDescription.TypeRef typeRef -> {
                if (isNativeType(typeRef.typeName())) {
                    model.add(modelFactory.createOpenElementTag("a"));
                } else {
                    model.add(modelFactory.createOpenElementTag("a", "href", generateQilletniTypeLink(typeRef.typeName())));
                }
                
                model.add(modelFactory.createText(" %s ".formatted(URLEncoder.encode(getNameFromQilletniType(typeRef.typeName()), Charset.defaultCharset()))));
                model.add(modelFactory.createCloseElementTag("a"));
            }
        }
    }
    
    public static void populateTypeLink(IElementTagStructureHandler structureHandler, DocFieldType docFieldType) {
        var identifier = docFieldType.identifier();
        
        switch (docFieldType.fieldType()) {
            case QILLETNI -> {
                if (!isNativeType(identifier)) {
                    structureHandler.setAttribute("href", generateQilletniTypeLink(identifier));
                }
                
                structureHandler.setBody(" %s ".formatted(URLEncoder.encode(getNameFromQilletniType(identifier), Charset.defaultCharset())), false);
            }
            case JAVA -> {
                structureHandler.setAttribute("href", generateJavadocLink(identifier));
                structureHandler.setAttribute("target", "_blank");
                structureHandler.setBody(" %s ".formatted(URLEncoder.encode(getNameFromClass(identifier), Charset.defaultCharset())), false);
            }
        }
    }

    private static String getNameFromClass(String fullyQualifiedClassName) {
        var split = fullyQualifiedClassName.split("\\.");
        return split[split.length - 1];
    }

    private static String generateJavadocLink(String fullyQualifiedClassName) {
        if (fullyQualifiedClassName == null || fullyQualifiedClassName.isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }

        // Extract package name
        int lastDotIndex = fullyQualifiedClassName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("Invalid fully qualified class name");
        }

        var packageName = fullyQualifiedClassName.substring(0, lastDotIndex);

        // Determine module name
        String moduleName = MODULE_MAP.get(packageName);
        if (moduleName == null) {
            throw new IllegalArgumentException("No module found for package: " + packageName);
        }

        // Replace dots with slashes and append .html
        var path = fullyQualifiedClassName.replace('.', '/');

        return JAVA_DOC_BASE_URL + moduleName + "/" + path + ".html";
    }

    private static String getNameFromQilletniType(String qilletniType) {
        var split = qilletniType.split("\\.");
        return split[split.length - 1];
    }

    private static String generateQilletniTypeLink(String qilletniType) {
        var split = qilletniType.split("\\.");
        System.out.println("split = " + Arrays.toString(split));
        return "/library/%s/entity/%s.html".formatted(split[0], split[1]);
    }
    
    private static boolean isNativeType(String qilletniType) {
        return NATIVE_QILLETNI_TYPES.contains(qilletniType);
    }

    static {
        // Core Java Modules
        MODULE_MAP.put("java.lang", "java.base");
        MODULE_MAP.put("java.io", "java.base");
        MODULE_MAP.put("java.math", "java.base");
        MODULE_MAP.put("java.net", "java.base");
        MODULE_MAP.put("java.nio", "java.base");
        MODULE_MAP.put("java.text", "java.base");
        MODULE_MAP.put("java.time", "java.base");
        MODULE_MAP.put("java.util", "java.base");
        MODULE_MAP.put("java.security", "java.base");
        MODULE_MAP.put("java.sql", "java.sql");
        MODULE_MAP.put("javax.sql", "java.sql");
        MODULE_MAP.put("java.xml", "java.xml");
        MODULE_MAP.put("javax.xml", "java.xml");
        MODULE_MAP.put("java.beans", "java.desktop");
        MODULE_MAP.put("java.awt", "java.desktop");
        MODULE_MAP.put("javax.swing", "java.desktop");
        MODULE_MAP.put("java.rmi", "java.rmi");
        MODULE_MAP.put("java.management", "java.management");
        MODULE_MAP.put("javax.management", "java.management");
        MODULE_MAP.put("javax.annotation.processing", "java.compiler");
        MODULE_MAP.put("javax.lang.model", "java.compiler");
        MODULE_MAP.put("javax.tools", "java.compiler");
        MODULE_MAP.put("javax.crypto", "java.base");
        MODULE_MAP.put("javax.net", "java.base");
        MODULE_MAP.put("javax.naming", "java.naming");
        MODULE_MAP.put("javax.script", "java.scripting");
        MODULE_MAP.put("javafx.application", "javafx.graphics");
        MODULE_MAP.put("javafx.scene", "javafx.graphics");
        MODULE_MAP.put("javafx.stage", "javafx.graphics");
        MODULE_MAP.put("javafx.animation", "javafx.graphics");
        MODULE_MAP.put("javafx.beans", "javafx.base");
        MODULE_MAP.put("javafx.concurrent", "javafx.base");
        MODULE_MAP.put("javafx.css", "javafx.base");
        MODULE_MAP.put("javafx.event", "javafx.base");
        MODULE_MAP.put("javafx.geometry", "javafx.base");
        MODULE_MAP.put("javafx.util", "javafx.base");
        MODULE_MAP.put("javafx.fxml", "javafx.fxml");
        MODULE_MAP.put("javafx.scene.chart", "javafx.controls");
        MODULE_MAP.put("javafx.scene.control", "javafx.controls");
        MODULE_MAP.put("javafx.scene.effect", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.image", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.input", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.layout", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.media", "javafx.media");
        MODULE_MAP.put("javafx.scene.paint", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.shape", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.text", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.transform", "javafx.graphics");
        MODULE_MAP.put("javafx.scene.web", "javafx.web");
        MODULE_MAP.put("javax.net.ssl", "java.base");
        MODULE_MAP.put("javax.xml.bind", "java.xml.bind");
        MODULE_MAP.put("javax.xml.crypto", "java.xml.crypto");
        MODULE_MAP.put("javax.xml.parsers", "java.xml");
        MODULE_MAP.put("javax.xml.soap", "java.xml.soap");
        MODULE_MAP.put("javax.xml.transform", "java.xml");
        MODULE_MAP.put("javax.xml.validation", "java.xml");
        MODULE_MAP.put("javax.xml.xpath", "java.xml");
        MODULE_MAP.put("java.nio.file", "java.base");
        MODULE_MAP.put("java.lang.invoke", "java.base");
        MODULE_MAP.put("java.lang.reflect", "java.base");
        MODULE_MAP.put("java.util.concurrent", "java.base");
        MODULE_MAP.put("java.util.logging", "java.base");
        MODULE_MAP.put("java.util.prefs", "java.prefs");
        MODULE_MAP.put("java.util.regex", "java.base");
        MODULE_MAP.put("java.util.spi", "java.base");
        MODULE_MAP.put("java.util.stream", "java.base");
        MODULE_MAP.put("java.util.zip", "java.base");
        MODULE_MAP.put("javax.annotation", "java.base");
        MODULE_MAP.put("javax.lang.model.element", "java.compiler");
        MODULE_MAP.put("javax.lang.model.type", "java.compiler");
        MODULE_MAP.put("javax.lang.model.util", "java.compiler");
        MODULE_MAP.put("javax.rmi", "java.rmi");
        MODULE_MAP.put("javax.rmi.ssl", "java.rmi");
        MODULE_MAP.put("javax.sound.midi", "java.desktop");
        MODULE_MAP.put("javax.sound.sampled", "java.desktop");
        MODULE_MAP.put("javax.sql.rowset", "java.sql.rowset");
        MODULE_MAP.put("javax.sql.rowset.serial", "java.sql.rowset");
        MODULE_MAP.put("javax.sql.rowset.spi", "java.sql.rowset");
        MODULE_MAP.put("javax.swing.event", "java.desktop");
        MODULE_MAP.put("javax.swing.plaf", "java.desktop");
        MODULE_MAP.put("javax.swing.plaf.basic", "java.desktop");
        MODULE_MAP.put("javax.swing.plaf.metal", "java.desktop");
        MODULE_MAP.put("javax.swing.plaf.multi", "java.desktop");
        MODULE_MAP.put("javax.swing.plaf.synth", "java.desktop");
        MODULE_MAP.put("javax.swing.table", "java.desktop");
        MODULE_MAP.put("javax.swing.text", "java.desktop");
        MODULE_MAP.put("javax.swing.text.html", "java.desktop");
        MODULE_MAP.put("javax.swing.text.html.parser", "java.desktop");
        MODULE_MAP.put("javax.swing.text.rtf", "java.desktop");
        MODULE_MAP.put("javax.swing.tree", "java.desktop");
        MODULE_MAP.put("javax.swing.undo", "java.desktop");
        MODULE_MAP.put("javax.transaction.xa", "java.transaction.xa");
        MODULE_MAP.put("javax.transaction", "java.transaction");
        MODULE_MAP.put("javax.validation.constraints", "java.validation");
        MODULE_MAP.put("javax.validation.groups", "java.validation");
        MODULE_MAP.put("javax.validation.metadata", "java.validation");
        MODULE_MAP.put("javax.validation", "java.validation");
        MODULE_MAP.put("javax.xml.bind.annotation", "java.xml.bind");
        MODULE_MAP.put("javax.xml.bind.attachment", "java.xml.bind");
        MODULE_MAP.put("javax.xml.bind.helpers", "java.xml.bind");
        MODULE_MAP.put("javax.xml.bind.util", "java.xml.bind");
        MODULE_MAP.put("javax.xml.crypto.dom", "java.xml.crypto");
        MODULE_MAP.put("javax.xml.crypto.dsig", "java.xml.crypto");
        MODULE_MAP.put("javax.xml.crypto.dsig.dom", "java.xml.crypto");
        MODULE_MAP.put("javax.xml.crypto.dsig.keyinfo", "java.xml.crypto");
        MODULE_MAP.put("javax.xml.crypto.dsig.spec", "java.xml.crypto");
        MODULE_MAP.put("javax.xml.stream", "java.xml");
        MODULE_MAP.put("javax.xml.stream.events", "java.xml");
        MODULE_MAP.put("javax.xml.stream.util", "java.xml");
        MODULE_MAP.put("javax.xml.transform.dom", "java.xml");
        MODULE_MAP.put("javax.xml.transform.sax", "java.xml");
        MODULE_MAP.put("javax.xml.transform.stax", "java.xml");
        MODULE_MAP.put("javax.xml.transform.stream", "java.xml");
    }
    
}
