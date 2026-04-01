package com.hospital.ordersystem.config;

import com.hospital.ordersystem.observer.OrderEventPublisher;
import com.hospital.ordersystem.observer.OrderObserver;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Wires the Observer chain on startup and configures CORS so the
 * GitHub-Pages-hosted frontend can call the Render-hosted backend.
 *
 * Adding a new observer requires ZERO changes here — just annotate the
 * new class with @Component and Spring will inject it automatically.
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final OrderEventPublisher eventPublisher;
    private final List<OrderObserver> observers;

    public AppConfig(OrderEventPublisher eventPublisher, List<OrderObserver> observers) {
        this.eventPublisher = eventPublisher;
        this.observers = observers;
    }

    @PostConstruct
    public void registerObservers() {
        observers.forEach(eventPublisher::subscribe);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
