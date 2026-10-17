package Yyxtao;

import java.util.Date;

public class AthleteScore {
    private int id;
    private String category;
    private double score;
    private String name;
    private Date date;

    public AthleteScore(int id, String category, double score, String name, Date date) {
        this.id = id;
        this.category = category;
        this.score = score;
        this.name = name;
        this.date = date;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    @Override
    public String toString() {
        return "AthleteScore{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", score=" + score +
                ", name='" + name + '\'' +
                ", date=" + date +
                '}';
    }
}


