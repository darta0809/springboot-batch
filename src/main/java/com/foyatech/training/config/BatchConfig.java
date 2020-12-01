package com.foyatech.training.config;

import java.util.Arrays;
import java.util.Set;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.foyatech.training.dao.TrainingDao;
import com.foyatech.training.listener.JobCompletionListener;
import com.foyatech.training.tasklet.CsvDataTasklet;
import com.foyatech.training.tasklet.FixDataTasklet;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Configuration
@EnableBatchProcessing
public class BatchConfig extends DefaultBatchConfigurer {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private ApplicationArguments shellInput;
	
	@Autowired
	private TrainingDao trainingDao;
	
	@Value("${format-config-file}")
	private String configFile;
	
	@Value("${input-file-path}/")
	private String inputPath;

	@Value(".${input-file-extension}")
	private String inputExtension;

	@Value("${output-file-path}/")
	private String outputPath;

	@Value(".${output-file-extension}")
	private String outputExtension;

	@Value("${error-file-path}/")
	private String errPath;

	@Value(".${error-file-extension}")
	private String errExtension;

	/*
	 * 設置內存數據庫 createJobRepository
	 * 設置 TransactionManager 以 Map 方式
	 * 內存中的存儲庫是易失性的，因此不允許在JVM實例之間重新啟動。
	 * 它還不能保證同時啟動兩個具有相同參數的作業實例，並且不適合在多線程Job或本地分區中使用Step
	 * */
	@Bean
	@Override
	public PlatformTransactionManager getTransactionManager() {
		return new ResourcelessTransactionManager();
	}

	@Bean
	@Override
	public JobRepository createJobRepository() throws Exception {
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
		factory.afterPropertiesSet();
	    factory.setTransactionManager(getTransactionManager());
	    return factory.getObject();
	}
	
	@Bean
	public Job job() {
		
		Set<String> argsNames = shellInput.getOptionNames();
		log.info("shellInput argsNames : " + argsNames);
		String[] argsValues = shellInput.getSourceArgs();
		log.info("shellInput argsValues : " + Arrays.asList(argsValues));
		String fileType = shellInput.getSourceArgs()[0];
		log.info("fileType : " + fileType);
		if("CSV".equals(fileType)) {
			return jobBuilderFactory
					.get("job")
					.incrementer(new RunIdIncrementer())
					.listener(new JobCompletionListener(configFile, fileType, trainingDao))
					.flow(step1(fileType, trainingDao))
					.end()
					.build();	
		}else if("FIX".equals(fileType)) {
			return jobBuilderFactory
					.get("job")
					.incrementer(new RunIdIncrementer())
					.listener(new JobCompletionListener(configFile, fileType, trainingDao))
					.flow(step2(fileType, trainingDao))
					.end()
					.build();
		}else {
			log.error(fileType + " 不是正確的格式，請輸入 CSV 或 FIX");
			System.exit(1);
		}
		return null;
	}

	@Bean
	public Step step1(@Value("CSV") String fileType, TrainingDao trainingDao) {
		return stepBuilderFactory
				.get("step1")
				.tasklet(csvDataTasklet(fileType, trainingDao))	
				.build();
	}
	
	@Bean
	public Step step2(@Value("FIX") String fileType, TrainingDao trainingDao) {
		return stepBuilderFactory
				.get("step2")
				.tasklet(fixDataTasklet(fileType, trainingDao))
				.build();
	}
	
	@Bean
	protected Tasklet csvDataTasklet(String fileType, TrainingDao trainingDao) {
		return new CsvDataTasklet(fileType, inputPath, inputExtension, outputPath, outputExtension, errPath, errExtension, trainingDao);
	}
	
	@Bean
	protected Tasklet fixDataTasklet(String fileType, TrainingDao trainingDao) {
		return new FixDataTasklet(fileType, inputPath, inputExtension, outputPath, outputExtension, errPath, errExtension, trainingDao);
	}

}
