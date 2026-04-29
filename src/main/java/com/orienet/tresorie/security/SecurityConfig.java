package com.orienet.tresorie.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeRequests()
                // Accès public
                .antMatchers("/login").permitAll()
                // Console H2 — accès public (développement)
                .antMatchers("/h2-console/**").permitAll()
                // Admin seulement
                .antMatchers("/admin/**").hasRole("ADMIN")
                // Utilisateur seulement (actions d'écriture)
                .antMatchers(
                    "/operations/nouvelle",
                    "/operations/sauvegarder",
                    "/operations/modifier/**",
                    "/operations/archiver/**",
                    "/archives/restaurer/**",
                    "/archives/supprimer/**",
                    "/champs/**"
                ).hasRole("UTILISATEUR")
                // Lecture : admin + utilisateur
                .antMatchers("/flux-tresorerie", "/archives", "/copilote").hasAnyRole("ADMIN", "UTILISATEUR")
                // Toute autre URL → authentifié
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/flux-tresorerie", true)
                .failureUrl("/login?error=true")
                .permitAll()
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            .and()
            // Désactiver CSRF pour la console H2 uniquement
            .csrf()
                .ignoringAntMatchers("/h2-console/**")
            .and()
            // Autoriser les iframes pour la console H2
            .headers()
                .frameOptions().sameOrigin();

        return http.build();
    }
}
