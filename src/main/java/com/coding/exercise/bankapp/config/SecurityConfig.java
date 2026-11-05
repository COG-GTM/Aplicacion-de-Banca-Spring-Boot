package com.coding.exercise.bankapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Security configuration.
 *
 * The H2 web console is disabled by default and only registered under the
 * {@code local} profile (see application-local.yml). Even then it is never
 * public: it requires HTTP Basic authentication and may only be framed by
 * pages from the same origin.
 *
 * @author sbathina
 *
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    static final String H2_CONSOLE_PATTERN = "/h2-console/**";

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeRequests()
                    .antMatchers(H2_CONSOLE_PATTERN).authenticated()
                    .antMatchers("/").permitAll()
                .and()
                .httpBasic()
                .and()
                // Stateless JSON API without cookie-based authentication.
                .csrf().disable()
                .headers().frameOptions().sameOrigin();
    }
}
