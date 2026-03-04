package com.helpdesk.config;

import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

/**
 * HibernateConfig — exposes the Hibernate SessionFactory and
 * HibernateTransactionManager as Spring beans via Java config,
 * complementing the XML declarations in applicationContext.xml.
 *
 * <p>DAOs and Services can inject {@code SessionFactory} directly via
 * {@code @Autowired} once this bean is registered in the Spring context.
 */
@Configuration
public class HibernateConfig {

    @Autowired
    private DataSource dataSource;

    /**
     * SessionFactory bean — scans all @Entity classes under com.helpdesk.model.
     * Hibernate properties mirror those in hibernate.cfg.xml for consistency.
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.helpdesk.model");

        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.put("hibernate.hbm2ddl.auto", "update"); // use 'validate' in prod
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.default_batch_fetch_size", "16"); // mitigates N+1 on comment trees
        props.put("hibernate.cache.use_second_level_cache", "false");
        factory.setHibernateProperties(props);

        return factory;
    }

    /**
     * Transaction manager wired to the SessionFactory above.
     * Enables @Transactional on service-layer methods.
     */
    @Bean
    public HibernateTransactionManager transactionManager(
        SessionFactory sessionFactory
    ) {
        return new HibernateTransactionManager(sessionFactory);
    }
}
