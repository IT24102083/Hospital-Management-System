package com.hospital.hospitalmanagementsystem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                // Public resources that don't require authentication
                .antMatchers("/", "/about", "/services", "/contact", "/pharmacy", "/api/medicines",
                        "/css/**", "/js/**", "/images/**", "/fonts/**", "/register", "/diagnostic").permitAll()
                // Role-based access
                .antMatchers("/patient/**").hasRole("PATIENT")
                .antMatchers("/doctor/**").hasRole("DOCTOR")
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/pharmacist/**").hasRole("PHARMACIST")
                .antMatchers("/receptionist/**").hasRole("RECEPTIONIST")
                .antMatchers("/dashboard/**").authenticated()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler())
                .permitAll()
                .and()
                .logout()
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
                .and()
                .exceptionHandling()
                .accessDeniedPage("/access-denied")
                .and()
                // Session management
                .sessionManagement()
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
                .and()
                // Session fixation protection
                .sessionFixation().migrateSession();

        // Disable CSRF for development
        http.csrf().disable();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    // Custom authentication success handler to store user info in session
    public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {

            HttpSession session = request.getSession();
            session.setAttribute("username", authentication.getName());
            session.setAttribute("authenticated", true);
            session.setAttribute("userRole", authentication.getAuthorities().iterator().next().getAuthority());
            session.setAttribute("currentDateTime", "2025-08-11 19:20:23");
            session.setAttribute("currentUser", "IT24102083");

            // Call the parent method to handle the redirect
            super.setDefaultTargetUrl("/");
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}