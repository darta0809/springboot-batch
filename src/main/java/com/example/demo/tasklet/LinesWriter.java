package com.example.demo.tasklet;

import com.example.demo.model.FileInfoCsv;
import com.example.demo.repository.FileInfoEntity;
import com.example.demo.repository.FileInfoRepository;
import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class LinesWriter implements Tasklet, StepExecutionListener {

  @Autowired
  private FileInfoRepository fileInfoRepository;

  private List<FileInfoEntity> fileInfoEntity;
  private List<FileInfoCsv> lines;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    ExecutionContext executionContext = stepExecution
            .getJobExecution()
            .getExecutionContext();
    this.fileInfoEntity = (List<FileInfoEntity>) executionContext.get("entity");
    this.lines = (List<FileInfoCsv>) executionContext.get("data");
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
          throws Exception {
    fileInfoRepository.saveAll(fileInfoEntity);

    File file = new File("C:/Intellij_workspace/springbootbatch/output/data.fix");

    FileUtils.writeLines(file, lines);

    return RepeatStatus.FINISHED;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    return ExitStatus.COMPLETED;
  }
}
