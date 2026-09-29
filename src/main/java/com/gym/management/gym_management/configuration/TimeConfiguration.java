package com.gym.management.gym_management.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfiguration {
    @Bean
    public Clock applicationClock(@Value("${app.time-zone:UTC}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
