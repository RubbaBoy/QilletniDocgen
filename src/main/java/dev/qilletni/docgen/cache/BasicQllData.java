package dev.qilletni.docgen.cache;

import dev.qilletni.api.lib.qll.QilletniInfoData;

public record BasicQllData(String name, String version, String author, String description, String sourceUrl) {
    public BasicQllData(QilletniInfoData libraryQll) {
        this(libraryQll.name(), libraryQll.version().getVersionString(), libraryQll.author(), libraryQll.description(), libraryQll.sourceUrl());
    }
}
