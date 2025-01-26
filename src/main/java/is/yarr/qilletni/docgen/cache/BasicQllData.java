package is.yarr.qilletni.docgen.cache;

import is.yarr.qilletni.api.lib.qll.QilletniInfoData;

public record BasicQllData(String name, String version, String author, String description) {
    public BasicQllData(QilletniInfoData libraryQll) {
        this(libraryQll.name(), libraryQll.version().getVersionString(), libraryQll.author(), libraryQll.description());
    }
}
