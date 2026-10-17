package com.navas.gastosapi.config;

import com.navas.gastosapi.auth.JwtAuthenticationFilter;
import com.navas.gastosapi.auth.JwtService;
import com.navas.gastosapi.ratelimit.RateLimitFilter;
import com.navas.gastosapi.ratelimit.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, RateLimitProperties.class})
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RateLimitProperties rateLimit,
                                                   @Value("${app.hardening.enabled:true}") boolean hardening)
            throws Exception {

        // Os filtros nao sao @Bean de proposito, para nao serem registrados duas vezes
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(
                new RateLimiter(rateLimit.generalPerMinute(), Duration.ofMinutes(1)),
                new RateLimiter(rateLimit.authPerMinute(), Duration.ofMinutes(1)));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        if (hardening) {
            // Headers extras alem dos padrao do Spring Security (nosniff, X-Frame-Options, Cache-Control).
            // Desligavel com HARDENING_ENABLED=false para gerar o relatorio "antes" do ZAP.
            http.headers(headers -> {
                // CSP restritiva em todas as rotas, exceto o Swagger UI (que precisa de scripts e estilos)
                headers.addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                        request -> !request.getRequestURI().startsWith("/swagger-ui"),
                        new StaticHeadersWriter("Content-Security-Policy",
                                "default-src 'none'; frame-ancestors 'none'")));
                headers.addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "no-referrer"));
                headers.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        "geolocation=(), microphone=(), camera=()"));
                headers.addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Resource-Policy", "same-origin"));
            });
        }
        return http.build();
    }
}
