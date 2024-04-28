package org.example.core.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateTimeUtils {

    /**
     * Конвертирует LocalDateTime в UTC.
     *
     * @param localDateTime LocalDateTime в локальном времени
     * @return LocalDateTime в UTC
     */
    public static LocalDateTime toUtc(LocalDateTime localDateTime) {
        // Конвертируем LocalDateTime из локального времени в UTC
        return localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * Конвертирует LocalDateTime из UTC в локальное время.
     *
     * @param utcDateTime LocalDateTime в UTC
     * @return LocalDateTime в локальном времени
     */
    public static LocalDateTime fromUtc(LocalDateTime utcDateTime) {
        // Конвертируем LocalDateTime из UTC в системное локальное время
        return utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
