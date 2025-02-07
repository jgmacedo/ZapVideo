package com.john.zapvideo.services;

import java.io.File;

public class UniqueDirectory {
    private final File directory;
    private final String uuid;

    public UniqueDirectory(File directory, String uuid) {
        this.directory = directory;
        this.uuid = uuid;
    }

    public File getDirectory() {
        return directory;
    }

    public String getUuid() {
        return uuid;
    }
}