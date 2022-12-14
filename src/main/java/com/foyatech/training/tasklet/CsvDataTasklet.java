package com.foyatech.training.tasklet;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;

import com.foyatech.training.dao.TrainingDao;
import com.foyatech.training.model.Fy_tb_file_cntrl;
import com.foyatech.training.model.Fy_tb_file_info;
import com.foyatech.training.model.XmlByInputColumn;
import com.foyatech.training.model.XmlByInputType;
import com.foyatech.training.model.XmlByOutputColumn;
import com.foyatech.training.model.XmlByOutputType;
import com.foyatech.training.model.XmlByTableColumn;
import com.foyatech.training.model.XmlByTableName;
import com.foyatech.training.util.Utility;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CsvDataTasklet implements Tasklet {

	private String fileType;
	private String inputPath;
	private String inputExtension;
	private String outputPath;
	private String outputExtension;
	private String errPath;
	private String errExtension;
	private TrainingDao trainingDao;
	private File outputFile;
	private File errFile;
	private LocalDateTime minTransactionTime;
	private LocalDateTime maxTransactionTime;

	public CsvDataTasklet(String fileType, String inputPath, String inputExtension, String outputPath,
			String outputExtension, String errPath, String errExtension, TrainingDao trainingDao) {
		this.fileType = fileType;
		this.inputPath = inputPath;
		this.inputExtension = inputExtension;
		this.outputPath = outputPath;
		this.outputExtension = outputExtension;
		this.errPath = errPath;
		this.errExtension = errExtension;
		this.trainingDao = trainingDao;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		// ???????????? CSV ??????
		Map<String, String> transactionVo = new LinkedHashMap<>();
		
		// ??? DB ???
		Fy_tb_file_info fy_tb_file_info01 = new Fy_tb_file_info();
		Fy_tb_file_info fy_tb_file_info02 = new Fy_tb_file_info();
		Fy_tb_file_cntrl fy_tb_file_cntrlForUpdate = new Fy_tb_file_cntrl();
		List<Fy_tb_file_info> infoList = new ArrayList<>();
		Map<String, Fy_tb_file_info> infoTableMap = new LinkedHashMap<String, Fy_tb_file_info>();

		// ??????????????????
		List<Fy_tb_file_cntrl> resultList = trainingDao.findPendingData(fileType);

		// ???????????????????????????
		XmlByInputType xmlByInputType = (XmlByInputType) chunkContext.getStepContext().getJobExecutionContext().get("xmlByInputType");
		XmlByOutputType xmlOutputType = (XmlByOutputType) chunkContext.getStepContext().getJobExecutionContext().get("xmlByOutputType");
		XmlByTableName xmlByTableName = (XmlByTableName) chunkContext.getStepContext().getJobExecutionContext().get("xmlByTableName");
		
		String inputType = xmlByInputType.getInputType();
		String delimiter = xmlByInputType.getDelimiter();
		String outputType = xmlOutputType.getOutputType();
		String tableName = xmlByTableName.getTableName();
		
		List<XmlByInputColumn> xmlByInputColumns = xmlByInputType.getCloumn();
		List<XmlByOutputColumn> xmlByOutputColumns = xmlOutputType.getCloumn();
		List<XmlByTableColumn> xmlByTableColumns = xmlByTableName.getColumn();

		// ?????????????????? array ???
		String[] inputName = new String[xmlByInputColumns.size()];
		String[] inputQuoter = new String[xmlByInputColumns.size()];
		String[] inputNullable = new String[xmlByInputColumns.size()];
		
		String[] outputName = new String[xmlByOutputColumns.size()];
		String[] outputLength = new String[xmlByOutputColumns.size()];

		String[] tableColumnName = new String[xmlByTableColumns.size()];
		String[] tableColumnType = new String[xmlByTableColumns.size()];
		String[] tableColumnValue = new String[xmlByTableColumns.size()];
		int[] argsType = new int[xmlByTableColumns.size()];			// ????????? table type ????????? sql.Type
		
		// ????????????????????? input ???
		for (int i = 0; i < inputName.length; i++) {
			inputName[i] = xmlByInputColumns.get(i).getName();
			inputQuoter[i] = xmlByInputColumns.get(i).getQuoted();
			inputNullable[i] = xmlByInputColumns.get(i).getNullable();
		}

		// ??????????????? output ???
		for (int i = 0; i < outputName.length; i++) {
			outputName[i] = xmlByOutputColumns.get(i).getName();
			outputLength[i] = xmlByOutputColumns.get(i).getLength();
		}
		
		// ??????????????? table ?????????
		for (int i = 0; i < xmlByTableColumns.size(); i++) {
			tableColumnName[i] = xmlByTableColumns.get(i).getName();
			tableColumnType[i] = xmlByTableColumns.get(i).getType();
			tableColumnValue[i] = xmlByTableColumns.get(i).getValue();
			
			if(tableColumnType[i].equals("string")) {
				argsType[i] = java.sql.Types.VARCHAR;
			}else if(tableColumnType[i].equals("number")) {
				argsType[i] = java.sql.Types.DECIMAL;
			}else if(tableColumnType[i].equals("timestamp")) {
				argsType[i] = java.sql.Types.TIMESTAMP;
			}
		}
		
		// ??????????????? table ???????????????????????????????????? SQL ??????
		String sql = Utility.generateInsert(tableName, tableColumnName, tableColumnType);
		
		// ?????????????????????(??? { (??????) } )
		for (Fy_tb_file_cntrl fy_tb_file_cntrl : resultList) {
			BigDecimal fileSeq = fy_tb_file_cntrl.getFileSeq();
			String fileName = fy_tb_file_cntrl.getFileName();
			
			log.info("FILETYPE = " + fileType);
			log.info("FILE_SEQ = " + fileSeq);
			log.info("FILENAME = " + fileName);

			boolean checkProcess = true;
			int totalCount = 0; // ???????????????
			int errorCount = 0; // ?????????????????????
			double type01TotalAmount = 0; // ?????? TRANSACTION_TYPE 01 ?????????
			double type02TotalAmount = 0; // ?????? TRANSACTION_TYPE 02 ?????????
			int type01TotalCount = 0; // ?????? TRANSACTION_TYPE 01 ??????
			int type02TotalCount = 0; // ?????? TRANSACTION_TYPE 02 ??????

			try {
				// ??? status ?????? W
				@SuppressWarnings("unused")
				int checkResultCount = trainingDao.updateStatusToW(fileSeq);
			} catch (JdbcUpdateAffectedIncorrectNumberOfRowsException e) {
				log.error(ExceptionUtils.getStackTrace(e));
				trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
				continue; // ??????:????????????????????????
			}
			// ?????? type ????????????
			if (inputType.equals(fileType)) {

				File file = new File(inputPath + fileName + inputExtension);
				outputFile = new File(outputPath + fileName + outputExtension); // output file
				errFile = new File(errPath + fileName + errExtension); // error file

				// ?????????????????????
				if (!file.exists()) {
					log.error("#### ??????????????? ####");
					trainingDao.updateFileCntrlError(fileSeq, "???????????????");
					continue; // ??????:????????????????????????
				}

				List<String> proclines = FileUtils.readLines(file, "UTF-8");

				for (String line : proclines) {
					String[] fields = line.split(delimiter + "(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);

					totalCount++; // ????????? +1 ???

					// ????????????????????? > ?????????????????? exception
					try {
						// ???????????? csv ?????? map ??????????????? ???????????? : ????????????
						for (int i = 0; i < inputName.length; i++) {
							transactionVo.put(inputName[i], fields[i]);
						}
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
						trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
					}
					
					List<String> csvfileList = transactionVo.values().stream().collect(Collectors.toList()); // ???????????????????????? List

					// ??????????????? input start
					// ================================================================================================================
					for (int i = 0; i < inputName.length; i++) {

						// ?????????????????? true (?????????????????????)
						if ("true".equals(inputQuoter[i])) {

							char checkValue = '"';
							int valueLength = csvfileList.get(i).length() - 1;

							if (csvfileList.get(i).charAt(0) == checkValue && csvfileList.get(i).charAt(valueLength) == checkValue) {
								transactionVo.put("flag", "false"); // ????????????????????? false ?????????
							} else {
								transactionVo.put("flag", "true");
								log.error("?????? input ????????? : ??? " + totalCount + " ??? " + inputName[i] + " ???????????? " + (csvfileList.get(i).charAt(0) == checkValue && csvfileList.get(i).charAt(valueLength) == checkValue));
								break; // ??????:???????????????????????????
							}
						}

						// ????????????????????? false (????????????)
						if ("false".equals(inputNullable[i])) {
							if (csvfileList.get(i) == null || csvfileList.get(i).isEmpty()) {
								transactionVo.put("flag", "true");
								log.error("?????? input null ??? : ??? " + totalCount + " ??? " + inputName[i] + " ???????????? " + inputNullable[i]);
								break; // ??????:???????????????????????????
							} else {
								transactionVo.put("flag", "false"); // ????????????????????? false ?????????
							}
						}
					}
					// ================================================================================================================
					// ??????????????? input end

					// ??????????????? output start
					// ================================================================================================================
					// ?????? input ?????????????????????????????? output
					if ("false".equals(transactionVo.get("flag"))) { 
						int[] checkValueLength = new int[outputName.length];
						
						for (int i = 0; i < outputName.length; i++) {
							checkValueLength[i] = Integer.parseInt(outputLength[i]); // ???????????????????????????

							String getValue = csvfileList.get(i);

							if (getValue.getBytes().length > checkValueLength[i]) {

								transactionVo.put("flag", "true");
								log.error("?????? Error output ?????? : ??? " + totalCount + " ??? " + outputName[i] + " ???????????? " + getValue.getBytes().length + " > " + checkValueLength[i]);
								break; // ??????:???????????????????????????
							} else {
								transactionVo.put("flag", "false"); // ????????????????????? false ?????????
							}
						}
					}
					// ================================================================================================================
					// ??????????????? output end

					// ?????? CSV ?????????(??????)
					String transactionType = transactionVo.get("TRANSACTION_TYPE");
					LocalDateTime transactionTime = Utility.parseDateTime(transactionVo.get("TRANSACTION_TIME"));
					String address = transactionVo.get("ADDRESS");
					String memo = transactionVo.get("MEMO");
					BigDecimal price = new BigDecimal(transactionVo.get("PRICE"));
					BigDecimal unit = new BigDecimal(transactionVo.get("UNIT"));

					// ?????? transactionTime ???????????????
					if (maxTransactionTime == null) {
						maxTransactionTime = transactionTime;
					}
					if (minTransactionTime == null) {
						minTransactionTime = transactionTime;
					}

					if (maxTransactionTime.isBefore(transactionTime)) {
						maxTransactionTime = transactionTime;
					}
					if (minTransactionTime.isAfter(transactionTime)) {
						minTransactionTime = transactionTime;
					}

					// ??????????????????
					String checkflag = transactionVo.get("flag");

					// ????????????????????????????????????????????? DB
					if ("false".equals(checkflag)) {
						address = address.replace("\"", "");
						memo = memo.replace("\"", "");

						// ?????? amount ??????
						BigDecimal amount = price.multiply(unit);
						
						transactionVo.put("FILE_TYPE", fileType);
						transactionVo.put("FILE_SEQ", fileSeq.toString());
						transactionVo.put("FILENAME", fileName);
						transactionVo.put("AMOUNT", amount.toString());

						// ?????? transactionType ??????????????????????????????
						// FIXME ???????????????????????? ?
						if ("01".equals(transactionType)) {
							type01TotalAmount += amount.doubleValue();
							type01TotalCount++;

							fy_tb_file_info01.setFileSeq(fileSeq);
							fy_tb_file_info01.setFileType(fileType);
							fy_tb_file_info01.setFileName(fileName);
							fy_tb_file_info01.setAmount(new BigDecimal(type01TotalAmount).setScale(5, BigDecimal.ROUND_HALF_UP));
							fy_tb_file_info01.setCount(new BigDecimal(type01TotalCount));
							fy_tb_file_info01.setTransactionType(transactionType);

							infoTableMap.put("Transaction01", fy_tb_file_info01);

						} else if ("02".equals(transactionType)) {
							type02TotalAmount += amount.doubleValue();
							type02TotalCount++;

							fy_tb_file_info02.setFileSeq(fileSeq);
							fy_tb_file_info02.setFileType(fileType);
							fy_tb_file_info02.setFileName(fileName);
							fy_tb_file_info02.setAmount(new BigDecimal(type02TotalAmount).setScale(5, BigDecimal.ROUND_HALF_UP));
							fy_tb_file_info02.setCount(new BigDecimal(type02TotalCount));
							fy_tb_file_info02.setTransactionType(transactionType);

							infoTableMap.put("Transaction02", fy_tb_file_info02);
						}

						// ??? transactionVo (Map) ?????? SQL ???????????? Object[]
						Object[] args = new Object[tableColumnName.length];
						
						for (int i = 0; i < args.length; i++) {
							args[i] = transactionVo.get(tableColumnName[i]);
							
							if(tableColumnType[i].equals("timestamp") && args[i] != null) {
								args[i] = Utility.parseDateTime(args[i].toString());
							}
						}
						
						try {
							trainingDao.insertTransaction(tableName, args, argsType, sql);
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));

							// Rollback ????????????????????????
							if (outputFile.exists()) {
								FileUtils.deleteQuietly(outputFile);
								log.info(outputFile + "?????????");
							}

							if (errFile.exists()) {
								FileUtils.deleteQuietly(errFile);
								log.info(errFile + "?????????");
							}

							trainingDao.rollbackTransaction(fileSeq);
							trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
							checkProcess = false;
							break; // ??????????????????
						}

						// ??????????????? output length ????????????????????????????????????
						String[] outputData = new String[outputName.length];
						
						String results = "";
						
						for (int i = 0; i < outputData.length; i++) {
							outputData[i] = Utility.fillSpaceRight(transactionVo.get(outputName[i]).replace("\"", ""), Integer.parseInt(outputLength[i]));
							results += outputData[i];
						}
						
						FileUtils.writeStringToFile(outputFile, results + "\n", "UTF-8", true);

					} else if ("true".equals(checkflag)) {
						// ????????????
						FileUtils.writeStringToFile(errFile, line + "\n", "UTF-8", true);
						errorCount++;
					}

					transactionVo.clear();

				} // ???????????? CSV ??????????????????

			} else {
				log.error(fileType + " ?????????????????? type???");
				trainingDao.updateFileCntrlError(fileSeq, "Type ?????????");
				continue; // ??????????????????
			}
			// ???????????????????????????????????????????????????
			if(checkProcess) {
				try {
	
					infoList = infoTableMap.values().stream().collect(Collectors.toList());
	
					// ?????? FY_TB_FILE_INFO
					trainingDao.insertFileInfo(infoList);
					// ??? OutputFile ???????????? FY_TB_FILE_CNTRL
					trainingDao.insertFileCntrlByOutputType(outputType, fileName);
	
					// FY_TB_FILE_CNTRL ????????????????????? S
					double totalAmount = type01TotalAmount + type02TotalAmount;
	
					fy_tb_file_cntrlForUpdate.setFileSeq(fileSeq);
					fy_tb_file_cntrlForUpdate.setFileType(fileType);
					fy_tb_file_cntrlForUpdate.setFileName(fileName);
					fy_tb_file_cntrlForUpdate.setEndTime(LocalDateTime.now());
					fy_tb_file_cntrlForUpdate.setErrorCount(new BigDecimal(errorCount));
					fy_tb_file_cntrlForUpdate.setMinTransactionTime(minTransactionTime);
					fy_tb_file_cntrlForUpdate.setMaxTransactionTime(maxTransactionTime);
					fy_tb_file_cntrlForUpdate.setStatus("S");
					fy_tb_file_cntrlForUpdate.setTotalAmount(new BigDecimal(totalAmount).setScale(5, BigDecimal.ROUND_HALF_UP));
					fy_tb_file_cntrlForUpdate.setTotalCount(new BigDecimal(totalCount));
	
					trainingDao.updateFileCntrl(fy_tb_file_cntrlForUpdate);
	
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
	
					// Rollback ????????????????????????
					if (outputFile.exists()) {
						FileUtils.deleteQuietly(outputFile);
						log.info(outputFile + "?????????");
					}
	
					if (errFile.exists()) {
						FileUtils.deleteQuietly(errFile);
						log.info(errFile + "?????????");
					}
	
					trainingDao.rollbackTransaction(fileSeq);
					trainingDao.rollbackFileInfo(fileSeq);
					trainingDao.rollbackFileCntrlByOutputType(fileName, outputType);
					trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
					continue; // ??????????????????
				}
			}
		} // ????????????

		return RepeatStatus.FINISHED;

	}

}
