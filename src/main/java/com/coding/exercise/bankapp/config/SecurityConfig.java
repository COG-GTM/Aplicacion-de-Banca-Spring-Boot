package com.coding.exercise.bankapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * 
 * Spring security denied access to h2-console.
 * This configuration will resolve 403 forbidden error when accessing h2-console.
 * 
 * Customer read/update/delete endpoints require an authenticated caller; fine-grained
 * authorization (admin or owning customer) is enforced with method security.
 * 
 * @author sbathina
 *
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests().antMatchers("/").permitAll().and()
                .authorizeRequests().antMatchers("/h2-console/**").permitAll().and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/customers/**").authenticated()
                .antMatchers(HttpMethod.PUT, "/customers/**").authenticated()
                .antMatchers(HttpMethod.DELETE, "/customers/**").authenticated();

        httpSecurity.httpBasic();
        httpSecurity.csrf().disable();
        httpSecurity.headers().frameOptions().disable();
    }
}
