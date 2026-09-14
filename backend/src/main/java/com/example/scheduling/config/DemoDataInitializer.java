package com.example.scheduling.config;

import com.example.scheduling.calendar.*;
import com.example.scheduling.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.*;

@Configuration
public class DemoDataInitializer {
    @Bean CommandLineRunner demoData(UserRepository users, CalendarDayRepository days, PasswordEncoder encoder,@Value("${app.demo-data}") boolean enabled){
        return args->{ if(!enabled)return;
            seedUser(users,encoder,"owner@example.com","王店長",AppUser.Role.OWNER,"Owner123!");
            seedUser(users,encoder,"amy@example.com","陳小美",AppUser.Role.EMPLOYEE,"Employee123!");
            seedUser(users,encoder,"ben@example.com","林志明",AppUser.Role.EMPLOYEE,"Employee123!");
            seedUser(users,encoder,"cindy@example.com","張雅婷",AppUser.Role.EMPLOYEE,"Employee123!");
            var holidays=Map.ofEntries(
                Map.entry(LocalDate.of(2026,10,9),"國慶日補假"),Map.entry(LocalDate.of(2026,10,10),"國慶日"),
                Map.entry(LocalDate.of(2026,10,25),"臺灣光復暨金門古寧頭大捷紀念日"),Map.entry(LocalDate.of(2026,10,26),"光復節補假"),
                Map.entry(LocalDate.of(2026,12,25),"行憲紀念日"),Map.entry(LocalDate.of(2027,1,1),"開國紀念日"));
            var start=LocalDate.now(ZoneId.of("Asia/Taipei")).withDayOfYear(1); var end=start.plusYears(2).minusDays(1);
            var batch=new ArrayList<CalendarDay>();
            for(var d=start;!d.isAfter(end);d=d.plusDays(1)){if(days.existsById(d))continue; var weekend=d.getDayOfWeek()==DayOfWeek.SATURDAY||d.getDayOfWeek()==DayOfWeek.SUNDAY; var name=holidays.get(d); var type=name!=null?CalendarDay.DayType.HOLIDAY:weekend?CalendarDay.DayType.WEEKEND:CalendarDay.DayType.WORKDAY; batch.add(new CalendarDay(d,type,name,!weekend&&name==null,"行政院人事行政總處／系統週末規則"));}
            days.saveAll(batch);
        };
    }
    private void seedUser(UserRepository repo,PasswordEncoder encoder,String email,String name,AppUser.Role role,String password){if(repo.findByEmailIgnoreCase(email).isEmpty())repo.save(new AppUser(email,encoder.encode(password),name,role));}
}
