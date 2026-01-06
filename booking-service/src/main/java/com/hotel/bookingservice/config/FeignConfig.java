package com.hotel.bookingservice.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,  // Connect timeout
                10, TimeUnit.SECONDS, // Read timeout
                true                   // Follow redirects
        );
    }

    @Bean
    public Retryer retryer() {
        // Retry 3 times with exponential backoff starting at 1 second
        return new Retryer.Default(1000, 5000, 3);
    }
}
