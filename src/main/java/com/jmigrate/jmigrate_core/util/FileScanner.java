package com.jmigrate.jmigrate_core.util;

import com.jmigrate.jmigrate_core.model.MigrationScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileScanner {
    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);

    // Pattern matcher for filenames like: V1__create_users_table.sql
    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("^V(\\d+)__([a-zA-Z0-9_]+)\\.sql$");

    public List<MigrationScript> scanMigrationFiles(String locationPath) {
        List<MigrationScript> scripts = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resourceUrl = classLoader.getResource(locationPath);

        if (resourceUrl == null) {
            logger.warn("Directory '{}' not found in classpath resources!", locationPath);
            return scripts;
        }

        File folder = new File(resourceUrl.getFile());
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".sql"));

        if (files == null || files.length == 0) {
            logger.info("No migration SQL files found in '{}'", locationPath);
            return scripts;
        }

        for (File file : files) {
            Matcher matcher = MIGRATION_FILE_PATTERN.matcher(file.getName());
            if (matcher.matches()) {
                int version = Integer.parseInt(matcher.group(1));
                String rawDescription = matcher.group(2);
                String description = rawDescription.replace("_", " ");

                MigrationScript script = new MigrationScript(version, description, file.getName(), file);
                scripts.add(script);
            } else {
                logger.warn("Skipping file '{}': Invalid naming format! Must be 'V<Number>__<description>.sql'", file.getName());
            }
        }

        // Automatic version sorting (V1 -> V2 -> V3)
        Collections.sort(scripts);
        logger.info("Discovered {} valid migration script(s).", scripts.size());

        return scripts;
    }
}