package org.example.core.models;

public class Theme {
    private int id;
    private String name;
    private int usageCount;

    public Theme() {
    }

    public Theme(int id, String name, int usageCount) {
        this.id = id;
        this.name = name;
        this.usageCount = usageCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    @Override
    public String toString() {
        return "Theme{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", usageCount=" + usageCount +
                '}';
    }
}
