package com.jmigrate.jmigrate_core.model;

import java.io.File;

public class MigrationScript implements Comparable<MigrationScript> {
    private final int version;
    private final String description;
    private final String scriptName;
    private final File file;
    private String checksum;

    public MigrationScript(int version, String description, String scriptName, File file) {
        this.version = version;
        this.description = description;
        this.scriptName = scriptName;
        this.file = file;
    }

    public int getVersion() { return version; }
    public String getDescription() { return description; }
    public String getScriptName() { return scriptName; }
    public File getFile() { return file; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    @Override
    public int compareTo(MigrationScript other) {
        return Integer.compare(this.version, other.version);
    }

    @Override
    public String toString() {
        return String.format("MigrationScript{V%d__%s, scriptName='%s', checksum='%s'}",
                version, description, scriptName, checksum);
    }
}