package org.example.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class StringRes {

    public static final String CHANNEL_TRIBUNE = "Трибуна";
    public static final String CHANNEL_JUDGE = "Судейская";
    public static final String CHANNEL_GOVERNMENT = "Правительство";
    public static final String CHANNEL_OPPOSITION = "Оппозиция";
    public static final String CHANNEL_WAITING_ROOM = "Зал ожидания";

    public static final String TITLE_DEBATER_LIST = "Дебатеры:";
    public static final String TITLE_JUDGES_LIST = "Судьи:";
    public static final String TITLE_TIMER = "Дебаты начнутся через:";
    public static final String TITLE_DEBATE_SUBSCRIBE = "Запись на дебаты АПФ:";
    public static final String TITLE_VOTING = "Голосование:";
    public static final String TITLE_MEMBER_GOVERNMENT_SPEECH = "Речь члена правительства.";
    public static final String TITLE_MEMBER_OPPOSITION_SPEECH = "Речь члена оппозиции.";
    public static final String TITLE_HEAD_GOVERNMENT_FIRST_SPEECH = "Вступительная речь главы правительства.";
    public static final String TITLE_HEAD_OPPOSITION_FIRST_SPEECH = "Вступительная речь главы оппозиции.";
    public static final String TITLE_HEAD_GOVERNMENT_LAST_SPEECH = "Заключительная речь главы правительства.";
    public static final String TITLE_HEAD_OPPOSITION_LAST_SPEECH = "Заключительная речь главы оппозиции.";
    public static final String TITLE_JUDGES_PREPARATION = "Подготовка судей.";
    public static final String TITLE_DEBATER_PREPARATION = "Подготовка дебатеров.";

    public static final String DESCRIPTION_NO_MEMBERS = "Нет участников";
    public static final String DESCRIPTION_NEED_GO_TO_TRIBUNE = "Дебаты начались! \nПерейдите в голосовой канал \"Трибуна\".";
    public static final String DESCRIPTION_VOTE_FOR_GOVERNMENT = "За правительство: ";
    public static final String DESCRIPTION_VOTE_FOR_OPPOSITION = "\nЗа оппозицию: ";

    public static final String BUTTON_SUBSCRIBE_DEBATER = "Записаться как дебатер";
    public static final String BUTTON_SUBSCRIBE_JUDGE = "Записаться как судья";
    public static final String BUTTON_UNSUBSCRIBE = "Отписаться";
    public static final String BUTTON_ASK_QUESTION = "Задать вопрос";
    public static final String BUTTON_END_SPEECH = "Закончить речь";
    public static final String BUTTON_END_DEBATE = "Закончить дебаты";
    public static final String BUTTON_VOTE_GOVERNMENT = "Голосовать за \"Правительство\"";
    public static final String BUTTON_VOTE_OPPOSITION = "Голосовать за \"Оппозицию\"";

    public static final String WARNING_NEED_WAITING_ROOM = "Вы должны находиться в голосовом канале \"Зал ожидания\".";
    public static final String WARNING_NEED_DEBATER_ROLE = "Нужно получить роль \"Дебатер\".";
    public static final String WARNING_NEED_JUDGE_ROLE = "Нужно получить роль \"Судья\".";
    public static final String WARNING_NEED_SUBSCRIBED = "Вы не записаны на дебаты.";
    public static final String WARNING_ALREADY_DEBATER = "Вы уже находитесь в списке дебатеров.";
    public static final String WARNING_ALREADY_JUDGE = "Вы уже находитесь в списке судей.";
    public static final String WARNING_NOT_IMPLEMENTED = "Данная функция пока не реализована.";
    public static final String WARNING_NOT_DEBATER = "Вы не дебатер";
    public static final String WARNING_NOT_JUDGE = "Вы не судья";
    public static final String WARNING_ALREADY_VOTED = "Вы уже проголосовали";
    public static final String WARNING_NOT_ASK_OWN_TEAM = "Нельзя задавать вопрос члену своей команды";

    public static final String REMARK_DEBATER_ADDED = "Вы были добавлены в список дебатеров.";
    public static final String REMARK_JUDGE_ADDED = "Вы были добавлены в список судей.";
    public static final String REMARK_DEBATER_REMOVED = "Вы были удалены из списка дебатеров.";
    public static final String REMARK_JUDGE_REMOVED = "Вы были удалены из списка судей.";
    public static final String REMARK_ASK_GOVERNMENT_MEMBER = "Задайте вопрос члену правительства.";
    public static final String REMARK_ASK_OPPOSITION_MEMBER = "Задайте вопрос члену оппозиции.";
    public static final String REMARK_SPEECH_END = "Вы закончили речь.";
    public static final String REMARK_VOTE_GOVERNMENT = "Вы проголосовали за \"Правительство\".";
    public static final String REMARK_VOTE_OPPOSITION = "Вы проголосовали за \"Оппозицию\".";

}
