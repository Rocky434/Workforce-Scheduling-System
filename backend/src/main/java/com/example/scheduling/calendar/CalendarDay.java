package com.example.scheduling.calendar;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "calendar_days")
public class CalendarDay {
    public enum DayType { WORKDAY, WEEKEND, HOLIDAY }
    @Id @Column(name = "calendar_date") private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(name = "day_type", nullable = false) private DayType dayType;
    @Column(name = "holiday_name") private String holidayName;
    @Column(nullable = false) private boolean schedulable;
    @Column(nullable = false) private String source;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected CalendarDay() {}
    public CalendarDay(LocalDate date, DayType dayType, String holidayName, boolean schedulable, String source) {
        this.date=date; this.dayType=dayType; this.holidayName=holidayName; this.schedulable=schedulable; this.source=source;
    }
    public LocalDate getDate(){return date;} public DayType getDayType(){return dayType;}
    public String getHolidayName(){return holidayName;} public boolean isSchedulable(){return schedulable;}
}

