package dev.qilletni.docgen.pages.filetree;

import java.util.ArrayList;
import java.util.List;

public record FileNode(String name, String currentPath, boolean directory, List<FileNode> children) {
    public FileNode(String name, String currentPath, boolean directory) {
        this(name, currentPath, directory, new ArrayList<>());
    }

    public void addChild(FileNode child) {
        children.add(child);
    }
}
