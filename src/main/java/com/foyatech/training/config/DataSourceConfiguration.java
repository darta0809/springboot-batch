package com.foyatech.training.config;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

@Configuration
@EnableTransactionManagement
public class DataSourceConfiguration implements TransactionManagementConfigurer {

    @Bean
    @Primary
    @BatchDataSource
    @ConfigurationProperties("spring.datasource")
    DataSource dataSource() throws SQLException, UnsupportedEncodingException {
        return DataSourceBuilder.create().build();
    }
	@Bean
	public JdbcTemplate jdbcTemplate() throws SQLException, UnsupportedEncodingException {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
		return jdbcTemplate;
	}

	@Bean
	public PlatformTransactionManager txManager() throws SQLException, UnsupportedEncodingException {
		return new DataSourceTransactionManager(dataSource());
	}
	
	@Override
	public PlatformTransactionManager annotationDrivenTransactionManager() {
			try {
				return txManager();
			} catch (SQLException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;
	}
}
