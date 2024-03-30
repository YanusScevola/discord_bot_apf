package org.example.data.source.db;

import org.example.data.models.DebateModel;
import org.example.data.models.DebaterModel;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class DbOperations {
    private final Database db;

    public DbOperations() {
        this.db = Database.getInstance();
    }

    public CompletableFuture<Boolean> addDebater(DebaterModel debater) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + DbConstants.TABLE_APF_DEBATERS + " (" +
                    DbConstants.COLUMN_DEBATERS_ID + ", " +
                    DbConstants.COLUMN_DEBATERS_NICKNAME + ", " +
                    DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME + ", " +
                    DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + ", " +
                    DbConstants.COLUMN_DEBATERS_LOSSES + ", " +
                    DbConstants.COLUMN_DEBATERS_WINS + ") VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    DbConstants.COLUMN_DEBATERS_NICKNAME + " = VALUES(" + DbConstants.COLUMN_DEBATERS_NICKNAME + "), " +
                    DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME + " = VALUES(" + DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME + "), " +
                    DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + " = VALUES(" + DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + "), " +
                    DbConstants.COLUMN_DEBATERS_LOSSES + " = VALUES(" + DbConstants.COLUMN_DEBATERS_LOSSES + "), " +
                    DbConstants.COLUMN_DEBATERS_WINS + " = VALUES(" + DbConstants.COLUMN_DEBATERS_WINS + ");";
            try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
                pstmt.setLong(1, debater.getMemberId());
                pstmt.setString(2, debater.getNickname());
                pstmt.setString(3, debater.getServerNickname());
                pstmt.setString(4, convertListIdToString(debater.getDebatesIds()));
                pstmt.setInt(5, debater.getLossesDebatesCount());
                pstmt.setInt(6, debater.getWinnDebatesCount());
                pstmt.executeUpdate();
                resultFuture.complete(true);
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при добавлении или обновлении ApfDebater", e);
                System.err.println(e.getMessage());
                resultFuture.completeExceptionally(e);
            }
        });
        return resultFuture;
    }


    public CompletableFuture<Boolean> addDebaters(List<DebaterModel> debaters) {
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + DbConstants.TABLE_APF_DEBATERS + " (" +
                    DbConstants.COLUMN_DEBATERS_ID + ", " +
                    DbConstants.COLUMN_DEBATERS_NICKNAME + ", " +
                    DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME + ", " +
                    DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + ", " +
                    DbConstants.COLUMN_DEBATERS_LOSSES + ", " +
                    DbConstants.COLUMN_DEBATERS_WINS + ") VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    DbConstants.COLUMN_DEBATERS_NICKNAME + " = VALUES(" + DbConstants.COLUMN_DEBATERS_NICKNAME + "), " +
                    DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME + " = VALUES(" + DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME + "), " +
                    DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + " = VALUES(" + DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS + "), " +
                    DbConstants.COLUMN_DEBATERS_LOSSES + " = VALUES(" + DbConstants.COLUMN_DEBATERS_LOSSES + "), " +
                    DbConstants.COLUMN_DEBATERS_WINS + " = VALUES(" + DbConstants.COLUMN_DEBATERS_WINS + ");";
            try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
                for (DebaterModel debater : debaters) {
                    pstmt.setLong(1, debater.getMemberId());
                    pstmt.setString(2, debater.getNickname());
                    pstmt.setString(3, debater.getServerNickname());
                    pstmt.setString(4, convertListIdToString(debater.getDebatesIds()));
                    pstmt.setInt(5, debater.getLossesDebatesCount());
                    pstmt.setInt(6, debater.getWinnDebatesCount());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                resultFuture.complete(true);
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при добавлении или обновлении ApfDebater", e);
                System.err.println(e.getMessage());
                resultFuture.completeExceptionally(e);
            }
        });
        return resultFuture;
    }


    public CompletableFuture<List<DebaterModel>> getDebatersByMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            String placeholders = memberIds.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));
            String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATERS + " WHERE " +
                    DbConstants.COLUMN_DEBATERS_ID + " IN (" + placeholders + ");";
            List<DebaterModel> results = new ArrayList<>();
            try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
                int index = 1;
                for (Long id : memberIds) {
                    pstmt.setLong(index++, id);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        DebaterModel result = new DebaterModel(
                                rs.getLong(DbConstants.COLUMN_DEBATERS_ID),
                                rs.getString(DbConstants.COLUMN_DEBATERS_NICKNAME),
                                rs.getString(DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS)),
                                rs.getInt(DbConstants.COLUMN_DEBATERS_LOSSES),
                                rs.getInt(DbConstants.COLUMN_DEBATERS_WINS)
                        );
                        results.add(result);
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при получении списка ApfDebater", e);
                throw new CompletionException(e);
            }
            return results;
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные ApfDebater", e);
            return Collections.emptyList();
        });
    }

    public CompletableFuture<DebaterModel> getDebater(long debaterId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATERS + " WHERE " +
                    DbConstants.COLUMN_DEBATERS_ID + " = ?;";
            try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                statement.setLong(1, debaterId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return new DebaterModel(
                                rs.getLong(DbConstants.COLUMN_DEBATERS_ID),
                                rs.getString(DbConstants.COLUMN_DEBATERS_NICKNAME),
                                rs.getString(DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS)),
                                rs.getInt(DbConstants.COLUMN_DEBATERS_LOSSES),
                                rs.getInt(DbConstants.COLUMN_DEBATERS_WINS)
                        );
                    } else {
                        throw new CompletionException(new NoSuchElementException("Debater with ID " + debaterId + " not found"));
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при получении ApfDebater", e);
                throw new CompletionException(e);
            }
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные ApfDebater", e);
            return null;
        });
    }

    public CompletableFuture<DebateModel> addDebate(DebateModel debate) {
        CompletableFuture<DebateModel> futureResult = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + DbConstants.TABLE_APF_DEBATES + " (" +
                    DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + ", " +
                    DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + ", " +
                    DbConstants.COLUMN_DEBATES_DATE_TIME + ", " +
                    DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + ") VALUES (?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE " +
                    DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " = VALUES(" + DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + "), " +
                    DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " = VALUES(" + DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + "), " +
                    DbConstants.COLUMN_DEBATES_DATE_TIME + " = VALUES(" + DbConstants.COLUMN_DEBATES_DATE_TIME + "), " +
                    DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + " = VALUES(" + DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + ");";
            try (PreparedStatement statement = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, convertListIdToString(debate.getGovernmentMembersIds()));
                statement.setString(2, convertListIdToString(debate.getOppositionMembersIds()));
                statement.setTimestamp(3, Timestamp.valueOf(debate.getStartDateTime()));
                statement.setBoolean(4, debate.isGovernmentWinner());

                statement.executeUpdate();
                // Получаем сгенерированный id
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long debateId = generatedKeys.getLong(1);
                        debate.setId(debateId);
                        futureResult.complete(debate);
                    } else {
                        throw new SQLException("Creating debate failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при добавлении дебата", e);
                System.err.println(e.getMessage());
                futureResult.completeExceptionally(e);
            }
        });

        return futureResult;
    }

    public CompletableFuture<DebateModel> getDebate(long debateId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " WHERE " +
                    DbConstants.COLUMN_DEBATES_ID + " = ?;";
            try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                statement.setLong(1, debateId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return new DebateModel(
                                rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                        );
                    } else {
                        throw new CompletionException(new NoSuchElementException("Debate with ID " + debateId + " not found"));
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при получении ApfDebate", e);
                throw new CompletionException(e);
            }
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные ApfDebate", e);
            return null;
        });
    }

    public CompletableFuture<List<DebaterModel>> getAllDebaters() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATERS + ";";
            List<DebaterModel> results = new ArrayList<>();
            try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        DebaterModel result = new DebaterModel(
                                rs.getLong(DbConstants.COLUMN_DEBATERS_ID),
                                rs.getString(DbConstants.COLUMN_DEBATERS_NICKNAME),
                                rs.getString(DbConstants.COLUMN_DEBATERS_SERVER_NICKNAME),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATERS_APF_DEBATES_IDS)),
                                rs.getInt(DbConstants.COLUMN_DEBATERS_LOSSES),
                                rs.getInt(DbConstants.COLUMN_DEBATERS_WINS)
                        );
                        results.add(result);
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при получении списка ApfDebater", e);
                throw new CompletionException(e);
            }
            return results;
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные ApfDebater", e);
            return Collections.emptyList();
        });
    }

    public CompletableFuture<List<DebateModel>> getDebates(List<Long> debateIds) {
        if (debateIds == null || debateIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            String placeholders = debateIds.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));
            String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " WHERE " +
                    DbConstants.COLUMN_DEBATES_ID + " IN (" + placeholders + ");";
            List<DebateModel> results = new ArrayList<>();
            try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                int index = 1;
                for (Long id : debateIds) {
                    statement.setLong(index++, id);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        DebateModel result = new DebateModel(
                                rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                        );
                        results.add(result);
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при получении списка ApfDebate", e);
                throw new CompletionException(e);
            }
            return results;
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные ApfDebate", e);
            return Collections.emptyList();
        });
    }

    public CompletableFuture<List<DebateModel>> getDebatesByMemberId(long memberId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " WHERE " +
                    DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " LIKE ? OR " +
                    DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " LIKE ?;";
            List<DebateModel> results = new ArrayList<>();
            try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                statement.setString(1, "%" + memberId + "%");
                statement.setString(2, "%" + memberId + "%");
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        DebateModel result = new DebateModel(
                                rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                        );
                        results.add(result);
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при получении списка ApfDebate", e);
                throw new CompletionException(e);
            }
            return results;
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные ApfDebate", e);
            return Collections.emptyList();
        });
    }

    public CompletableFuture<List<DebateModel>> getDebatesByMemberId(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            String placeholders = memberIds.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));
            String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " WHERE " +
                    DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " IN (" + placeholders + ") OR " +
                    DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " IN (" + placeholders + ");";
            List<DebateModel> results = new ArrayList<>();
            try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                int index = 1;
                for (Long id : memberIds) {
                    statement.setLong(index++, id);
                }
                for (Long id : memberIds) {
                    statement.setLong(index++, id);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        DebateModel result = new DebateModel(
                                rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                        );
                        results.add(result);
                    }
                }
            } catch (SQLException e) {
                db.getLogger().debug("Ошибка при получении списка ApfDebate", e);
                throw new CompletionException(e);
            }
            return results;
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные ApfDebate", e);
            return Collections.emptyList();
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
