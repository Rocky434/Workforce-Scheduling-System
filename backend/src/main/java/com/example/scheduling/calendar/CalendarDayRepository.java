package com.example.scheduling.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;

public interface CalendarDayRepository extends JpaRepository<CalendarDay, LocalDate> {
    List<CalendarDay> findByDateBetweenOrderByDate(LocalDate start, LocalDate end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CalendarDay d where d.date in :dates order by d.date")
    List<CalendarDay> lockAll(@Param("dates") List<LocalDate> dates);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CalendarDay d where d.date between :start and :end order by d.date")
    List<CalendarDay> lockMonth(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
