package com.example.lifereapplication.data.model;

public class Problem {
    private final int id;
    private final String title;
    private final String description;
    private final String difficulty;
    private final String solution;

    public Problem(int id, String title, String description, String difficulty, String solution) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.solution = solution;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getSolution() {
        return solution;
    }
}
