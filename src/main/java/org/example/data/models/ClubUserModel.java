package org.example.data.models;

import java.util.List;

public class ClubUserModel {
    private Long id;
    private String nickname;
    private String serverNickname;
    private Long apfDebaterId;
    private List<Long> testsIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getServerNickname() {
        return serverNickname;
    }

    public void setServerNickname(String serverNickname) {
        this.serverNickname = serverNickname;
    }

    public Long getApfDebaterId() {
        return apfDebaterId;
    }

    public void setApfDebaterId(Long apfDebaterId) {
        this.apfDebaterId = apfDebaterId;
    }

    public List<Long> getTestsIds() {
        return testsIds;
    }

    public void setTestsIds(List<Long> testsIds) {
        this.testsIds = testsIds;
    }
}
