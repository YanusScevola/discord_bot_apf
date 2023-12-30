package org.example.resources;

import java.util.HashMap;
import java.util.Map;

public class StringRes {

    public enum Language {
        ENGLISH, RUSSIAN
    }

    public enum Key {
        DEBATER_ADDED, NEED_DEBATER_ROLE, NEED_WAITING_ROOM, DEBATE_SUBSCRIBE_TITLE, NO_MEMBERS, DEBATER_LIST_TITLE,
        JUDGES_LIST_TITLE, BUTTON_SUBSCRIBE_DEBATER, BUTTON_SUBSCRIBE_JUDGE, BUTTON_UNSUBSCRIBE, DEBATER_REMOVED,
        JUDGE_REMOVED, DEBATER_NOT_SUBSCRIBED, JUDGE_NOT_SUBSCRIBED, JUDGE_ADDED, NEED_JUDGE_ROLE, TIMER_TITLE,
        DEBATE_STARTED,
    }

    private static StringRes instance;
    private Language currentLanguage;
    private final Map<Key, Map<Language, String>> strings = new HashMap<>();

    private StringRes(Language language) {
        this.currentLanguage = language;
        initializeStrings();
    }

    public static StringRes getInstance(Language language) {
        if (instance == null || instance.currentLanguage != language) {
            instance = new StringRes(language);
        }
        return instance;
    }

    private void initializeStrings() {
        addString(Key.DEBATE_SUBSCRIBE_TITLE, Language.RUSSIAN, "Запись на дебаты АПФ");
        addString(Key.NO_MEMBERS, Language.RUSSIAN, "Нет участников");
        addString(Key.DEBATER_LIST_TITLE, Language.RUSSIAN, "Дебатеры:");
        addString(Key.JUDGES_LIST_TITLE, Language.RUSSIAN, "Судьи:");

        addString(Key.DEBATER_ADDED, Language.ENGLISH, "You have been added to the debaters list.");
        addString(Key.DEBATER_ADDED, Language.RUSSIAN, "Вы были добавлены в список дебатеров.");

        addString(Key.NEED_DEBATER_ROLE, Language.ENGLISH, "You need to acquire the \"Debater\" role.");
        addString(Key.NEED_DEBATER_ROLE, Language.RUSSIAN, "Нужно получить роль \"Дебатер\".");

        addString(Key.NEED_WAITING_ROOM, Language.RUSSIAN, "Вы должны находиться в голосовом канале \"Зал ожидания\".");

        addString(Key.BUTTON_SUBSCRIBE_DEBATER, Language.RUSSIAN, "Записаться как дебатер");
        addString(Key.BUTTON_SUBSCRIBE_JUDGE, Language.RUSSIAN, "Записаться как судья");
        addString(Key.BUTTON_UNSUBSCRIBE, Language.RUSSIAN, "Отписаться");
        addString(Key.DEBATER_REMOVED, Language.RUSSIAN, "Вы были удалены из списка дебатеров.");
        addString(Key.JUDGE_REMOVED, Language.RUSSIAN, "Вы были удалены из списка судей.");
        addString(Key.DEBATER_NOT_SUBSCRIBED, Language.RUSSIAN, "Вы не записаны на дебаты.");
        addString(Key.JUDGE_NOT_SUBSCRIBED, Language.RUSSIAN, "Вы не записаны на дебаты.");
        addString(Key.JUDGE_ADDED, Language.RUSSIAN, "Вы были добавлены в список судей.");
        addString(Key.NEED_JUDGE_ROLE, Language.RUSSIAN, "Нужно получить роль \"Судья\".");
        addString(Key.TIMER_TITLE, Language.RUSSIAN, "Дебаты начнутся через:");
        addString(Key.DEBATE_STARTED, Language.RUSSIAN, "Дебаты начались! \nПерейдите в голосовой канал \"Трибуна\".");



    }

    private void addString(Key key, Language lang, String text) {
        if (!strings.containsKey(key)) {
            strings.put(key, new HashMap<>());
        }
        strings.get(key).put(lang, text);
    }

    public String get(Key key) {
        return strings.getOrDefault(key, new HashMap<>()).getOrDefault(currentLanguage, "String not found");
    }

    public void setLanguage(Language newLanguage) {
        this.currentLanguage = newLanguage;
    }
}
