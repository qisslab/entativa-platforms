package com.entativa.id.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
import java.time.Duration

/**
 * Enterprise Security Configuration for Entativa ID
 * 
 * @author Neo Qiss  
 * @status Production-ready security configuration
 */
@Configuration
@EnableWebSecurity
@ConfigurationProperties(prefix = "entativa.security")
class SecurityConfig {
    
    var jwt = JwtProperties()
    var oauth = OAuthProperties()
    var session = SessionProperties()
    var cors = CorsProperties()
    var rateLimit = RateLimitProperties()
    
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(12) // Strong bcrypt rounds
    }
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() } // Disabled for API, using JWT
            .sessionManagement { 
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) 
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .headers { headers ->
                headers
                    .frameOptions { it.deny() }
                    .contentTypeOptions { }
                    .httpStrictTransportSecurity { hsts ->
                        hsts
                            .maxAgeInSeconds(31536000) // 1 year
                            .includeSubdomains(true)
                            .preload(true)
                    }
                    .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    .addHeaderWriter(XXssProtectionHeaderWriter())
                    .cacheControl { }
            }
            .authorizeHttpRequests { requests ->
                requests
                    // Public health endpoints
                    .requestMatchers("/health/**", "/metrics/**").permitAll()
                    
                    // Public auth endpoints
                    .requestMatchers("/api/v1/auth/login").permitAll()
                    .requestMatchers("/api/v1/auth/register").permitAll()
                    .requestMatchers("/api/v1/auth/refresh").permitAll()
                    .requestMatchers("/api/v1/auth/forgot-password").permitAll()
                    .requestMatchers("/api/v1/auth/reset-password").permitAll()
                    .requestMatchers("/api/v1/auth/verify-email").permitAll()
                    .requestMatchers("/api/v1/auth/verify-phone").permitAll()
                    
                    // OAuth2 endpoints
                    .requestMatchers("/oauth2/authorize").permitAll()
                    .requestMatchers("/oauth2/token").permitAll()
                    .requestMatchers("/oauth2/revoke").permitAll()
                    .requestMatchers("/oauth2/introspect").permitAll()
                    .requestMatchers("/.well-known/oauth-authorization-server").permitAll()
                    .requestMatchers("/.well-known/openid_configuration").permitAll()
                    .requestMatchers("/oauth2/jwks").permitAll()
                    
                    // Admin endpoints require admin role
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    
                    // Account management requires authentication
                    .requestMatchers("/api/v1/account/**").authenticated()
                    
                    // App management requires authentication
                    .requestMatchers("/api/v1/apps/**").authenticated()
                    
                    // Sync endpoints require authentication
                    .requestMatchers("/api/v1/sync/**").authenticated()
                    
                    // All other requests require authentication
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                }
            }
            .build()
    }
    
    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder.withJwkSetUri(jwt.jwkSetUri)
            .jwsAlgorithm(jwt.algorithm)
            .cache(Duration.ofMinutes(5))
            .build()
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        
        configuration.allowedOriginPatterns = cors.allowedOrigins.toMutableList()
        configuration.allowedMethods = cors.allowedMethods.toMutableList()
        configuration.allowedHeaders = cors.allowedHeaders.toMutableList()
        configuration.exposedHeaders = cors.exposedHeaders.toMutableList()
        configuration.allowCredentials = cors.allowCredentials
        configuration.maxAge = cors.maxAge
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
    
    // Configuration Properties Classes
    class JwtProperties {
        var issuer: String = "https://id.entativa.com"
        var audience: String = "entativa-api"
        var jwkSetUri: String = "https://id.entativa.com/oauth2/jwks"
        var algorithm: org.springframework.security.oauth2.jose.jws.SignatureAlgorithm = 
            org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256
        var accessTokenExpiry: Duration = Duration.ofMinutes(15)
        var refreshTokenExpiry: Duration = Duration.ofDays(30)
        var clockSkew: Duration = Duration.ofMinutes(2)
    }
    
    class OAuthProperties {
        var authorizationCodeExpiry: Duration = Duration.ofMinutes(10)
        var deviceCodeExpiry: Duration = Duration.ofMinutes(10)
        var deviceCodeInterval: Duration = Duration.ofSeconds(5)
        var pkceRequired: Boolean = true
        var consentRequired: Boolean = true
        var multipleResponseTypes: Boolean = true
        var requireProofKey: Boolean = true
    }
    
    class SessionProperties {
        var maxConcurrentSessions: Int = 5
        var sessionTimeout: Duration = Duration.ofHours(24)
        var rememberMeTimeout: Duration = Duration.ofDays(30)
        var sessionCookieName: String = "ENTATIVA_SESSION"
        var sessionCookieSecure: Boolean = true
        var sessionCookieHttpOnly: Boolean = true
        var sessionCookieSameSite: String = "Strict"
    }
    
    class CorsProperties {
        var allowedOrigins: List<String> = listOf(
            "https://entativa.com",
            "https://*.entativa.com",
            "https://localhost:3000",
            "https://localhost:8080"
        )
        var allowedMethods: List<String> = listOf(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        )
        var allowedHeaders: List<String> = listOf(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "Origin", 
            "X-Requested-With",
            "X-API-Key",
            "X-Client-Version"
        )
        var exposedHeaders: List<String> = listOf(
            "X-Total-Count",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset"
        )
        var allowCredentials: Boolean = true
        var maxAge: Long = 3600
    }
    
    class RateLimitProperties {
        var loginAttempts = LoginRateLimit()
        var apiRequests = ApiRateLimit()
        var registration = RegistrationRateLimit()
        var passwordReset = PasswordResetRateLimit()
        
        class LoginRateLimit {
            var maxAttempts: Int = 5
            var windowMinutes: Int = 15
            var lockoutMinutes: Int = 30
        }
        
        class ApiRateLimit {
            var requestsPerMinute: Int = 100
            var requestsPerHour: Int = 5000
            var requestsPerDay: Int = 50000
        }
        
        class RegistrationRateLimit {
            var maxRegistrations: Int = 3
            var windowHours: Int = 24
        }
        
        class PasswordResetRateLimit {
            var maxRequests: Int = 3
            var windowHours: Int = 1
        }
    }
}
