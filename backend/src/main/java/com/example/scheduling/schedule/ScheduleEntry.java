package com.example.scheduling.schedule;

import com.example.scheduling.calendar.CalendarDay;
import com.example.scheduling.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="schedule_entries", uniqueConstraints=@UniqueConstraint(columnNames={"employee_id","work_date"}))
public class ScheduleEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="employee_id") private AppUser employee;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="work_date") private CalendarDay calendarDay;
    @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();
    protected ScheduleEntry() {}
    public ScheduleEntry(AppUser employee, CalendarDay calendarDay){this.employee=employee;this.calendarDay=calendarDay;}
    public Long getId(){return id;} public AppUser getEmployee(){return employee;} public CalendarDay getCalendarDay(){return calendarDay;}
}

