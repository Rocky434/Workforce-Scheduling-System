package com.example.scheduling.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;

@RestController @RequestMapping("/api/schedules") @PreAuthorize("hasRole('EMPLOYEE')")
public class ScheduleController {
    private final ScheduleService service;
    public ScheduleController(ScheduleService service){this.service=service;}
    @GetMapping("/me") public ScheduleService.MonthView mine(@RequestParam(required=false) String month,@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt){return service.employeeMonth(Long.valueOf(jwt.getSubject()),parseMonth(month));}
    @PutMapping("/me") public ScheduleService.MonthView replace(@Valid @RequestBody UpdateScheduleRequest request,@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt){return service.replaceMonth(Long.valueOf(jwt.getSubject()),YearMonth.parse(request.month()),request.dates());}
    private YearMonth parseMonth(String month){return month==null?YearMonth.now(ZoneId.of("Asia/Taipei")).plusMonths(1):YearMonth.parse(month);}
    public record UpdateScheduleRequest(@NotBlank @Pattern(regexp="\\d{4}-\\d{2}",message="月份格式必須是 YYYY-MM") String month,@NotNull List<LocalDate> dates){}
}

