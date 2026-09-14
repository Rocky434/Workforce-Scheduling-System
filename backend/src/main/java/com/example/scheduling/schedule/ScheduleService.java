package com.example.scheduling.schedule;

import com.example.scheduling.calendar.*;
import com.example.scheduling.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    private static final ZoneId TAIPEI=ZoneId.of("Asia/Taipei");
    private final ScheduleEntryRepository entries; private final CalendarDayRepository days; private final UserRepository users;
    public ScheduleService(ScheduleEntryRepository entries,CalendarDayRepository days,UserRepository users){this.entries=entries;this.days=days;this.users=users;}

    public MonthView employeeMonth(Long employeeId,YearMonth month){
        validateNextMonth(month);
        var start=month.atDay(1); var end=month.atEndOfMonth();
        var selected=entries.findEmployeeMonth(employeeId,start,end).stream().map(e->e.getCalendarDay().getDate()).collect(Collectors.toSet());
        var counts=counts(start,end);
        var view=days.findByDateBetweenOrderByDate(start,end).stream().map(d->new DayView(d.getDate(),d.getDayType().name(),d.getHolidayName(),d.isSchedulable(),counts.getOrDefault(d.getDate(),0L).intValue(),selected.contains(d.getDate()))).toList();
        return new MonthView(month.toString(),selected.size(),6,15,2,view);
    }

    @Transactional
    public MonthView replaceMonth(Long employeeId,YearMonth month,List<LocalDate> requestedDates){
        validateNextMonth(month);
        var unique=new TreeSet<>(requestedDates==null?List.of():requestedDates);
        if(unique.size()<6||unique.size()>15)throw bad("每月必須選擇 6 至 15 天");
        if(unique.stream().anyMatch(d->!YearMonth.from(d).equals(month)))throw bad("所有日期都必須屬於指定月份");
        var user=users.findById(employeeId).filter(AppUser::isActive).filter(u->u.getRole()==AppUser.Role.EMPLOYEE).orElseThrow(()->bad("找不到有效的員工帳號"));
        var start=month.atDay(1); var end=month.atEndOfMonth();
        var locked=days.lockMonth(start,end);
        if(locked.size()!=month.lengthOfMonth())throw bad("日曆資料尚未準備完成");
        var invalid=locked.stream().filter(d->unique.contains(d.getDate())&&!d.isSchedulable()).map(CalendarDay::getDate).toList();
        if(!invalid.isEmpty())throw bad("包含不可排班日期："+invalid);
        entries.deleteEmployeeMonth(employeeId,start,end); entries.flush();
        var counts=counts(start,end);
        var full=unique.stream().filter(d->counts.getOrDefault(d,0L)>=2).toList();
        if(!full.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"以下日期已額滿："+full);
        var byDate=locked.stream().collect(Collectors.toMap(CalendarDay::getDate,d->d));
        entries.saveAll(unique.stream().map(d->new ScheduleEntry(user,byDate.get(d))).toList());
        return employeeMonth(employeeId,month);
    }

    private Map<LocalDate,Long> counts(LocalDate start,LocalDate end){return entries.countByDateBetween(start,end).stream().collect(Collectors.toMap(r->(LocalDate)r[0],r->(Long)r[1]));}
    private void validateNextMonth(YearMonth month){var allowed=YearMonth.now(TAIPEI).plusMonths(1);if(!allowed.equals(month))throw bad("目前只能排定 "+allowed+" 的班表");}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    public record DayView(LocalDate date,String dayType,String holidayName,boolean schedulable,int assignedCount,boolean selected){}
    public record MonthView(String month,int selectedCount,int minimumDays,int maximumDays,int dailyCapacity,List<DayView> days){}
}
