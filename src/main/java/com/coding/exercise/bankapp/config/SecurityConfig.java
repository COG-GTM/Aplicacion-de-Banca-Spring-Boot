package com.coding.exercise.bankapp.config;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * 
 * Requires HTTP Basic authentication for every request except the root path and
 * the actuator health check. The h2-console is reachable (framed, without CSRF)
 * only after authenticating. Startup fails if no password is configured.
 * 
 * @author sbathina
 *
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public SecurityConfig(SecurityProperties securityProperties) {
        if (securityProperties.getUser().isPasswordGenerated()) {
            throw new IllegalStateException(
                    "No API password configured: set the BANKAPP_PASSWORD environment variable");
        }
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests()
                .antMatchers("/", "/actuator/health").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        httpSecurity.csrf().disable();
        httpSecurity.headers().frameOptions().sameOrigin();
    }
}
