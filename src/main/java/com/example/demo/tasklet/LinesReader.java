package com.example.demo.tasklet;

import com.example.demo.model.FileInfoCsv;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Log4j2
public class LinesReader implements Tasklet, StepExecutionListener {

  private List<FileInfoCsv> fileInfos;
  private File file;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    this.fileInfos = new ArrayList<>();
    this.file = new File("C:/Intellij_workspace/springbootbatch/input/data.csv");
    log.debug("Lines Reader initialized.");
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
      throws Exception {
    Reader reader = new BufferedReader(new FileReader(file));
    CsvToBean<FileInfoCsv> csvToBean = new CsvToBeanBuilder(reader)
            .withType(FileInfoCsv.class)
            .withIgnoreLeadingWhiteSpace(true)
            .build();
    this.fileInfos = csvToBean.parse();

    return RepeatStatus.FINISHED;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    stepExecution.getJobExecution().getExecutionContext().put("data", fileInfos);
    log.debug("Lines Reader ended.");
    return ExitStatus.COMPLETED;
  }


}
