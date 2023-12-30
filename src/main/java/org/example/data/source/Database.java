package org.example.data.source;

import org.example.ui.models.Debater;
import org.example.data.source.models.DebateSourceModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

            createTable();
            createDebateTable();
            createTeamsTable();
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

    private void createTable() {
        if (connection != null) {
            try (Statement stmt = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS DEBATERS " +
                        "(ID BIGINT PRIMARY KEY NOT NULL," +
                        " NICKNAME VARCHAR(255) NOT NULL, " +
                        " DEBATE_COUNT INT, " +
                        " WINNER INT, " +
                        " BALLS INT);";

                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                logger.debug("Ошибка создания таблицы", e);
            }
        } else {
            logger.debug("Ошибка подключения к БД");
        }
    }

    private void createDebateTable() {
        if (connection != null) {
            try (Statement stmt = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS DEBATE " +
                        "(ID INT PRIMARY KEY NOT NULL, " +
                        " THESIS VARCHAR(255) NOT NULL, " +
                        " GOVERNMENT_TEAM VARCHAR(255), " +
                        " OPPOSITION_TEAM VARCHAR(255), " +
                        " DATE VARCHAR(255), " +
                        " WINNER_TEAM INT);";

                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                logger.debug("Ошибка создания таблицы DEBATE", e);
            }
        } else {
            logger.debug("Ошибка подключения к БД");
        }
    }

    private void createTeamsTable() {
        if (connection != null) {
            try (Statement stmt = connection.createStatement()) {
                // Создание таблицы teams
                String sqlTeams = "CREATE TABLE IF NOT EXISTS TEAMS " +
                        "(ID INT PRIMARY KEY NOT NULL," +
                        " NAME VARCHAR(255) NOT NULL, " +
                        " DEBATERS_ID VARCHAR(255));";
                stmt.executeUpdate(sqlTeams);
            } catch (SQLException e) {
                logger.debug("Ошибка создания таблицы TEAMS", e);
            }
        } else {
            logger.debug("Ошибка подключения к БД");
        }
    }




    public void insertDebaters(@NotNull List<Debater> debaters) {
        PreparedStatement pstmt = null;
        PreparedStatement updateStmt = null;
        try {
            String sql = "INSERT IGNORE INTO DEBATERS (ID, NICKNAME) VALUES (?, ?);";
            pstmt = connection.prepareStatement(sql);

            String updateSql = "UPDATE DEBATERS SET NICKNAME = ? WHERE ID = ?;";
            updateStmt = connection.prepareStatement(updateSql);

            for (Debater debater : debaters) {
                pstmt.setString(1, String.valueOf(debater.getId()));
                pstmt.setString(2, debater.getNickname());
                pstmt.addBatch();

                updateStmt.setString(1, debater.getNickname());
                updateStmt.setString(2, String.valueOf(debater.getId())); // Здесь также используется setString вместо setLong
                updateStmt.addBatch();
            }

            pstmt.executeBatch();
            updateStmt.executeBatch();
        } catch (SQLException e) {
             logger.debug("Ошибка вставки дебатеров", e);
        } finally {
            closeStatement(pstmt);
            closeStatement(updateStmt);
        }
    }

    public void insertDebater(@NotNull Debater debater) {
        PreparedStatement pstmt = null;
        PreparedStatement updateStmt = null;
        try {
            String sql = "INSERT IGNORE INTO DEBATERS (ID, NICKNAME) VALUES (?, ?);";
            pstmt = connection.prepareStatement(sql);

            String updateSql = "UPDATE DEBATERS SET NICKNAME = ? WHERE ID = ?;";
            updateStmt = connection.prepareStatement(updateSql);

            pstmt.setString(1, String.valueOf(debater.getId()));
            pstmt.setString(2, debater.getNickname());
            pstmt.executeUpdate();

            updateStmt.setString(1, debater.getNickname());
            updateStmt.setString(2, String.valueOf(debater.getId()));
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Ошибка вставки дебатера", e);
        } finally {
            closeStatement(pstmt);
            closeStatement(updateStmt);
        }
    }

    public List<Debater> getAllDebaters() {
        Statement stmt = null;
        List<Debater> debaters = new ArrayList<>();
        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM DEBATERS;");

            while (rs.next()) {
                String id = rs.getString("ID");
                int debateCount = rs.getInt("DEBATE_COUNT");
                int balls = rs.getInt("BALLS");
                String nickname = rs.getString("NICKNAME");
                int winner = rs.getInt("WINNER");

                Debater debater = new Debater();
                debater.setId(id);
                debater.setDebateCount(debateCount);
                debater.setBalls(balls);
                debater.setNickname(nickname);
                debater.setWinner(winner);

                debaters.add(debater);
            }
        } catch (SQLException e) {
           logger.debug("Ошибка чтения дебатеров", e);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                logger.debug("Ошибка закрытия statement", e);
            }
        }
        return debaters;
    }


    public List<Debater> getDebatersByIds(List<String> ids) {
        Statement stmt = null;
        List<Debater> debaters = new ArrayList<>();

        String joinedIds = String.join(",", ids);

        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM DEBATERS WHERE ID IN (" + joinedIds + ");");

            while (rs.next()) {
                String id = rs.getString("ID");
                int debateCount = rs.getInt("DEBATE_COUNT");
                int balls = rs.getInt("BALLS");
                String nickname = rs.getString("NICKNAME");
                int winner = rs.getInt("WINNER");

                Debater debater = new Debater();
                debater.setId(id);
                debater.setDebateCount(debateCount);
                debater.setBalls(balls);
                debater.setNickname(nickname);
                debater.setWinner(winner);

                debaters.add(debater);
            }
        } catch (SQLException e) {
            logger.debug("Ошибка чтения дебатеров по ID", e);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                logger.debug("Ошибка закрытия statement", e);
            }
        }
        return debaters;
    }

    public List<DebateSourceModel> getAllDebates() {
        Statement stmt = null;
        List<DebateSourceModel> debates = new ArrayList<>();
        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM DEBATE;");

            while (rs.next()) {
                String id = rs.getString("ID");
                String thesis = rs.getString("THESIS");
                String dateString = String.valueOf(rs.getDate("DATE"));
                String governmentTeam = rs.getString("GOVERNMENT_TEAM");
                String oppositionTeam = rs.getString("OPPOSITION_TEAM");
                int winner = rs.getInt("WINNER_TEAM");

                DebateSourceModel debate = new DebateSourceModel();
                debate.setId(id);
                debate.setThesis(thesis);
                debate.setDateString(dateString);
                debate.setGovernmentTeamId(governmentTeam);
                debate.setOppositionTeamId(oppositionTeam);
                debate.setWinner(winner);

                debates.add(debate);
            }
        } catch (SQLException e) {
            logger.debug("Ошибка чтения дебатов", e);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                logger.debug("Ошибка закрытия statement", e);
            }
        }
        return debates;
    }


    public void deleteDebater(Long id) {
        PreparedStatement pstmt = null;
        try {
            String sql = "DELETE FROM DEBATERS WHERE ID = ?;";
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, String.valueOf(id));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Ошибка удаления дебатера", e);
        } finally {
            closeStatement(pstmt);
        }
    }


    private void closeStatement(PreparedStatement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            logger.debug("Ошибка закрытия statement", e);
        }
    }

}