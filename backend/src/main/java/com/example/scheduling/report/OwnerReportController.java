package com.example.scheduling.report;

import com.example.scheduling.schedule.*;
import com.example.scheduling.user.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/owner") @PreAuthorize("hasRole('OWNER')")
public class OwnerReportController {
    private final ScheduleEntryRepository entries; private final UserRepository users;
    public OwnerReportController(ScheduleEntryRepository entries,UserRepository users){this.entries=entries;this.users=users;}
    @GetMapping("/dashboard") public Dashboard dashboard(@RequestParam String month){
        var ym=YearMonth.parse(month); var start=ym.atDay(1); var end=ym.atEndOfMonth();
        var staff=users.findAllByRoleAndActiveTrueOrderByDisplayName(AppUser.Role.EMPLOYEE);
        var monthly=entries.findDetailedBetween(start,end); var yearly=entries.findDetailedBetween(LocalDate.of(ym.getYear(),1,1),LocalDate.of(ym.getYear(),12,31));
        var monthCounts=monthly.stream().collect(Collectors.groupingBy(e->e.getEmployee().getId(),Collectors.counting()));
        var yearCounts=yearly.stream().collect(Collectors.groupingBy(e->e.getEmployee().getId(),Collectors.counting()));
        var employeeViews=staff.stream().map(u->new EmployeeStat(u.getId(),u.getDisplayName(),monthCounts.getOrDefault(u.getId(),0L).intValue(),yearCounts.getOrDefault(u.getId(),0L).intValue())).sorted(Comparator.comparingInt(EmployeeStat::monthlyDays).reversed().thenComparing(EmployeeStat::displayName)).toList();
        var assignments=monthly.stream().map(e->new Assignment(e.getCalendarDay().getDate(),e.getEmployee().getId(),e.getEmployee().getDisplayName())).toList();
        return new Dashboard(month,employeeViews,assignments);
    }
    public record EmployeeStat(Long employeeId,String displayName,int monthlyDays,int yearlyDays){}
    public record Assignment(LocalDate date,Long employeeId,String displayName){}
    public record Dashboard(String month,List<EmployeeStat> employees,List<Assignment> assignments){}
}
