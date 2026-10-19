package com.example.seed2stem;

public class CalendarEventDTO {

    private String id;
    private String title;
    private String start;
    private String end;
    private boolean allDay;
    private String color;
    private String url;
    private String eventType;
    /** Run status (IN_PROGRESS, PENDING, APPROVED, REJECTED); the calendar colours and filters by it. */
    private String status;
    private String statusLabel;
    /** Who did the task, for the label and the person filter. */
    private String person;
    private Long personId;

    public CalendarEventDTO() {}

    public CalendarEventDTO(String id, String title, String start, String end,
                            boolean allDay, String color, String url, String eventType) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.allDay = allDay;
        this.color = color;
        this.url = url;
        this.eventType = eventType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public String getPerson() { return person; }
    public void setPerson(String person) { this.person = person; }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
}
