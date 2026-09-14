package com.example.scheduling.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.scheduling.calendar.CalendarDay.DayType;
import com.example.scheduling.calendar.NtpcHolidayClient.HolidayRecord;

class HolidayCalendarInitializerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneId.of("Asia/Taipei"));

    @Test
    void importsHolidayNamesAndWorkingWeekendsFromApi() {
        var repository = mock(CalendarDayRepository.class);
        var client = mock(NtpcHolidayClient.class);
        when(repository.findByDateBetweenOrderByDate(any(), any())).thenReturn(List.of());
        when(client.fetchAll()).thenReturn(completeYearRecords());

        new HolidayCalendarInitializer(repository, client, CLOCK).run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CalendarDay>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        var days = captor.getValue().stream()
                .map(CalendarDay.class::cast)
                .collect(Collectors.toMap(CalendarDay::getDate, Function.identity()));

        var nationalDay = days.get(LocalDate.of(2026, 10, 10));
        assertEquals(DayType.HOLIDAY, nationalDay.getDayType());
        assertEquals("國慶日", nationalDay.getHolidayName());
        assertFalse(nationalDay.isSchedulable());

        var weekend = days.get(LocalDate.of(2026, 10, 11));
        assertEquals(DayType.WEEKEND, weekend.getDayType());
        assertNull(weekend.getHolidayName());

        var compensatoryDay = days.get(LocalDate.of(2026, 10, 9));
        assertEquals(DayType.HOLIDAY, compensatoryDay.getDayType());
        assertEquals("補假", compensatoryDay.getHolidayName());

        var namedCompensatoryDay = days.get(LocalDate.of(2026, 2, 20));
        assertEquals(DayType.HOLIDAY, namedCompensatoryDay.getDayType());
        assertEquals("春節補假", namedCompensatoryDay.getHolidayName());
        assertFalse(namedCompensatoryDay.isSchedulable());

        var adjustedWorkday = days.get(LocalDate.of(2026, 10, 17));
        assertEquals(DayType.WORKDAY, adjustedWorkday.getDayType());
        assertTrue(adjustedWorkday.isSchedulable());
    }

    @Test
    void rejectsPartialApiResponseInsteadOfErasingCachedHolidays() {
        var repository = mock(CalendarDayRepository.class);
        var client = mock(NtpcHolidayClient.class);
        var cached = new CalendarDay(LocalDate.of(2026, 10, 10), DayType.HOLIDAY, "快取國慶日", false, "cache");
        when(client.fetchAll()).thenReturn(List.of(record("20261010", "國慶日", "是", "放假之紀念日及節日")));
        when(repository.findByDateBetweenOrderByDate(any(), any())).thenReturn(List.of(cached));

        new HolidayCalendarInitializer(repository, client, CLOCK).run(null);

        verify(repository).findByDateBetweenOrderByDate(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CalendarDay>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertFalse(captor.getValue().stream().anyMatch(day -> day.getDate().equals(cached.getDate())));
    }

    @Test
    void keepsCachedDataWhenApiIsUnavailable() {
        var repository = mock(CalendarDayRepository.class);
        var client = mock(NtpcHolidayClient.class);
        when(client.fetchAll()).thenThrow(new IllegalStateException("offline"));
        when(repository.findByDateBetweenOrderByDate(any(), any())).thenReturn(List.of());

        new HolidayCalendarInitializer(repository, client, CLOCK).run(null);

        verify(repository).findByDateBetweenOrderByDate(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31));
        verify(repository).saveAll(any());
    }

    private HolidayRecord record(String date, String name, String isHoliday, String category) {
        return new HolidayRecord(date, date.substring(0, 4), name, isHoliday, category, null);
    }

    private List<HolidayRecord> completeYearRecords() {
        var records = new ArrayList<HolidayRecord>();
        for (var date = LocalDate.of(2026, 1, 1); date.getYear() == 2026; date = date.plusDays(1)) {
            var weekend = date.getDayOfWeek().getValue() >= 6;
            if (weekend) {
                records.add(record(date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE), null,
                        "是", "星期六、星期日"));
            }
        }
        records.removeIf(item -> List.of("20260220", "20261009", "20261010", "20261011", "20261017")
                .contains(item.date()));
        records.add(record("20260220", "春節", "是", "補假"));
        records.add(record("20261010", "國慶日", "是", "放假之紀念日及節日"));
        records.add(record("20261011", null, "是", "星期六、星期日"));
        records.add(record("20261009", null, "是", "補假"));
        records.add(record("20261017", null, "否", "補行上班日"));
        return records;
    }
}
