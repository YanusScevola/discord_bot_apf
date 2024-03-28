package org.example.data.source.db;

import org.example.data.models.DebateModel;
import org.example.data.models.DebaterModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static Database instance;
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private Database() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String username = "u5068_vEssE0KzVg";
            String password = "EtBKaPUf5VtIAdco!a=OTOVk";
            String url = "jdbc:mysql://u5068_vEssE0KzVg:EtBKaPUf5VtIAdco!a%3DOTOVk@172.105.158.16:3306/s5068_debate_club";
            this.connection = DriverManager.getConnection(url, username, password);

            createDebatersTable();
            createDebatesTable();
        } catch (ClassNotFoundException | SQLException e) {
            logger.debug("Ошибка подключения к БД", e);
        }
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    private void createDebatersTable() {
        executeTableCreation(DBConstants.TABLE_APF_DEBATERS,
                "(" + DBConstants.COLUMN_DEBATERS_ID + " INT NOT NULL AUTO_INCREMENT, " +
                        DBConstants.COLUMN_DEBATERS_NICKNAME + " VARCHAR(255), " +
                        DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME + " VARCHAR(255), " +
                        DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + " TEXT, " +
                        DBConstants.COLUMN_DEBATERS_LOSSES + " INT DEFAULT 0, " +
                        DBConstants.COLUMN_DEBATERS_WINS + " INT DEFAULT 0, " +
                        "PRIMARY KEY (" + DBConstants.COLUMN_DEBATERS_ID + "));");
    }

    private void createDebatesTable() {
        executeTableCreation(DBConstants.TABLE_APF_DEBATES,
                "(" + DBConstants.COLUMN_DEBATES_ID + " INT NOT NULL AUTO_INCREMENT, " +
                        DBConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " TEXT, " +
                        DBConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " TEXT, " +
                        DBConstants.COLUMN_DEBATES_DATE_TIME + " TIMESTAMP, " +
                        DBConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + " BOOLEAN, " +
                        "PRIMARY KEY (" + DBConstants.COLUMN_DEBATES_ID + "));");
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

    public CompletableFuture<Boolean> addDebater(DebaterModel debater) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + DBConstants.TABLE_APF_DEBATERS + " (" +
                    DBConstants.COLUMN_DEBATERS_NICKNAME + ", " +
                    DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME + ", " +
                    DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + ", " +
                    DBConstants.COLUMN_DEBATERS_LOSSES + ", " +
                    DBConstants.COLUMN_DEBATERS_WINS + ") VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME + " = VALUES(" + DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME + "), " +
                    DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + " = VALUES(" + DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + "), " +
                    DBConstants.COLUMN_DEBATERS_LOSSES + " = VALUES(" + DBConstants.COLUMN_DEBATERS_LOSSES + "), " +
                    DBConstants.COLUMN_DEBATERS_WINS + " = VALUES(" + DBConstants.COLUMN_DEBATERS_WINS + ");";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, debater.getNickname());
                pstmt.setString(2, debater.getServerNickname());
                pstmt.setString(3, convertListIdToString(debater.getDebatesIds()));
                pstmt.setInt(4, debater.getLossesDebatesCount());
                pstmt.setInt(5, debater.getWinnDebatesCount());

                pstmt.executeUpdate();
                resultFuture.complete(true);
            } catch (SQLException e) {
                logger.debug("Ошибка при добавлении или обновлении ApfDebater", e);
                resultFuture.completeExceptionally(e);
            }
        });
        return resultFuture;
    }


    public CompletableFuture<Boolean> addDebaters(List<DebaterModel> debaters) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + DBConstants.TABLE_APF_DEBATERS + " (" +
                    DBConstants.COLUMN_DEBATERS_NICKNAME + ", " +
                    DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME + ", " +
                    DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + ", " +
                    DBConstants.COLUMN_DEBATERS_LOSSES + ", " +
                    DBConstants.COLUMN_DEBATERS_WINS + ") VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME + " = VALUES(" + DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME + "), " +
                    DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + " = VALUES(" + DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + "), " +
                    DBConstants.COLUMN_DEBATERS_LOSSES + " = VALUES(" + DBConstants.COLUMN_DEBATERS_LOSSES + "), " +
                    DBConstants.COLUMN_DEBATERS_WINS + " = VALUES(" + DBConstants.COLUMN_DEBATERS_WINS + ");";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (DebaterModel debater : debaters) {
                    pstmt.setString(1, debater.getNickname());
                    pstmt.setString(2, debater.getServerNickname());
                    pstmt.setString(3, convertListIdToString(debater.getDebatesIds()));
                    pstmt.setInt(4, debater.getLossesDebatesCount());
                    pstmt.setInt(5, debater.getWinnDebatesCount());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                resultFuture.complete(true);
            } catch (SQLException e) {
                logger.debug("Ошибка при добавлении или обновлении списка ApfDebater", e);
                System.err.println(e.getMessage());
                resultFuture.completeExceptionally(e);
            }
        });
        return resultFuture;
    }



    public CompletableFuture<Boolean> addDebate(DebateModel debate) {
        CompletableFuture<Boolean> futureResult = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            // Обратите внимание, что id больше не включается в список параметров.
            String sql = "INSERT INTO " + DBConstants.TABLE_APF_DEBATES + " (" +
                    DBConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + ", " +
                    DBConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + ", " +
                    DBConstants.COLUMN_DEBATES_DATE_TIME + ", " +
                    DBConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + ") VALUES (?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    DBConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " = VALUES(" + DBConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + "), " +
                    DBConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " = VALUES(" + DBConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + "), " +
                    DBConstants.COLUMN_DEBATES_DATE_TIME + " = VALUES(" + DBConstants.COLUMN_DEBATES_DATE_TIME + "), " +
                    DBConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + " = VALUES(" + DBConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + ");";
            try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                // Обновляем индексы параметров, так как больше не используем id в качестве параметра
                pstmt.setString(1, convertListIdToString(debate.getGovernmentMembersIds()));
                pstmt.setString(2, convertListIdToString(debate.getOppositionMembersIds()));
                pstmt.setTimestamp(3, Timestamp.valueOf(debate.getStartDateTime()));
                pstmt.setBoolean(4, debate.isGovernmentWinner());

                pstmt.executeUpdate();
                // Получаем сгенерированный id
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long debateId = generatedKeys.getLong(1);
                        System.out.println("Generated Debate ID: " + debateId); // Пример использования сгенерированного id
                    } else {
                        throw new SQLException("Creating debate failed, no ID obtained.");
                    }
                }
                futureResult.complete(true); // Успешное выполнение, возвращаем true
            } catch (SQLException e) {
                logger.debug("Ошибка при добавлении дебата", e);
                System.err.println(e.getMessage());
                futureResult.completeExceptionally(e); // В случае ошибки завершаем future с исключением
            }
        });

        return futureResult;
    }




    public CompletableFuture<DebaterModel> getDebater(long debaterId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + DBConstants.TABLE_APF_DEBATERS + " WHERE " +
                    DBConstants.COLUMN_DEBATERS_ID + " = ?;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, debaterId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new DebaterModel(
                                rs.getLong(DBConstants.COLUMN_DEBATERS_ID),
                                rs.getString(DBConstants.COLUMN_DEBATERS_NICKNAME),
                                rs.getString(DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME),
                                convertStringToListId(rs.getString(DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS)),
                                rs.getInt(DBConstants.COLUMN_DEBATERS_LOSSES),
                                rs.getInt(DBConstants.COLUMN_DEBATERS_WINS)
                        );
                    } else {
                        throw new CompletionException(new NoSuchElementException("Debater with ID " + debaterId + " not found"));
                    }
                }
            } catch (SQLException e) {
                logger.debug("Ошибка при получении ApfDebater", e);
                throw new CompletionException(e);
            }
        }).exceptionally(e -> {
            // Здесь может быть логика обработки исключений, например, возвращение null
            // или логирование ошибки
            logger.error("Не удалось получить данные ApfDebater", e);
            return null; // Возвращаем null или можно бросить unchecked исключение, если это приемлемо для логики приложения
        });
    }


    public CompletableFuture<DebateModel> getDebate(long debateId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + DBConstants.TABLE_APF_DEBATES + " WHERE " +
                    DBConstants.COLUMN_DEBATES_ID + " = ?;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, debateId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new DebateModel(
                                rs.getLong(DBConstants.COLUMN_DEBATES_ID),
                                convertStringToListId(rs.getString(DBConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                convertStringToListId(rs.getString(DBConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                rs.getTimestamp(DBConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                rs.getBoolean(DBConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                        );
                    } else {
                        throw new CompletionException(new NoSuchElementException("Debate with ID " + debateId + " not found"));
                    }
                }
            } catch (SQLException e) {
                logger.debug("Ошибка при получении ApfDebate", e);
                throw new CompletionException(e);
            }
        }).exceptionally(e -> {
            // Здесь может быть логика обработки исключений, например, возвращение null
            // или логирование ошибки
            logger.error("Не удалось получить данные ApfDebate", e);
            return null; // Возвращаем null или можно бросить unchecked исключение, если это приемлемо для логики приложения
        });
    }


    public CompletableFuture<List<DebateModel>> getDebates(List<Long> debateIds) {
        if (debateIds == null || debateIds.isEmpty()) {
            // Возвращаем CompletableFuture с пустым списком, если входной список пуст
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            String placeholders = debateIds.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));
            String sql = "SELECT * FROM " + DBConstants.TABLE_APF_DEBATES + " WHERE " +
                    DBConstants.COLUMN_DEBATES_ID + " IN (" + placeholders + ");";
            List<DebateModel> results = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int index = 1;
                for (Long id : debateIds) {
                    pstmt.setLong(index++, id);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        DebateModel result = new DebateModel(
                                rs.getLong(DBConstants.COLUMN_DEBATES_ID),
                                convertStringToListId(rs.getString(DBConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                convertStringToListId(rs.getString(DBConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                rs.getTimestamp(DBConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                rs.getBoolean(DBConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                        );
                        results.add(result);
                    }
                }
            } catch (SQLException e) {
                logger.debug("Ошибка при получении списка ApfDebate", e);
                throw new CompletionException(e); // Пробрасываем исключение
            }
            return results; // Возвращаем результаты
        }).exceptionally(e -> {
            // Обработка исключения, возможно логгирование
            logger.error("Не удалось получить данные ApfDebate", e);
            return Collections.emptyList(); // Возвращение пустого списка в случае ошибки
        });
    }



    public CompletableFuture<List<DebaterModel>> getAllDebaters() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + DBConstants.TABLE_APF_DEBATERS + ";";
            List<DebaterModel> results = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        DebaterModel result = new DebaterModel(
                                rs.getLong(DBConstants.COLUMN_DEBATERS_ID),
                                rs.getString(DBConstants.COLUMN_DEBATERS_NICKNAME),
                                rs.getString(DBConstants.COLUMN_DEBATERS_SERVER_NICKNAME),
                                convertStringToListId(rs.getString(DBConstants.COLUMN_DEBATERS_APF_DEBATES_IDS)),
                                rs.getInt(DBConstants.COLUMN_DEBATERS_LOSSES),
                                rs.getInt(DBConstants.COLUMN_DEBATERS_WINS)
                        );
                        results.add(result);
                    }
                }
            } catch (SQLException e) {
                logger.debug("Ошибка при получении списка ApfDebater", e);
                throw new CompletionException(e); // Пробрасываем исключение
            }
            return results; // Возвращаем результаты
        }).exceptionally(e -> {
            // Обработка исключения, возможно логгирование
            // Возвращение пустого списка или null, в зависимости от требований к обработке ошибок
            logger.error("Не удалось получить данные ApfDebater", e);
            return Collections.emptyList(); // Или возвращение null, если это приемлемо
        });
    }


    private String convertListIdToString(List<Long> list) {
        if (list == null) return null;
        return list.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    private List<Long> convertStringToListId(String data) {
        if (data == null || data.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(data.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

}
