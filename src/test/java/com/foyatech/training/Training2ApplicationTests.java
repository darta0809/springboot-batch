package com.foyatech.training;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.foyatech.training.dao.TrainingDao;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
class Training2ApplicationTests {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Test
	void contextLoads() {
		/*
		DriverManagerDataSource dataSource=new DriverManagerDataSource(); 
		dataSource.setDriverClassName("oracle.jdbc.OracleDriver"); 
		dataSource.setUrl("jdbc:oracle:thin:@10.1.1.205:1521:iotbs");
		dataSource.setUsername("BT3500"); 
		dataSource.setPassword("BT3500"); 
		
		JdbcTemplate jdbcTemplate=new JdbcTemplate(dataSource);
*/
		jdbcTemplate.afterPropertiesSet();
		
	}

}
