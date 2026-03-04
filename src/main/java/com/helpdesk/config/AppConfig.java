package com.helpdesk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

/**
 * AppConfig — Spring MVC Java configuration.
 *
 * <p>Scans only the {@code controller} package so that service/dao beans
 * defined in {@code applicationContext.xml} are not double-instantiated.
 * Configures JSP view resolution and static resource handling.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.helpdesk.controller")
public class AppConfig implements WebMvcConfigurer {

    /**
     * JSP view resolver.
     * Maps logical view names (e.g. "ticket-list") to
     * /WEB-INF/views/ticket-list.jsp.
     */
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        resolver.setOrder(1);
        return resolver;
    }

    /**
     * Serve static assets (CSS, JS, images) without hitting the DispatcherServlet.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
        registry.addResourceHandler("/images/**").addResourceLocations("/images/");
    }
}
