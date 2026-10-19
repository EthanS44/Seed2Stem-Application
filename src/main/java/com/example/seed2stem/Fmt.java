package com.example.seed2stem;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Display helpers for templates, used as {@code ${@fmt.when(run.endTime)}}.
 * Keeps date wording consistent across dashboards and review pages.
 */
@Component("fmt")
public class Fmt {

    // Request locale, matching what #temporals uses in the same templates.
    private static DateTimeFormatter time() {
        return DateTimeFormatter.ofPattern("h:mm a", LocaleContextHolder.getLocale());
    }

    private static DateTimeFormatter dayTime() {
        return DateTimeFormatter.ofPattern("MMM d, h:mm a", LocaleContextHolder.getLocale());
    }

    /** "9:40 PM" today, "Yesterday 9:40 PM", otherwise "Oct 3, 9:40 PM". */
    public String when(LocalDateTime t) {
        if (t == null) return "";
        LocalDate day = t.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) return t.format(time());
        if (day.equals(today.minusDays(1))) return "Yesterday " + t.format(time());
        return t.format(dayTime());
    }

    /** "just now", "12 min ago", "3 h ago", "2 days ago". */
    public String ago(LocalDateTime t) {
        if (t == null) return "";
        long minutes = Duration.between(t, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " h ago";
        long days = hours / 24;
        return days == 1 ? "1 day ago" : days + " days ago";
    }

    /** "45 min" or "1 h 20 min". */
    public String minutes(Long total) {
        if (total == null) return "";
        if (total < 1) return "under 1 min";
        if (total < 60) return total + " min";
        long h = total / 60, m = total % 60;
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    public String initials(User u) {
        if (u == null) return "?";
        String f = u.getFirstName() != null && !u.getFirstName().isBlank() ? u.getFirstName().substring(0, 1) : "";
        String l = u.getLastName() != null && !u.getLastName().isBlank() ? u.getLastName().substring(0, 1) : "";
        String s = (f + l).toUpperCase();
        return s.isEmpty() ? "?" : s;
    }
}
