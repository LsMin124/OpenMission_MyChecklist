package com.example.demo.util;

import com.example.demo.domain.Task;
import com.example.demo.domain.TaskType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RecurrenceUtils {

     // 오늘 날짜(date)가 해당 Task의 일정에 포함되는지 판단
    public static boolean isScheduledForDate(Task task, LocalDate date, boolean isCompletedToday) {
        if (task.getTaskType() == TaskType.ONE_TIME) {
            return isOneTimeTaskScheduled(task, date, isCompletedToday);
        }
        return isRecurringTaskScheduled(task, date);
    }

    private static boolean isOneTimeTaskScheduled(Task task, LocalDate date, boolean isCompletedToday) {
        LocalDate due = task.getDueDate();
        // 오늘 마감
        if (due.isEqual(date)) return true;
        // 마감 지났는데 아직 안 함
        if (due.isBefore(date) && !isCompletedToday) return true;

        return false;
    }

    private static boolean isRecurringTaskScheduled(Task task, LocalDate date) {
        String rule = task.getRecurrenceRule();
        if (rule == null) return false;

        // 매일
        if ("DAILY".equals(rule)) return true;

        try {
            // N일 간격 ("EVERY_N_DAYS:3")
            if (rule.startsWith("EVERY_N_DAYS:")) {
                int n = Integer.parseInt(rule.split(":")[1]);
                LocalDate start = task.getCreatedAt().toLocalDate();
                long daysBetween = ChronoUnit.DAYS.between(start, date);
                return daysBetween >= 0 && daysBetween % n == 0;
            }
            // 매월 ("MONTHLY:25")
            if (rule.startsWith("MONTHLY:")) {
                int day = Integer.parseInt(rule.split(":")[1]);
                return date.getDayOfMonth() == day;
            }
        } catch (Exception e) {
            // 파싱 에러 시 안전하게 false 반환 (로그 남기면 좋음)
            return false;
        }
        return false;
    }
}