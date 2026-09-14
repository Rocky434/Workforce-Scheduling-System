package com.example.scheduling.schedule;

import com.example.scheduling.calendar.CalendarDayRepository;
import com.example.scheduling.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ScheduleServiceTest {
    private final ScheduleService service=new ScheduleService(mock(ScheduleEntryRepository.class),mock(CalendarDayRepository.class),mock(UserRepository.class));
    @Test void rejectsFewerThanSixDays(){var next=YearMonth.now(ZoneId.of("Asia/Taipei")).plusMonths(1);assertThrows(ResponseStatusException.class,()->service.replaceMonth(1L,next,List.of(next.atDay(1))));}
    @Test void rejectsMonthOtherThanNextMonth(){var current=YearMonth.now(ZoneId.of("Asia/Taipei"));assertThrows(ResponseStatusException.class,()->service.employeeMonth(1L,current));}
}
