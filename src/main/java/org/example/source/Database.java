package org.example.source;

import org.example.models.Debater;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private static Database instance;
    private Connection connection;

    private Database(String dbName) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String username = "u5068_vEssE0KzVg";
            String password = "EtBKaPUf5VtIAdco!a=OTOVk";
            String url = "jdbc:mysql://u5068_vEssE0KzVg:EtBKaPUf5VtIAdco!a%3DOTOVk@172.105.158.16:3306/s5068_debate_club";
            this.connection = DriverManager.getConnection(url, username, password);

            createTable();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database("debate_club");
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
                e.printStackTrace();
            }
        } else {
            System.err.println("Соединение с БД не установлено");
        }
    }


    public void insertDebaters(List<Debater> debaters) {
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
            e.printStackTrace();
        } finally {
            closeStatement(pstmt);
            closeStatement(updateStmt);
        }
    }

    public void insertDebater(Debater debater) {
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
            e.printStackTrace();
        } finally {
            closeStatement(pstmt);
            closeStatement(updateStmt);
        }
    }

    public List<Debater> readAllDebaters() {
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
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return debaters;
    }

    public void deleteDebater(Long id) {
        PreparedStatement pstmt = null;
        try {
            String sql = "DELETE FROM DEBATERS WHERE ID = ?;";
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, String.valueOf(id));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeStatement(pstmt);
        }
    }


    private void closeStatement(PreparedStatement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}