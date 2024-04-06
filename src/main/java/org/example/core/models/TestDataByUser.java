package org.example.core.models;

import net.dv8tion.jda.api.entities.Member;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class TestDataByUser {
    private Member member;
    private List<Question> questions;
    private Question currentQuestion;
    private int currentQuestionNumber = 0;
    private boolean isInProcess = false;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<Member, ScheduledFuture<?>> timers = new HashMap<>();

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

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public Map<Member, ScheduledFuture<?>> getTimers() {
        return timers;
    }

    public void setTimers(Map<Member, ScheduledFuture<?>> timers) {
        this.timers = timers;
    }

    public boolean isInProcess() {
        return isInProcess;
    }

    public void setInProcess(boolean isInProcess) {
        this.isInProcess = isInProcess;
    }

}
