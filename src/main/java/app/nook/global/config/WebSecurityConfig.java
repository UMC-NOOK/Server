package app.nook.global.config;

import app.nook.user.filter.JwtExceptionFilter;
import app.nook.user.filter.JwtFilter;
import app.nook.user.handler.CustomAccessDeniedHandler;
import app.nook.user.handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableScheduling
public class WebSecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtFilter jwtAuthFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        // ===== 공개 리소스 =====
                        .requestMatchers(
                                "/",
                                "/docs/**",              // REST Docs
                                "/index.html",
                                "/api/auth/**",
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                );

        // JWT Filter Chain
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtExceptionFilter, JwtFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:8080",
                "https://nook-app.shop",
                "https://readingnook.netlify.app"
        ));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Set-Cookie");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

