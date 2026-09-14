package com.example.scheduling.calendar;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NtpcHolidayClient {
    private final RestClient restClient;
    private final String apiUrl;

    public NtpcHolidayClient(
            @Value("${app.holiday-api-url}") String apiUrl) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.apiUrl = apiUrl;
    }

    List<HolidayRecord> fetchAll() {
        var separator = apiUrl.contains("?") ? "&" : "?";
        var uri = URI.create(apiUrl + separator + "page=0&size=10000");
        var records = restClient.get().uri(uri).retrieve().body(HolidayRecord[].class);
        return records == null ? List.of() : Arrays.asList(records);
    }

    public record HolidayRecord(
            String date,
            String year,
            String name,
            String isholiday,
            String holidaycategory,
            String description) {
    }
}
