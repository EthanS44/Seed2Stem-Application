package com.example.seed2stem;

/** One item in a dashboard's Alerts section: something that needs a look, with a link to it. */
public class Notification {

    /** Most urgent first; the section is sorted in this order. */
    public enum Level {
        DANGER("bad"), WARN("warn"), INFO("info"), OK("ok");

        private final String tone;

        Level(String tone) { this.tone = tone; }

        /** CSS tone used by the dashboard (matches the pill colours). */
        public String getTone() { return tone; }
    }

    private final Level level;
    private final String icon;
    private final String title;
    private final String detail;
    private final String href;
    private final String action;

    public Notification(Level level, String icon, String title, String detail, String href, String action) {
        this.level = level;
        this.icon = icon;
        this.title = title;
        this.detail = detail;
        this.href = href;
        this.action = action;
    }

    public Level getLevel() { return level; }
    public String getIcon() { return icon; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getHref() { return href; }
    public String getAction() { return action; }
}
