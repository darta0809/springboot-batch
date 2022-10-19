package com.example.demo.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SpringBatchConfig extends DefaultBatchConfigurer {

//  @Bean
//  @BatchDataSource
//  @ConfigurationProperties("spring.datasource")
//  public DataSource dataSource() {
//    return DataSourceBuilder.create().build();
//  }

  @Bean
  @Override
  public PlatformTransactionManager getTransactionManager() {
    return new ResourcelessTransactionManager();
  }

  @Bean
  @Override
  public JobRepository createJobRepository() throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
//    factory.setDataSource(dataSource());
    factory.setTransactionManager(getTransactionManager());
    return factory.getObject();
  }

  @Bean
  @Override
  public JobLauncher createJobLauncher() throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setJobRepository(createJobRepository());
    return jobLauncher;
  }
}
