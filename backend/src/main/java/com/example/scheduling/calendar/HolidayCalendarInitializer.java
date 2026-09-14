package com.example.scheduling.calendar;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.scheduling.calendar.CalendarDay.DayType;
import com.example.scheduling.calendar.NtpcHolidayClient.HolidayRecord;

@Component
public class HolidayCalendarInitializer implements ApplicationRunner {
    static final String API_SOURCE = "新北市政府資料開放平臺";
    static final String RULE_SOURCE = "系統週末規則";

    private static final Logger log = LoggerFactory.getLogger(HolidayCalendarInitializer.class);
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final CalendarDayRepository repository;
    private final NtpcHolidayClient client;
    private final Clock clock;

    @Autowired
    public HolidayCalendarInitializer(CalendarDayRepository repository, NtpcHolidayClient client) {
        this(repository, client, Clock.system(TAIPEI));
    }

    HolidayCalendarInitializer(CalendarDayRepository repository, NtpcHolidayClient client, Clock clock) {
        this.repository = repository;
        this.client = client;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        var start = LocalDate.now(clock).withDayOfYear(1);
        var end = start.plusYears(2).minusDays(1);

        try {
            var officialRecords = index(client.fetchAll());
            var officialYears = completeYears(officialRecords);
            if (!officialYears.contains(start.getYear())) {
                throw new IllegalStateException("Holiday API returned incomplete data for " + start.getYear());
            }
            var existing = repository.findByDateBetweenOrderByDate(start, end).stream()
                    .collect(Collectors.toMap(CalendarDay::getDate, Function.identity()));
            var days = new ArrayList<CalendarDay>();
            for (var date = start; !date.isAfter(end); date = date.plusDays(1)) {
                days.add(dayFor(date, officialRecords, officialYears, existing));
            }
            repository.saveAll(days);
            log.info("Synchronized {} calendar days from the NTPC holiday API", days.size());
        } catch (RuntimeException ex) {
            seedMissingDays(start, end);
            log.warn("Holiday API synchronization skipped; retained cached holiday data: {}", ex.getMessage());
            log.debug("Holiday API synchronization failure", ex);
        }
    }

    private Set<Integer> completeYears(Map<LocalDate, HolidayRecord> officialRecords) {
        return officialRecords.keySet().stream()
                .collect(Collectors.groupingBy(LocalDate::getYear, Collectors.toSet()))
                .entrySet().stream()
                .filter(entry -> isCompleteYear(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private boolean isCompleteYear(int year, Set<LocalDate> dates) {
        var weekendCount = 0;
        for (var date = LocalDate.of(year, 1, 1); date.getYear() == year; date = date.plusDays(1)) {
            var weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (weekend) {
                weekendCount++;
            }
            if (weekend && !dates.contains(date)) {
                return false;
            }
        }
        return dates.size() > weekendCount;
    }

    private CalendarDay dayFor(LocalDate date, Map<LocalDate, HolidayRecord> officialRecords,
            Set<Integer> officialYears, Map<LocalDate, CalendarDay> existing) {
        if (officialYears.contains(date.getYear())) {
            return toCalendarDay(date, officialRecords.get(date));
        }
        return existing.getOrDefault(date, defaultDay(date));
    }

    private Map<LocalDate, HolidayRecord> index(Iterable<HolidayRecord> records) {
        var result = new HashMap<LocalDate, HolidayRecord>();
        for (var record : records) {
            try {
                result.put(LocalDate.parse(record.date(), API_DATE_FORMAT), record);
            } catch (DateTimeParseException | NullPointerException ex) {
                log.warn("Ignored holiday API record with invalid date: {}", record.date());
            }
        }
        return result;
    }

    private CalendarDay toCalendarDay(LocalDate date, HolidayRecord record) {
        if (record == null) {
            return defaultDay(date);
        }

        var calendarWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (record.holidaycategory() != null && "特定節日".equals(record.holidaycategory().trim())) {
            return new CalendarDay(date, DayType.HOLIDAY, holidayName(record), !calendarWeekend, API_SOURCE);
        }

        if (!"是".equals(record.isholiday())) {
            return new CalendarDay(date, DayType.WORKDAY, null, true, API_SOURCE);
        }

        var weekend = record.holidaycategory() != null && record.holidaycategory().contains("星期六、星期日");
        var name = weekend && !hasText(record.name()) ? null : holidayName(record);
        var type = weekend && name == null ? DayType.WEEKEND : DayType.HOLIDAY;
        return new CalendarDay(date, type, name, false, API_SOURCE);
    }

    private String holidayName(HolidayRecord record) {
        var name = hasText(record.name()) ? record.name().trim() : null;
        var category = hasText(record.holidaycategory()) ? record.holidaycategory().trim() : null;
        if (category != null && category.contains("補假")) {
            if (name == null) {
                return category;
            }
            return name.contains("補假") ? name : name + "補假";
        }
        return name != null ? name : category != null ? category : "放假日";
    }

    private void seedMissingDays(LocalDate start, LocalDate end) {
        var existing = repository.findByDateBetweenOrderByDate(start, end);
        var existingDates = existing.stream().map(CalendarDay::getDate).collect(Collectors.toSet());
        var missing = new ArrayList<CalendarDay>();
        for (var date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!existingDates.contains(date)) {
                missing.add(defaultDay(date));
            }
        }
        if (!missing.isEmpty()) {
            repository.saveAll(missing);
        }
    }

    private CalendarDay defaultDay(LocalDate date) {
        var weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        return new CalendarDay(date, weekend ? DayType.WEEKEND : DayType.WORKDAY, null, !weekend, RULE_SOURCE);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
