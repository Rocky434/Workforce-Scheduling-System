package com.example.scheduling.report;

import com.example.scheduling.calendar.*;
import com.example.scheduling.schedule.*;
import com.example.scheduling.user.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerReportController {
    private final ScheduleEntryRepository entries;
    private final UserRepository users;
    private final CalendarDayRepository days;

    public OwnerReportController(ScheduleEntryRepository entries, UserRepository users, CalendarDayRepository days) {
        this.entries = entries;
        this.users = users;
        this.days = days;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam String month) {
        var requestedMonth = YearMonth.parse(month);
        var monthStart = requestedMonth.atDay(1);
        var monthEnd = requestedMonth.atEndOfMonth();
        var employees = users.findAllByRoleAndActiveTrueOrderByDisplayName(AppUser.Role.EMPLOYEE);
        var monthlyEntries = entries.findDetailedBetween(monthStart, monthEnd);
        var yearlyEntries = entries.findDetailedBetween(LocalDate.of(requestedMonth.getYear(), 1, 1), LocalDate.of(requestedMonth.getYear(), 12, 31));
        var monthlyAssignmentCounts = monthlyEntries.stream()
                .collect(Collectors.groupingBy(entry -> entry.getEmployee().getId(), Collectors.counting()));
        var yearlyAssignmentCounts = yearlyEntries.stream()
                .collect(Collectors.groupingBy(entry -> entry.getEmployee().getId(), Collectors.counting()));
        var employeeStatistics = employees.stream().map(employee -> new EmployeeStat(employee.getId(), employee.getDisplayName(),
                monthlyAssignmentCounts.getOrDefault(employee.getId(), 0L).intValue(), yearlyAssignmentCounts.getOrDefault(employee.getId(), 0L).intValue()))
                .sorted(Comparator.comparingInt(EmployeeStat::monthlyDays).reversed()
                        .thenComparing(EmployeeStat::displayName))
                .toList();
        var assignments = monthlyEntries.stream().map(entry -> new Assignment(entry.getCalendarDay().getDate(),
                entry.getEmployee().getId(), entry.getEmployee().getDisplayName())).toList();
        var calendarDays = days.findByDateBetweenOrderByDate(monthStart, monthEnd).stream()
                .map(calendarDay -> new Day(calendarDay.getDate(), calendarDay.getDayType().name(), calendarDay.getHolidayName(), calendarDay.isSchedulable()))
                .toList();
        return new Dashboard(month, employeeStatistics, assignments, calendarDays);
    }

    public record EmployeeStat(Long employeeId, String displayName, int monthlyDays, int yearlyDays) {
    }

    public record Assignment(LocalDate date, Long employeeId, String displayName) {
    }

    public record Day(LocalDate date, String dayType, String holidayName, boolean schedulable) {
    }

    public record Dashboard(String month, List<EmployeeStat> employees, List<Assignment> assignments, List<Day> days) {
    }
}
