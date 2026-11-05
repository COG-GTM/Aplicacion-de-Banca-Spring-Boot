package com.coding.exercise.bankapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * 
 * Requires HTTP Basic authentication for every request except the root path and
 * the actuator health probe. The H2 console is also behind authentication and is
 * allowed to render in same-origin frames.
 * 
 * @author sbathina
 *
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests()
                .antMatchers("/", "/actuator/health").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();

        httpSecurity.csrf().disable();
        httpSecurity.headers().frameOptions().sameOrigin();
    }
}
