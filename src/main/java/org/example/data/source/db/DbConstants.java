package org.example.data.source.db;

public class DbConstants {
    public static final int VERSION = 1;

    // Названия таблиц
    public static final String TABLE_DB_VERSION = "db_version";
    public static final String TABLE_THEMES = "apf_themes";
    public static final String TABLE_APF_DEBATERS = "apf_debaters";
    public static final String TABLE_APF_DEBATES = "apf_debates";

    // Столбцы для таблицы db_version
    public static final String COLUMN_VERSION = "version";

    // Столбцы для таблицы themes
    public static final String COLUMN_THEMES_ID = "id";
    public static final String COLUMN_THEMES_NAME = "name";
    public static final String COLUMN_THEMES_USAGE_COUNT = "usage_count";


    // Столбцы для таблицы apf_debaters
    public static final String COLUMN_DEBATERS_ID = "user_id";
    public static final String COLUMN_DEBATERS_NICKNAME = "nickname";
    public static final String COLUMN_DEBATERS_SERVER_NICKNAME = "server_nickname";
    public static final String COLUMN_DEBATERS_APF_DEBATES_IDS = "apf_debates_ids";
    public static final String COLUMN_DEBATERS_LOSSES = "losses_debates_count";
    public static final String COLUMN_DEBATERS_WINS = "win_debates_count";

    // Столбцы для таблицы apf_debates
    public static final String COLUMN_DEBATES_ID = "id";
    public static final String COLUMN_DEBATES_TEME_ID = "theme";
    public static final String COLUMN_DEBATES_GOVERNMENT_USERS_IDS = "government_users_ids";
    public static final String COLUMN_DEBATES_OPPOSITION_USERS_IDS = "opposition_users_ids";
    public static final String COLUMN_DEBATES_DATE_TIME = "date_time";
    public static final String COLUMN_DEBATES_IS_GOVERNMENT_WINNER = "is_government_winner";

    private DbConstants() {
        // Приватный конструктор, чтобы предотвратить создание экземпляров класса
    }
}

