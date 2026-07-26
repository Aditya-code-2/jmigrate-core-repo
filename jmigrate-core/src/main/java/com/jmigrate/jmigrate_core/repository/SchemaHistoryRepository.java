package com.jmigrate.jmigrate_core.repository;

import com.jmigrate.jmigrate_core.model.MigrationScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SchemaHistoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(SchemaHistoryRepository.class);
    private static final String HISTORY_TABLE = "schema_version_history";


    public void createHistoryTableIfNotExists(Connection connection) throws SQLException {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS %s (
                installed_rank INT AUTO_INCREMENT PRIMARY KEY,
                version INT NOT NULL UNIQUE,
                description VARCHAR(200) NOT NULL,
                script VARCHAR(100) NOT NULL,
                checksum VARCHAR(64) NOT NULL,
                installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                execution_time INT NOT NULL,
                success TINYINT(1) NOT NULL
            );
        """.formatted(HISTORY_TABLE);

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
            logger.info("Schema history table '{}' is ready.", HISTORY_TABLE);
        }
    }


    public Map<Integer, String> getExecutedMigrations(Connection connection) throws SQLException {
        Map<Integer, String> executedScripts = new HashMap<>();
        String query = "SELECT version, checksum FROM " + HISTORY_TABLE + " WHERE success = 1";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                executedScripts.put(rs.getInt("version"), rs.getString("checksum"));
            }
        }
        return executedScripts;
    }


    public void recordMigration(Connection connection, MigrationScript script, long executionTimeMs) throws SQLException {
        String insertSql = """
            INSERT INTO %s (version, description, script, checksum, execution_time, success)
            VALUES (?, ?, ?, ?, ?, 1)
        """.formatted(HISTORY_TABLE);

        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setInt(1, script.getVersion());
            pstmt.setString(2, script.getDescription());
            pstmt.setString(3, script.getScriptName());
            pstmt.setString(4, script.getChecksum());
            pstmt.setLong(5, executionTimeMs);
            pstmt.executeUpdate();
        }
    }
}