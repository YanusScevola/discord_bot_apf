package org.example.data.source.db;

import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static Database instance;
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private Database() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String username = "u5068_AgJsVUDQAd";
            String password = "Q^@mZ7JmQlJ04L4oxHGshasT";
            String url = "jdbc:mysql://u5068_AgJsVUDQAd:Q%5E%40mZ7JmQlJ04L4oxHGshasT@172.105.158.16:3306/s5068_debate_club?autoReconnect=true";
            this.connection = DriverManager.getConnection(url, username, password);

            // Создание таблиц, если они еще не созданы
            createVersionTable();
            checkAndUpdateDatabaseVersion();

            createDebatersTable();
            createDebatesTable();
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

//        try {
        if (currentVersion < 1) {
            // Так как мы начинаем отсчет версий с 1, в реальности этот блок может не быть нужен,
            // если только вы не хотите обрабатывать случаи с непредвиденной инициализацией БД.
        }

        if (currentVersion < 2) {
            // Обновляем версию в базе данных.
//                updateDatabaseVersion(2);
        }

        if (currentVersion < 3) {
            // Обновляем версию в базе данных.
//               updateDatabaseVersion(3);
        }
//        } catch (SQLException e) {
//            logger.debug("Ошибка при выполнении миграции базы данных", e);
//        }
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
                        DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " TEXT, " +
                        DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " TEXT, " +
                        DbConstants.COLUMN_DEBATES_DATE_TIME + " TIMESTAMP, " +
                        DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + " BOOLEAN, " +
                        "PRIMARY KEY (" + DbConstants.COLUMN_DEBATES_ID + "));");
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

}
