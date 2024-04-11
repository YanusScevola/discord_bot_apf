package org.example.data.source.db;

        import org.example.data.models.DebateModel;
        import org.example.data.models.DebaterModel;
        import org.example.data.models.QuestionModel;
        import org.example.data.models.ThemeModel;

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
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                CompletableFuture<Boolean> failedFuture = new CompletableFuture<>();
                failedFuture.complete(false);
                return failedFuture;
            } else {
                return CompletableFuture.supplyAsync(() -> {
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
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        statement.setLong(1, debater.getMemberId());
                        statement.setString(2, debater.getNickname());
                        statement.setString(3, debater.getServerNickname());
                        statement.setString(4, convertListIdToString(debater.getDebatesIds()));
                        statement.setInt(5, debater.getLossesDebatesCount());
                        statement.setInt(6, debater.getWinnDebatesCount());
                        statement.executeUpdate();
                        return true;
                    } catch (SQLException e) {
                        System.err.println("Ошибка при добавлении дебатера: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
            }
        });
    }

    public CompletableFuture<Boolean> addDebaters(List<DebaterModel> debaters) {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(false);
            } else {
                return CompletableFuture.supplyAsync(() -> {
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
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        for (DebaterModel debater : debaters) {
                            statement.setLong(1, debater.getMemberId());
                            statement.setString(2, debater.getNickname());
                            statement.setString(3, debater.getServerNickname());
                            statement.setString(4, convertListIdToString(debater.getDebatesIds()));
                            statement.setInt(5, debater.getLossesDebatesCount());
                            statement.setInt(6, debater.getWinnDebatesCount());
                            statement.addBatch();
                        }
                        statement.executeBatch();
                        return true;
                    } catch (SQLException e) {
                        System.err.println("Ошибка при добавлении дебатеров: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
            }
        });
    }

    public CompletableFuture<ThemeModel> getTheme(long themeId) {
        // Проверяем и восстанавливаем соединение
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                // Если соединение не удалось восстановить, возвращаем CompletableFuture, завершённый исключением
                CompletableFuture<ThemeModel> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new SQLException("Не удалось восстановить соединение с базой данных"));
                return failedFuture;
            } else {
                // Если соединение успешно, выполняем запрос на получение темы
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "SELECT * FROM " + DbConstants.TABLE_THEMES + " WHERE " +
                            DbConstants.COLUMN_THEMES_ID + " = ?;";
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        statement.setLong(1, themeId);
                        try (ResultSet rs = statement.executeQuery()) {
                            if (rs.next()) {
                                return new ThemeModel(
                                        rs.getInt(DbConstants.COLUMN_THEMES_ID),
                                        rs.getString(DbConstants.COLUMN_THEMES_NAME),
                                        rs.getInt(DbConstants.COLUMN_THEMES_USAGE_COUNT)
                                );
                            } else {
                                throw new CompletionException(new NoSuchElementException("Theme with ID " + themeId + " not found"));
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Ошибка при получении темы: " + e.getMessage());
                        throw new CompletionException(e);
                    }
                });
            }
        }).exceptionally(e -> {
            System.err.println(e.getMessage());
            return null;
        });
    }


    public CompletableFuture<List<ThemeModel>> getThemes(List<Integer> themeIds) {
        if (themeIds == null || themeIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    String placeholders = themeIds.stream()
                            .map(id -> "?")
                            .collect(Collectors.joining(","));
                    String sql = "SELECT * FROM " + DbConstants.TABLE_THEMES + " WHERE " +
                            DbConstants.COLUMN_THEMES_ID + " IN (" + placeholders + ");";
                    List<ThemeModel> results = new ArrayList<>();
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        int index = 1;
                        for (Integer id : themeIds) {
                            statement.setInt(index++, id);
                        }
                        try (ResultSet rs = statement.executeQuery()) {
                            while (rs.next()) {
                                ThemeModel result = new ThemeModel(
                                        rs.getInt(DbConstants.COLUMN_THEMES_ID),
                                        rs.getString(DbConstants.COLUMN_THEMES_NAME),
                                        rs.getInt(DbConstants.COLUMN_THEMES_USAGE_COUNT)
                                );
                                results.add(result);
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Ошибка при получении списка тем: " + e.getMessage());
                        return Collections.<ThemeModel>emptyList();
                    }
                    return results;
                });
            }
        });
    }

    public CompletableFuture<ThemeModel> getRandomTheme() {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                // Если соединение не удалось восстановить, возвращаем CompletableFuture, завершённый исключением
                CompletableFuture<ThemeModel> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new SQLException("Не удалось восстановить соединение с базой данных"));
                return failedFuture;
            } else {
                // Если соединение успешно восстановлено, выполняем запрос на получение случайной темы
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "SELECT * FROM " + DbConstants.TABLE_THEMES + " ORDER BY " + DbConstants.COLUMN_THEMES_USAGE_COUNT + " ASC, RAND() LIMIT 1;";
                    try (Statement statement = db.getConnection().createStatement();
                         ResultSet rs = statement.executeQuery(sql)) {
                        if (rs.next()) {
                            return new ThemeModel(
                                    rs.getInt(DbConstants.COLUMN_THEMES_ID),
                                    rs.getString(DbConstants.COLUMN_THEMES_NAME),
                                    rs.getInt(DbConstants.COLUMN_THEMES_USAGE_COUNT)
                            );
                        } else {
                            throw new CompletionException(new NoSuchElementException("No themes found"));
                        }
                    } catch (SQLException e) {
                        db.getLogger().debug("Ошибка при получении случайной темы", e);
                        throw new CompletionException(e);
                    }
                });
            }
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные случайной темы", e);
            return null; // Лучше возвращать Optional.empty() для избежания null, если это возможно
        });
    }

    public CompletableFuture<Boolean> addTheme(ThemeModel theme) {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                CompletableFuture<Boolean> failedFuture = new CompletableFuture<>();
                failedFuture.complete(false);
                return failedFuture;
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "INSERT INTO " + DbConstants.TABLE_THEMES + " (" +
                            DbConstants.COLUMN_THEMES_ID + ", " +
                            DbConstants.COLUMN_THEMES_NAME + ", " +
                            DbConstants.COLUMN_THEMES_USAGE_COUNT + ") VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE " +
                            DbConstants.COLUMN_THEMES_NAME + " = VALUES(" + DbConstants.COLUMN_THEMES_NAME + "), " +
                            DbConstants.COLUMN_THEMES_USAGE_COUNT + " = VALUES(" + DbConstants.COLUMN_THEMES_USAGE_COUNT + ");";
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        statement.setInt(1, theme.getId());
                        statement.setString(2, theme.getName());
                        statement.setInt(3, theme.getUsageCount());
                        statement.executeUpdate();
                        return true;
                    } catch (SQLException e) {
                        System.err.println("Ошибка при добавлении темы: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
            }
        });
    }


    public CompletableFuture<List<DebaterModel>> getDebatersByMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
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
                                        // Предполагается, что convertStringToListId корректно реализован
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
                });
            }
        });
    }

    public CompletableFuture<DebaterModel> getDebater(long debaterId) {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                // Если соединение не удалось восстановить, возвращаем CompletableFuture, завершённый исключением
                CompletableFuture<DebaterModel> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new SQLException("Не удалось восстановить соединение с базой данных"));
                return failedFuture;
            } else {
                // Если соединение успешно восстановлено, выполняем запрос на получение информации о дебатере
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
                                throw new NoSuchElementException("Debater with ID " + debaterId + " not found");
                            }
                        }
                    } catch (SQLException e) {
                        db.getLogger().error("Ошибка при получении Debater", e);
                        throw new RuntimeException(e);
                    }
                });
            }
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные Debater", e);
            return null; // Возвращение null здесь допустимо, но рекомендуется использовать Optional<DebaterModel>
        });
    }

    public CompletableFuture<DebateModel> addDebate(DebateModel debate) {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                CompletableFuture<DebateModel> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new SQLException("Не удалось восстановить соединение с базой данных"));
                return failedFuture;
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "INSERT INTO " + DbConstants.TABLE_APF_DEBATES + " (" +
                            DbConstants.COLUMN_DEBATES_THEME_ID + ", " +
                            DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + ", " +
                            DbConstants.COLUMN_DEBATES_JUDGES_IDS + ", " +
                            DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + ", " +
                            DbConstants.COLUMN_DEBATES_DATE_TIME + ", " +
                            DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + ") VALUES (?, ?, ?, ?, ?, ?)" +
                            " ON DUPLICATE KEY UPDATE " +
                            DbConstants.COLUMN_DEBATES_THEME_ID + " = VALUES(" + DbConstants.COLUMN_DEBATES_THEME_ID + "), " +
                            DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " = VALUES(" + DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + "), " +
                            DbConstants.COLUMN_DEBATES_JUDGES_IDS + " = VALUES(" + DbConstants.COLUMN_DEBATES_JUDGES_IDS + "), " +
                            DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " = VALUES(" + DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + "), " +
                            DbConstants.COLUMN_DEBATES_DATE_TIME + " = VALUES(" + DbConstants.COLUMN_DEBATES_DATE_TIME + "), " +
                            DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + " = VALUES(" + DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER + ");";
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setInt(1, debate.getThemeId());
                        statement.setString(2, convertListIdToString(debate.getGovernmentMembersIds()));
                        statement.setString(3, convertListIdToString(debate.getJudgesIds()));
                        statement.setString(4, convertListIdToString(debate.getOppositionMembersIds())); // Исправлен номер параметра с 3 на 4
                        statement.setTimestamp(5, Timestamp.valueOf(debate.getStartDateTime()));
                        statement.setBoolean(6, debate.isGovernmentWinner()); // Исправлен номер параметра с 5 на 6

                        int affectedRows = statement.executeUpdate();
                        if (affectedRows == 0) {
                            throw new SQLException("Creating debate failed, no rows affected.");
                        }

                        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                debate.setId(generatedKeys.getLong(1));
                                return debate;
                            } else {
                                throw new SQLException("Creating debate failed, no ID obtained.");
                            }
                        }
                    } catch (SQLException e) {
                        db.getLogger().error("Ошибка при добавлении дебата", e);
                        throw new CompletionException(e);
                    }
                });
            }
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось добавить дебат", e);
            return null; // Лучше использовать Optional<DebateModel> для избежания возвращения null
        });
    }


    public CompletableFuture<DebateModel> getDebate(long debateId) {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                // Если соединение не удалось восстановить, возвращаем CompletableFuture, завершённый исключением
                CompletableFuture<DebateModel> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new SQLException("Не удалось восстановить соединение с базой данных"));
                return failedFuture;
            } else {
                // Если соединение успешно восстановлено, выполняем запрос на получение информации о дебате
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " WHERE " +
                            DbConstants.COLUMN_DEBATES_ID + " = ?;";
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        statement.setLong(1, debateId);
                        try (ResultSet rs = statement.executeQuery()) {
                            if (rs.next()) {
                                return new DebateModel(
                                        rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                        rs.getInt(DbConstants.COLUMN_DEBATES_THEME_ID),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_JUDGES_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                        rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                        rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                                );
                            } else {
                                throw new NoSuchElementException("Debate with ID " + debateId + " not found");
                            }
                        }
                    } catch (SQLException e) {
                        db.getLogger().error("Ошибка при получении дебата", e);
                        throw new RuntimeException(e);
                    }
                });
            }
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные дебата", e);
            return null; // Использование null не рекомендуется; лучше возвращать Optional<DebateModel>
        });
    }

    public CompletableFuture<DebateModel> getLastDebate() {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                CompletableFuture<DebateModel> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new SQLException("Не удалось восстановить соединение с базой данных"));
                return failedFuture;
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " ORDER BY " +
                            DbConstants.COLUMN_DEBATES_DATE_TIME + " DESC LIMIT 1;";
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        try (ResultSet rs = statement.executeQuery()) {
                            if (rs.next()) {
                                return new DebateModel(
                                        rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                        rs.getInt(DbConstants.COLUMN_DEBATES_THEME_ID),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_JUDGES_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                        rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                        rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                                );
                            } else {
                                throw new NoSuchElementException("No debates found");
                            }
                        }
                    } catch (SQLException e) {
                        db.getLogger().error("Ошибка при получении последнего дебата", e);
                        throw new RuntimeException(e);
                    }
                });
            }
        }).exceptionally(e -> {
            db.getLogger().error("Не удалось получить данные последнего дебата", e);
            return null;
        });
    }

    public CompletableFuture<List<DebaterModel>> getAllDebaters() {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
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
                        db.getLogger().error("Ошибка при получении списка дебатеров", e);
                        throw new RuntimeException(e);
                    }
                    return results;
                });
            }
        });
    }

    public CompletableFuture<List<DebateModel>> getDebates(List<Long> debateIds) {
        if (debateIds == null || debateIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
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
                                        rs.getInt(DbConstants.COLUMN_DEBATES_THEME_ID),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_JUDGES_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                        rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                        rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                                );
                                results.add(result);
                            }
                        }
                    } catch (SQLException e) {
                        db.getLogger().error("Ошибка при получении списка дебатов", e);
                        throw new RuntimeException(e);
                    }
                    return results;
                });
            }
        });
    }

    public CompletableFuture<List<DebateModel>> getDebatesByMemberId(long memberId) {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " WHERE " +
                            DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " LIKE ? OR " +
                            DbConstants.COLUMN_DEBATES_JUDGES_IDS + " LIKE ? OR " +
                            DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " LIKE ?;";
                    List<DebateModel> results = new ArrayList<>();
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        statement.setString(1, "%" + memberId + "%");
                        statement.setString(2, "%" + memberId + "%");
                        try (ResultSet rs = statement.executeQuery()) {
                            while (rs.next()) {
                                DebateModel result = new DebateModel(
                                        rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                        rs.getInt(DbConstants.COLUMN_DEBATES_THEME_ID),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                        rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                        rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                                );
                                results.add(result);
                            }
                        }
                    } catch (SQLException e) {
                        db.getLogger().error("Ошибка при получении списка дебатов по ID участника", e);
                        throw new RuntimeException(e); // Изменено на RuntimeException для правильной обработки в exceptionally
                    }
                    return results;
                });
            }
        });
    }

    public CompletableFuture<List<DebateModel>> getDebatesByMemberId(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    // Создание строки с плейсхолдерами для каждого ID в списке
                    String placeholders = memberIds.stream()
                            .map(id -> "?")
                            .collect(Collectors.joining(","));
                    String sql = "SELECT * FROM " + DbConstants.TABLE_APF_DEBATES + " WHERE " +
                            DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS + " IN (" + placeholders + ") OR " +
                            DbConstants.COLUMN_DEBATES_JUDGES_IDS + " IN (" + placeholders + ") OR " +
                            DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS + " IN (" + placeholders + ");";
                    List<DebateModel> results = new ArrayList<>();
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        int index = 1;
                        // Дважды заполняем плейсхолдеры, поскольку у нас два условия IN в SQL запросе
                        for (int i = 0; i < 2; i++) {
                            for (Long id : memberIds) {
                                statement.setLong(index++, id);
                            }
                        }
                        try (ResultSet rs = statement.executeQuery()) {
                            while (rs.next()) {
                                DebateModel result = new DebateModel(
                                        rs.getLong(DbConstants.COLUMN_DEBATES_ID),
                                        rs.getInt(DbConstants.COLUMN_DEBATES_THEME_ID),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_GOVERNMENT_USERS_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_JUDGES_IDS)),
                                        convertStringToListId(rs.getString(DbConstants.COLUMN_DEBATES_OPPOSITION_USERS_IDS)),
                                        rs.getTimestamp(DbConstants.COLUMN_DEBATES_DATE_TIME).toLocalDateTime(),
                                        rs.getBoolean(DbConstants.COLUMN_DEBATES_IS_GOVERNMENT_WINNER)
                                );
                                results.add(result);
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Ошибка при получении списка дебатов: " + e.getMessage());
                        db.getLogger().error("Ошибка при получении списка ApfDebate", e);
                        throw new RuntimeException(e);
                    }
                    return results;
                });
            }
        });
    }

    public CompletableFuture<List<QuestionModel>> getQuestions(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    String placeholders = questionIds.stream()
                            .map(id -> "?")
                            .collect(Collectors.joining(","));
                    String sql = "SELECT * FROM " + DbConstants.TABLE_TESTS + " WHERE " +
                            DbConstants.COLUMN_TESTS_ID + " IN (" + placeholders + ");";
                    List<QuestionModel> results = new ArrayList<>();
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        int index = 1;
                        for (Integer id : questionIds) {
                            statement.setInt(index++, id);
                        }
                        try (ResultSet rs = statement.executeQuery()) {
                            while (rs.next()) {
                                QuestionModel result = new QuestionModel(
                                        rs.getInt(DbConstants.COLUMN_TESTS_ID),
                                        rs.getString(DbConstants.COLUMN_TESTS_QUESTION),
                                        Arrays.asList(
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_1),
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_2),
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_3),
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_4)
                                        ),
                                        rs.getString(DbConstants.COLUMN_TESTS_CORRECT_ANSWER)
                                );
                                results.add(result);
                            }
                        }
                    } catch (SQLException e) {
                        db.getLogger().error("Ошибка при получении списка вопросов", e);
                        throw new RuntimeException(e);
                    }
                    return results;
                });
            }
        });
    }

    public CompletableFuture<List<QuestionModel>> getAllQuestions() {
        return db.executeWithConnection().thenCompose(connectionResult -> {
            if (!connectionResult) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    String sql = "SELECT * FROM " + DbConstants.TABLE_TESTS + ";";
                    List<QuestionModel> results = new ArrayList<>();
                    try (PreparedStatement statement = db.getConnection().prepareStatement(sql)) {
                        try (ResultSet rs = statement.executeQuery()) {
                            while (rs.next()) {
                                QuestionModel result = new QuestionModel(
                                        rs.getInt(DbConstants.COLUMN_TESTS_ID),
                                        rs.getString(DbConstants.COLUMN_TESTS_QUESTION),
                                        Arrays.asList(
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_1),
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_2),
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_3),
                                                rs.getString(DbConstants.COLUMN_TESTS_ANSWER_4)
                                        ),
                                        rs.getString(DbConstants.COLUMN_TESTS_CORRECT_ANSWER)
                                );
                                results.add(result);
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Ошибка при получении списка вопросов: " + e.getMessage());
                        db.getLogger().error("Ошибка при получении списка вопросов", e);
                        throw new RuntimeException(e);
                    }
                    System.out.println("Вопросы получены");
                    return results;
                });
            }
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

