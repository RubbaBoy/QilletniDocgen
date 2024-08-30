module QilletniDocgen.main {
    requires thymeleaf;
    requires Qilletni.qilletni.api.main;
    requires is.yarr.qilletni.Qilletni.main;
    requires org.slf4j;
    requires unbescape;
    requires msgpack.core;
    
    exports is.yarr.qilletni.docgen;
}
