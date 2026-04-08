package com.hospital.ordersystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides a {@link Clock} bean so that all time-sensitive components
 * (DeadlineFirstTriageStrategy, PriorityEscalationDecorator) receive an
 * injectable clock that tests can substitute with a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
