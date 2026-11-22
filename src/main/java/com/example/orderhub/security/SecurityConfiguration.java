package com.example.orderhub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless HTTP Basic security. Reads are public; partners place orders; admins manage
 * the catalog and order lifecycle.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

	static final String ADMIN = "ADMIN";

	static final String PARTNER = "PARTNER";

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
			.requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole(ADMIN)
			.requestMatchers("/error").permitAll()
			.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
			.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
			.requestMatchers("/api/products/**").hasRole(ADMIN)
			.requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasRole(ADMIN)
			.requestMatchers("/api/orders/**").hasAnyRole(PARTNER, ADMIN)
			.anyRequest().denyAll())
			.httpBasic(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}

	/** Delegating encoder: hashes with bcrypt today, stays able to verify/upgrade older hashes. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsService users(PasswordEncoder encoder,
			@Value("${orderhub.security.admin-password}") String adminPassword,
			@Value("${orderhub.security.partner-password}") String partnerPassword) {
		var admin = User.withUsername("admin").password(encoder.encode(adminPassword)).roles(ADMIN).build();
		var partner = User.withUsername("partner").password(encoder.encode(partnerPassword)).roles(PARTNER).build();
		return new InMemoryUserDetailsManager(admin, partner);
	}

}
