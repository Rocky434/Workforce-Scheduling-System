package com.example.scheduling.schedule;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry,Long> {
    @Query("select e from ScheduleEntry e join fetch e.employee join fetch e.calendarDay where e.calendarDay.date between :start and :end order by e.calendarDay.date, e.employee.displayName")
    List<ScheduleEntry> findDetailedBetween(@Param("start") LocalDate start,@Param("end") LocalDate end);
    @Query("select e from ScheduleEntry e join fetch e.calendarDay where e.employee.id=:employeeId and e.calendarDay.date between :start and :end")
    List<ScheduleEntry> findEmployeeMonth(@Param("employeeId") Long employeeId,@Param("start") LocalDate start,@Param("end") LocalDate end);
    @Query("select e.calendarDay.date, count(e) from ScheduleEntry e where e.calendarDay.date between :start and :end group by e.calendarDay.date")
    List<Object[]> countByDateBetween(@Param("start") LocalDate start,@Param("end") LocalDate end);
    @Modifying @Query("delete from ScheduleEntry e where e.employee.id=:employeeId and e.calendarDay.date between :start and :end")
    void deleteEmployeeMonth(@Param("employeeId") Long employeeId,@Param("start") LocalDate start,@Param("end") LocalDate end);
}
