package org.example.source;

import org.example.models.Debater;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private static Database instance;
    private Connection connection;

    private Database(String dbName) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            createTable();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database("debate_club.db");
        }
        return instance;
    }

    private void createTable() {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS DEBATERS " +
                    "(ID INT PRIMARY KEY NOT NULL," +
                    " NICKNAME TEXT NOT NULL, " +
                    " DEBATE_COUNT INT, " +
                    " WINNER INT, " +
                    " BALLS INT)";

            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public void insertDebaters(List<Debater> debaters) {
        PreparedStatement pstmt = null;
        PreparedStatement updateStmt = null;
        try {
            String sql = "INSERT OR IGNORE INTO DEBATERS (ID, NICKNAME) VALUES (?, ?);";
            pstmt = connection.prepareStatement(sql);

            String updateSql = "UPDATE DEBATERS SET NICKNAME = ? WHERE ID = ?;";
            updateStmt = connection.prepareStatement(updateSql);

            for (Debater debater : debaters) {
                pstmt.setLong(1, debater.getId());
                pstmt.setString(2, debater.getNickname());
                pstmt.addBatch();

                updateStmt.setString(1, debater.getNickname());
                updateStmt.setLong(2, debater.getId());
                updateStmt.addBatch();
            }

            pstmt.executeBatch();
            updateStmt.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (updateStmt != null) updateStmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void insertDebater(Debater debater) {
        PreparedStatement pstmt = null;
        PreparedStatement updateStmt = null;
        try {
            String sql = "INSERT OR IGNORE INTO DEBATERS (ID, NICKNAME) VALUES (?, ?);";
            pstmt = connection.prepareStatement(sql);

            String updateSql = "UPDATE DEBATERS SET NICKNAME = ? WHERE ID = ?;";
            updateStmt = connection.prepareStatement(updateSql);

            pstmt.setLong(1, debater.getId());
            pstmt.setString(2, debater.getNickname());
            pstmt.executeUpdate();

            updateStmt.setString(1, debater.getNickname());
            updateStmt.setLong(2, debater.getId());
            updateStmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (updateStmt != null) updateStmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public void deleteDebater(long id) {
        PreparedStatement pstmt = null;
        try {
            String sql = "DELETE FROM DEBATERS WHERE ID = ?;";
            pstmt = connection.prepareStatement(sql);
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

}
