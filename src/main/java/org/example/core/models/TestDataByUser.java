package org.example.core.models;

import net.dv8tion.jda.api.entities.Member;

import java.util.Collections;
import java.util.List;

public class TestDataByUser {
    private Member member;
    private List<Question> questions;
    private Question currentQuestion;
    private int currentQuestionNumber = 0;
    private String selectedAnswer;

    public TestDataByUser(Member member, List<Question> questions) {
        this.member = member;
        this.questions = questions;
        Collections.shuffle(this.questions);
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(Question currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public int getCurrentQuestionNumber() {
        return currentQuestionNumber;
    }

    public void setCurrentQuestionNumber(int currentQuestionNumber) {
        this.currentQuestionNumber = currentQuestionNumber;
    }

}
