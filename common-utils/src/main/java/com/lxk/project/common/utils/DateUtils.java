package com.lxk.project.common.utils;

import com.lxk.project.common.po.DateStyle;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/5/19 15:05
 * @ClassName DateUtils
 * @Remark
 */

public class DateUtils {

    public static String formatDateToString(Date date, DateStyle style){
        LocalDateTime localDateTime = date2LocalDateTime(date);
        return formatLocalDateTimeToString(localDateTime, style);
    }

    public static LocalDateTime date2LocalDateTime(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        return instant.atZone(zoneId).toLocalDateTime();
    }
    public static String formatLocalDateTimeToString(LocalDateTime localDateTime, DateStyle dateStyle) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateStyle.getValue());
            return localDateTime.format(formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
