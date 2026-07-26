package com.jmigrate.jmigrate_core.engine;

import com.jmigrate.jmigrate_core.model.MigrationScript;
import com.jmigrate.jmigrate_core.repository.SchemaHistoryRepository;
import com.jmigrate.jmigrate_core.util.ChecksumCalculator;
import com.jmigrate.jmigrate_core.util.FileScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JMigrateEngine {
    private static final Logger logger = LoggerFactory.getLogger(JMigrateEngine.class);

    private final String url;
    private final String user;
    private final String password;
    private final FileScanner fileScanner;
    private final SchemaHistoryRepository repository;

    public JMigrateEngine(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.fileScanner = new FileScanner();
        this.repository = new SchemaHistoryRepository();
    }

    public void migrate() {
        logger.info("Starting JMigrate database migration process...");

        try (Connection connection = DriverManager.getConnection(url, user, password)) {

            repository.createHistoryTableIfNotExists(connection);


            List<MigrationScript> availableScripts = fileScanner.scanMigrationFiles("db/migration");

            if (availableScripts.isEmpty()) {
                logger.info("No migration scripts found to execute.");
                return;
            }


            Map<Integer, String> executedMigrations = repository.getExecutedMigrations(connection);

            for (MigrationScript script : availableScripts) {

                String currentChecksum = ChecksumCalculator.calculateSHA256(script.getFile());
                script.setChecksum(currentChecksum);

                if (executedMigrations.containsKey(script.getVersion())) {
                    String oldChecksum = executedMigrations.get(script.getVersion());


                    if (!oldChecksum.equals(currentChecksum)) {
                        throw new RuntimeException("CRITICAL ERROR: Migration script " + script.getScriptName() +
                                " has been modified after execution! Old Hash: " + oldChecksum + ", New Hash: " + currentChecksum);
                    }

                    logger.info("Skipping V{} ({}) - Already applied.", script.getVersion(), script.getDescription());
                } else {

                    executeScriptWithTransaction(connection, script);
                }
            }

            logger.info("SUCCESS: All database migrations executed smoothly!");

        } catch (Exception e) {
            logger.error("MIGRATION FAILED! Cause: {}", e.getMessage(), e);
            throw new RuntimeException("Migration execution terminated.", e);
        }
    }

    private void executeScriptWithTransaction(Connection connection, MigrationScript script) throws Exception {
        logger.info("Executing migration: V{}__{}", script.getVersion(), script.getDescription());
        long startTime = System.currentTimeMillis();


        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (Statement statement = connection.createStatement();
             BufferedReader reader = new BufferedReader(new FileReader(script.getFile()))) {


            String sqlContent = reader.lines().collect(Collectors.joining("\n"));
            String[] rawQueries = sqlContent.split(";");

            for (String rawQuery : rawQueries) {
                String trimmedQuery = rawQuery.trim();
                if (!trimmedQuery.isEmpty()) {
                    statement.execute(trimmedQuery);
                }
            }

            long executionTime = System.currentTimeMillis() - startTime;


            repository.recordMigration(connection, script, executionTime);


            connection.commit();
            logger.info("Successfully applied V{} in {} ms", script.getVersion(), executionTime);

        } catch (Exception e) {

            connection.rollback();
            logger.error("Failed to execute V{}. Rolled back all changes from this script!", script.getVersion());
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}