package dev.qilletni.docgen.pages.filetree;

import java.util.ArrayList;
import java.util.List;

public record FileNode(String name, boolean directory, List<FileNode> children) {
    public FileNode(String name, boolean directory) {
        this(name, directory, new ArrayList<>());
    }

    public void addChild(FileNode child) {
        children.add(child);
    }
}
