package com.example.demo.config;

//@Configuration
//@EnableBatchProcessing
public class SpringBatchConfig /*extends DefaultBatchConfigurer */ {

//  @Bean
//  @BatchDataSource
//  @ConfigurationProperties("spring.datasource")
//  public DataSource dataSource() {
//    return DataSourceBuilder.create().build();
//  }

//  @Bean
//  @Override
//  public PlatformTransactionManager getTransactionManager() {
//    return new ResourcelessTransactionManager();
//  }

//  @Bean
//  @Override
//  public JobRepository createJobRepository() throws Exception {
//    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
//    factory.setDataSource(dataSource());
//    factory.setTransactionManager(getTransactionManager());
//    return factory.getObject();
//  }

//  @Bean
//  @Override
//  public JobLauncher createJobLauncher() throws Exception {
//    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
//    jobLauncher.setJobRepository(createJobRepository());
//    jobLauncher.afterPropertiesSet();
//    return jobLauncher;
//  }
}
