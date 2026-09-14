package com.example.scheduling.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.scheduling.calendar.CalendarDay;
import com.example.scheduling.calendar.CalendarDay.DayType;
import com.example.scheduling.calendar.CalendarDayRepository;
import com.example.scheduling.schedule.ScheduleEntryRepository;
import com.example.scheduling.user.AppUser;
import com.example.scheduling.user.UserRepository;

class OwnerReportControllerTest {

    @Test
    void dashboardIncludesHolidayAndWeekendDetails() {
        var entries = mock(ScheduleEntryRepository.class);
        var users = mock(UserRepository.class);
        var days = mock(CalendarDayRepository.class);
        when(users.findAllByRoleAndActiveTrueOrderByDisplayName(AppUser.Role.EMPLOYEE)).thenReturn(List.of());
        when(entries.findDetailedBetween(any(), any())).thenReturn(List.of());
        when(days.findByDateBetweenOrderByDate(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(
                        new CalendarDay(LocalDate.of(2026, 10, 9), DayType.HOLIDAY, "補假", false, "api"),
                        new CalendarDay(LocalDate.of(2026, 10, 10), DayType.HOLIDAY, "國慶日", false, "api"),
                        new CalendarDay(LocalDate.of(2026, 10, 11), DayType.WEEKEND, null, false, "api")));

        var dashboard = new OwnerReportController(entries, users, days).dashboard("2026-10");

        assertEquals(3, dashboard.days().size());
        assertEquals("補假", dashboard.days().get(0).holidayName());
        assertEquals("HOLIDAY", dashboard.days().get(1).dayType());
        assertEquals("國慶日", dashboard.days().get(1).holidayName());
        assertEquals("WEEKEND", dashboard.days().get(2).dayType());
        assertFalse(dashboard.days().get(2).schedulable());
    }
}
