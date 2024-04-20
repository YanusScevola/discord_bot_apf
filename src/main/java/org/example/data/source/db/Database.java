package org.example.data.source.db;

import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static Database instance;
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static final String USER_NAME = "u7213_rrNI7cZ0CU";
    private static final String PASSWORD = "!G8!w@TSBdMcY1lx2PJm8H7V";
    private static final String URL = "jdbc:mysql://u7213_rrNI7cZ0CU:!G8!w%40TSBdMcY1lx2PJm8H7V@172.105.158.16:3306/s7213_debate_apf_db";

    private Database() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER_NAME, PASSWORD);

            createVersionTable();
            createThemesTable();
            createDebatersTable();
            createDebatesTable();
            createTestsTable();

            checkAndUpdateDatabaseVersion();
        } catch (ClassNotFoundException | SQLException e) {
            logger.debug("Ошибка подключения к БД", e);
        }
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public Logger getLogger() {
        return logger;
    }

    private void createVersionTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DbConstants.TABLE_DB_VERSION + " (" + DbConstants.COLUMN_VERSION + " INT)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            // Инициализация версии, если таблица только что была создана
            if (getDatabaseVersion() == -1) {
                updateDatabaseVersion(1); // Установите начальную версию вашей схемы БД
            }
        } catch (SQLException e) {
            logger.debug("Ошибка создания таблицы версий", e);
        }
    }

    private void createThemesTable() {
        executeTableCreation(DbConstants.TABLE_THEMES,
                "(" + DbConstants.COLUMN_THEMES_ID + " INT NOT NULL AUTO_INCREMENT, " +
                        DbConstants.COLUMN_THEMES_NAME + " VARCHAR(255), " +
                        DbConstants.COLUMN_THEMES_USAGE_COUNT + " INT DEFAULT 0, " +
                        "PRIMARY KEY (" + DbConstants.COLUMN_THEMES_ID + "));");
    }

    private void createDebatersTable() {
        executeTableCreation(DbConstants.TABLE_APF_DEBATERS,
                "(" + DbConstants.COLUMN_DEBATERS_ID + " BIGINT NOT NULL, " +
                        DbConstants.COLUMN_DEBATERS_NICKNAME + " VARCHAR(255), " +
                        DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME + " VARCHAR(255), " +
                        DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + " TEXT, " +
                        DbConstants.COLUMN_DEBATERS_LOSSES + " INT DEFAULT 0, " +
                        DbConstants.COLUMN_DEBATERS_WINS + " INT DEFAULT 0, " +
                        "PRIMARY KEY (" + DbConstants.COLUMN_DEBATERS_ID + "));");
    }

    private void createDebatesTable() {
        executeTableCreation(DbConstants.TABLE_APF_DEBATES,
                "(" + DbConstants.COLUMN_DEBATES_ID + " BIGINT NOT NULL AUTO_INCREMENT, " +
                        DbConstants.COLUMN_DEBATES_THEME_ID + " INT, " +
                        DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " TEXT, " +
                        DbConstants.COLUMN_DEBATES_JUDGES_IDS + " TEXT, " +
                        DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " TEXT, " +
                        DbConstants.COLUMN_DEBATES_DATE_TIME + " TIMESTAMP, " +
                        DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + " BOOLEAN, " +
                        "PRIMARY KEY (" + DbConstants.COLUMN_DEBATES_ID + "));");
    }

    private void createTestsTable() {
        executeTableCreation(DbConstants.TABLE_TESTS,
                "(" + DbConstants.COLUMN_TESTS_ID + " INT NOT NULL AUTO_INCREMENT, " +
                        DbConstants.COLUMN_TESTS_QUESTION + " VARCHAR(255), " +
                        DbConstants.COLUMN_TESTS_ANSWER_1 + " VARCHAR(255), " +
                        DbConstants.COLUMN_TESTS_ANSWER_2 + " VARCHAR(255), " +
                        DbConstants.COLUMN_TESTS_ANSWER_3 + " VARCHAR(255), " +
                        DbConstants.COLUMN_TESTS_ANSWER_4 + " VARCHAR(255), " +
                        DbConstants.COLUMN_TESTS_CORRECT_ANSWER + " VARCHAR(255), " +
                        "PRIMARY KEY (" + DbConstants.COLUMN_TESTS_ID + "));");
    }

    private void executeTableCreation(String tableName, String tableDefinition) {
        if (connection != null) {
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " " + tableDefinition;
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                logger.debug("Ошибка создания таблицы " + tableName, e);
            }
        } else {
            logger.debug("Ошибка подключения к БД");
        }
    }

    private int getDatabaseVersion() {
        String sql = "SELECT " + DbConstants.COLUMN_VERSION + " FROM " + DbConstants.TABLE_DB_VERSION
                + " ORDER BY " + DbConstants.COLUMN_VERSION
                + " DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(DbConstants.COLUMN_VERSION);
            } else {
                // Если в таблице нет записей, считаем версию -1
                return -1;
            }
        } catch (SQLException e) {
            logger.debug("Ошибка при получении версии базы данных", e);
            return -1;
        }
    }

    private void updateDatabaseVersion(int newVersion) {
        // Удаляем все существующие записи из таблицы версий.
        String deleteSql = "DELETE FROM " + DbConstants.TABLE_DB_VERSION;
        // Вставляем новую версию в таблицу версий.
        String insertSql = "INSERT INTO " + DbConstants.TABLE_DB_VERSION + " (" + DbConstants.COLUMN_VERSION + ") VALUES (?)";

        try (Statement deleteStmt = connection.createStatement()) {
            // Выполнение запроса на удаление.
            deleteStmt.executeUpdate(deleteSql);

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                // Установка параметров и выполнение запроса на вставку.
                insertStmt.setInt(1, newVersion);
                insertStmt.executeUpdate();
                logger.debug("Версия базы данных обновлена до: " + newVersion);
            } catch (SQLException e) {
                logger.debug("Ошибка при вставке новой версии базы данных", e);
            }
        } catch (SQLException e) {
            logger.debug("Ошибка при удалении старых версий из таблицы " + DbConstants.COLUMN_VERSION, e);
        }
    }

    private void checkAndUpdateDatabaseVersion() {
        int currentVersion = getDatabaseVersion();

        if (currentVersion < DbConstants.VERSION) {
            updateDatabaseVersion(DbConstants.VERSION);
        }

        if (currentVersion < 2) {
            // Обновляем версию в базе данных.
//                updateDatabaseVersion(2);
        }

        if (currentVersion < 3) {
            // Обновляем версию в базе данных.
//               updateDatabaseVersion(3);
        }
    }

    private void dropTable(String tableName) {
        if (connection != null) {
            String sql = "DROP TABLE IF EXISTS " + tableName;
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                logger.debug("Ошибка удаления таблицы " + tableName, e);
            }
        } else {
            logger.debug("Ошибка подключения к БД");
        }
    }

    public CompletableFuture<Boolean> executeWithConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed() || !connection.isValid(5)) {
                    return reconnect().get();
                }
                return true;
            } catch (SQLException | InterruptedException | ExecutionException e) {
                System.err.println("Ошибка при проверке соединения с базой данных или при попытке переподключения");
                logger.debug("Ошибка при проверке соединения с базой данных или при попытке переподключения", e);
                return false;
            } catch (Exception e) {
                System.err.println("Ошибка при попытке переподключения к базе данных");
                logger.debug("Ошибка при попытке переподключения к базе данных", e);
                return false;
            }
        });
    }

    CompletableFuture<Boolean> reconnect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                connection = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
                System.out.println("Соединение с базой данных успешно восстановлено.");
                logger.debug("Соединение с базой данных успешно восстановлено.");
                return true;
            } catch (SQLException e) {
                System.err.println("Ошибка при попытке восстановить соединение с базой данных");
                logger.debug("Ошибка при попытке восстановить соединение с базой данных", e);
                return false;
            }
        });
    }

//    private void reconnect() throws SQLException {
//        connection = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
//        logger.debug("Соединение с базой данных успешно восстановлено.");
//    }


}
