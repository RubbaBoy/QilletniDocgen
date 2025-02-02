module qilletni.docgen {
    requires qilletni.impl;
    requires qilletni.api;
    
    requires thymeleaf;
    requires org.slf4j;
    requires unbescape;
    requires msgpack.core;
    requires flexmark.util.data;
    requires flexmark;
    requires flexmark.util.ast;
    requires com.google.gson;

    exports is.yarr.qilletni.docgen;
}
