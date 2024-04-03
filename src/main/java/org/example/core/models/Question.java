package org.example.core.models;

import java.util.Collections;
import java.util.List;

public class Question {
    private int id;
    private String text;
    private List<String> answers;
    private String correctAnswer;

    public Question(int id, String text, List<String> answers, String correctAnswer) {
        this.id = id;
        this.text = text;
        this.answers = answers;
        this.correctAnswer = correctAnswer;
        Collections.shuffle(this.answers);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}
