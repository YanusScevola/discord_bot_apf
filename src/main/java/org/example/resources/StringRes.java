package org.example.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class StringRes {
    private final Map<String, String> ruStrings;
    private final Map<String, String> enStrings;
    private Map<String, String> currentStrings;
    private final ObjectMapper objectMapper;

    public StringRes(String defaultLanguage) {
        objectMapper = new ObjectMapper();
        ruStrings = loadLocalizationData("ru_strings.json");
        enStrings = loadLocalizationData("en_strings.json");
        setLanguage(defaultLanguage);
    }

    private Map<String, String> loadLocalizationData(String fileName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("strings/" + fileName);
            if (is == null) {
                throw new FileNotFoundException("Resource file not found: " + fileName);
            }
            return objectMapper.readValue(is, new TypeReference<HashMap<String, String>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void setLanguage(String language) {
        if ("ru".equals(language)) {
            currentStrings = ruStrings;
        } else if ("en".equals(language)) {
            currentStrings = enStrings;
        } else {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    public String get(Key key) {
        return currentStrings.getOrDefault(key.getKey(), "String not found");
    }

    public enum Key {
        TITLE_DEBATER_LIST("title_debater_list"),
        TITLE_JUDGES_LIST("title_judges_list"),
        TITLE_TIMER("title_timer"),
        TITLE_DEBATE_SUBSCRIBE("title_debate_subscribe"),
        TITLE_VOTING("title_voting"),
        TITLE_MEMBER_GOVERNMENT_SPEECH("title_member_government_speech"),
        TITLE_MEMBER_OPPOSITION_SPEECH("title_member_opposition_speech"),
        TITLE_HEAD_GOVERNMENT_FIRST_SPEECH("title_head_government_first_speech"),
        TITLE_HEAD_OPPOSITION_FIRST_SPEECH("title_head_opposition_first_speech"),
        TITLE_HEAD_GOVERNMENT_LAST_SPEECH("title_head_government_last_speech"),
        TITLE_HEAD_OPPOSITION_LAST_SPEECH("title_head_opposition_last_speech"),
        TITLE_JUDGES_PREPARATION("title_judges_preparation"),
        TITLE_DEBATER_PREPARATION("title_debater_preparation"),

        DESCRIPTION_NO_MEMBERS("description_no_members"),
        DESCRIPTION_NEED_GO_TO_TRIBUNE("description_go_to_tribune"),
        DESCRIPTION_VOTE_FOR_GOVERNMENT("description_vote_for_government"),
        DESCRIPTION_VOTE_FOR_OPPOSITION("description_vote_for_opposition"),

        BUTTON_SUBSCRIBE_DEBATER("button_subscribe_debater"),
        BUTTON_SUBSCRIBE_JUDGE("button_subscribe_judge"),
        BUTTON_UNSUBSCRIBE("button_unsubscribe"),
        BUTTON_ASK_QUESTION("button_ask_question"),
        BUTTON_END_SPEECH("button_end_speech"),
        BUTTON_END_DEBATE("button_end_debate"),
        BUTTON_VOTE_GOVERNMENT("button_vote_government"),
        BUTTON_VOTE_OPPOSITION("button_vote_opposition"),

        CHANNEL_TRIBUNE("channel_tribune"),
        CHANNEL_JUDGE("channel_judge"),
        CHANNEL_GOVERNMENT("channel_government"),
        CHANNEL_OPPOSITION("channel_opposition"),

        WARNING_NEED_WAITING_ROOM("warning_need_waiting_room"),
        WARNING_NEED_DEBATER_ROLE("warning_need_debater_role"),
        WARNING_NEED_JUDGE_ROLE("warning_need_judge_role"),
        WARNING_NEED_SUBSCRIBED("warning_need_subscribed"),
        WARNING_ALREADY_DEBATER("warning_already_debater"),
        WARNING_ALREADY_JUDGE("warning_already_judge"),
        WARNING_NOT_IMPLEMENTED("warning_not_implemented"),
        WARNING_NOT_DEBATER("warning_not_debater"),
        WARNING_NOT_JUDGE("warning_not_judge"),
        WARNING_ALREADY_VOTED("warning_already_voted"),
        WARNING_NOT_ASK_OWN_TEAM("warning_not_ask_own_team"),

        REMARK_ASK_OPPONENT_MEMBER("remark_ask_opponent_member"),
        REMARK_DEBATER_ADDED("remark_debater_added"),
        REMARK_JUDGE_ADDED("remark_judge_added"),
        REMARK_DEBATER_REMOVED("remark_debater_removed"),
        REMARK_JUDGE_REMOVED("remark_judge_removed"),
        REMARK_ASK_GOVERNMENT_MEMBER("remark_ask_government_member"),
        REMARK_SPEECH_END("remark_speech_end");

        private final String key;
        Key(String key) {
            this.key = key;
        }
        private String getKey() {
            return key;
        }
    }
}
