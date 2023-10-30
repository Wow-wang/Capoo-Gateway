package org.wow.backend.http.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-20 21:57
 **/

//@Configuration
//@EnableWebSecurity
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .authorizeRequests()
//                .antMatchers("/http-server/ping").hasIpAddress("172.23.208.1")
//                .anyRequest().authenticated()
//                .and()
//                .httpBasic();
//    }
//
//
//}