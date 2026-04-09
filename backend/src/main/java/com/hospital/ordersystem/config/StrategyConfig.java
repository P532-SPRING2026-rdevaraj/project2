package com.hospital.ordersystem.config;

import com.hospital.ordersystem.strategy.DeadlineFirstTriageStrategy;
import com.hospital.ordersystem.strategy.LoadBalancingTriageStrategy;
import com.hospital.ordersystem.strategy.PriorityFirstTriageStrategy;
import com.hospital.ordersystem.strategy.TriageStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

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
