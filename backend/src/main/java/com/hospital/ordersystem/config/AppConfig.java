package com.hospital.ordersystem.config;

import com.hospital.ordersystem.observer.NotificationObserver;
import com.hospital.ordersystem.observer.OrderEventPublisher;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the Observer chain on startup and configures CORS so the
 * GitHub-Pages-hosted frontend can call the Render-hosted backend.
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final OrderEventPublisher eventPublisher;
    private final NotificationObserver notificationObserver;

    public AppConfig(OrderEventPublisher eventPublisher,
                     NotificationObserver notificationObserver) {
        this.eventPublisher = eventPublisher;
        this.notificationObserver = notificationObserver;
    }

    @PostConstruct
    public void registerObservers() {
        eventPublisher.subscribe(notificationObserver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
