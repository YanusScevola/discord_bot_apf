package org.example.resources;

import java.util.HashMap;
import java.util.Map;

public class StringsRes {
    public enum Language {
        ENGLISH, RUSSIAN
    }

    // Перечисление для ключей
    public enum Key {
        DEBATER_ADDED, NEED_DEBATER_ROLE, NEED_WAITING_ROOM
    }

    private static StringsRes instance;
    private Language currentLanguage;
    private final Map<Key, Map<Language, String>> strings = new HashMap<>();

    private StringsRes(Language language) {
        this.currentLanguage = language;
        initializeStrings();
    }

    public static StringsRes getInstance(Language language) {
        if (instance == null || instance.currentLanguage != language) {
            instance = new StringsRes(language);
        }
        return instance;
    }

    private void initializeStrings() {
        addString(Key.DEBATER_ADDED, Language.ENGLISH, "You have been added to the debaters list.");
        addString(Key.DEBATER_ADDED, Language.RUSSIAN, "Вы были добавлены в список дебатеров.");

        addString(Key.NEED_DEBATER_ROLE, Language.ENGLISH, "You need to acquire the \"Debater\" role.");
        addString(Key.NEED_DEBATER_ROLE, Language.RUSSIAN, "Нужно получить роль \"Дебатер\".");

        addString(Key.NEED_WAITING_ROOM, Language.RUSSIAN, "Вы должны находиться в голосовом канале \"Зал ожидания\".");

    }

    private void addString(Key key, Language lang, String text) {
        if (!strings.containsKey(key)) {
            strings.put(key, new HashMap<>());
        }
        strings.get(key).put(lang, text);
    }

    public String getString(Key key) {
        return strings.getOrDefault(key, new HashMap<>()).getOrDefault(currentLanguage, "String not found");
    }

    public void setLanguage(Language newLanguage) {
        this.currentLanguage = newLanguage;
    }
}
