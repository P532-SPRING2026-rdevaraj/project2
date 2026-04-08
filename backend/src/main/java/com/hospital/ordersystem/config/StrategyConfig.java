package com.hospital.ordersystem.config;

import com.hospital.ordersystem.strategy.DeadlineFirstTriageStrategy;
import com.hospital.ordersystem.strategy.LoadBalancingTriageStrategy;
import com.hospital.ordersystem.strategy.PriorityFirstTriageStrategy;
import com.hospital.ordersystem.strategy.TriageStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/**
 * Registers all three TriageStrategy implementations as named beans so
 * OrderManager can look them up by strategy name at runtime.
 *
 * The PRIORITY_FIRST bean delegates to the existing @Component and is marked
 * @Primary so TriagingEngine's constructor injection resolves unambiguously
 * without any modification to PriorityFirstTriageStrategy or TriagingEngine.
 */
@Configuration
public class StrategyConfig {

    @Bean("PRIORITY_FIRST")
    @Primary
    TriageStrategy priorityFirstStrategy(PriorityFirstTriageStrategy existing) {
        return existing;
    }

    @Bean("LOAD_BALANCING")
    TriageStrategy loadBalancingStrategy() {
        return new LoadBalancingTriageStrategy();
    }

    @Bean("DEADLINE_FIRST")
    TriageStrategy deadlineFirstStrategy(Clock clock) {
        return new DeadlineFirstTriageStrategy(clock);
    }
}
