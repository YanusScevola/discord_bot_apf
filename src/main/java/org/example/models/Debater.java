package org.example.models;

public class Debater {
        private String id;
        private String nickname;
        private String teamName;
        private int balls;
        private int debateCount;
        private int winner;

    public Debater() {
    }

    public Long getId() {
        return  Long.valueOf(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getBalls() {
        return balls;
    }

    public void setBalls(int balls) {
        this.balls = balls;
    }

    public int getDebateCount() {
        return debateCount;
    }

    public void setDebateCount(int debateCount) {
        this.debateCount = debateCount;
    }

   public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    @Override
    public String toString() {
        return "Debater{" +
                "id='" + id + '\'' +
                ", nickname='" + nickname + '\'' +
                ", balls=" + balls +
                ", debateCount=" + debateCount +
                '}';
    }
}
