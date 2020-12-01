package com.foyatech.training.tasklet;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class FixDataTasklet implements Tasklet {

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

	public FixDataTasklet(String fileType, String inputPath, String inputExtension, String outputPath,
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

		// 裝讀取的 FIX 資料
		Map<String, String> transactionVo = new LinkedHashMap<>();
		// 將切割完的資料組成 list 
		List<String> subStringLine = new ArrayList<>();

		// 給 DB 用
		Fy_tb_file_info fy_tb_file_info01 = new Fy_tb_file_info();
		Fy_tb_file_info fy_tb_file_info02 = new Fy_tb_file_info();
		Fy_tb_file_cntrl fy_tb_file_cntrlForUpdate = new Fy_tb_file_cntrl();
		List<Fy_tb_file_info> infoList = new ArrayList<>();
		Map<String, Fy_tb_file_info> infoTableMap = new LinkedHashMap<String, Fy_tb_file_info>();

		// 取待處理資料
		List<Fy_tb_file_cntrl> resultList = trainingDao.findPendingData(fileType);

		// 取得設定檔內容
		XmlByInputType xmlByInputType = (XmlByInputType) chunkContext.getStepContext().getJobExecutionContext().get("xmlByInputType");
		XmlByOutputType xmlByOutputType = (XmlByOutputType) chunkContext.getStepContext().getJobExecutionContext().get("xmlByOutputType");
		XmlByTableName xmlByTableName = (XmlByTableName) chunkContext.getStepContext().getJobExecutionContext().get("xmlByTableName");
		
		String inputType = xmlByInputType.getInputType();
		String outputType = xmlByOutputType.getOutputType();
		String delimiter = xmlByOutputType.getDelimiter();
		String tableName = xmlByTableName.getTableName();
		
		List<XmlByInputColumn> xmlByInputColumns = xmlByInputType.getCloumn();
		List<XmlByOutputColumn> xmlByOutputColumns = xmlByOutputType.getCloumn();
		List<XmlByTableColumn> xmlByTableColumns = xmlByTableName.getColumn();
		
		// 將檢核內容用 array 裝
		String[] inputName = new String[xmlByInputColumns.size()];
		String[] inputNullable = new String[xmlByInputColumns.size()];
		String[] inputLength = new String[xmlByInputColumns.size()];
		int[] inputLengthInt = new int[xmlByInputColumns.size()];	// 讀檔時，需要切割資料的長度、檢核資料長度 String => int
		
		String[] outputQuoter = new String[xmlByOutputColumns.size()];
		String[] outputName = new String[xmlByOutputColumns.size()];
		
		String[] tableColumnName = new String[xmlByTableColumns.size()];
		String[] tableColumnType = new String[xmlByTableColumns.size()];
		String[] tableColumnValue = new String[xmlByTableColumns.size()];
		int[] argsType = new int[xmlByTableColumns.size()];			// 設定檔 table type 值轉成 sql.Type
		
		// 取出設定檔 input 檢核值
		for (int i = 0; i < xmlByInputColumns.size(); i++) {
			inputName[i] = xmlByInputColumns.get(i).getName();
			inputLength[i] = xmlByInputColumns.get(i).getLength();
			inputNullable[i] = xmlByInputColumns.get(i).getNullable();
			inputLengthInt[i] = Integer.parseInt(inputLength[i]);
		}

		// 取出設定檔 output 檢核值
		for (int i = 0; i < xmlByOutputColumns.size(); i++) {
			outputName[i] = xmlByOutputColumns.get(i).getName();
			outputQuoter[i] = xmlByOutputColumns.get(i).getQuoted();
		}

		// 取出設定檔 table 相關值
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
		
		// 根據設定檔 table 欄位名稱、欄位數量，組成 SQL 語法
		String sql = Utility.generateInsert(tableName, tableColumnName, tableColumnType);
		
		// 取得待處理資料資訊(多筆)
		for (Fy_tb_file_cntrl fy_tb_file_cntrl : resultList) {
			BigDecimal fileSeq = fy_tb_file_cntrl.getFileSeq();
			String fileName = fy_tb_file_cntrl.getFileName();
			
			log.info("FILE_TYPE = " + fileType);
			log.info("FILE_SEQ = " + fileSeq);
			log.info("FILENAME = " + fileName);

			boolean checkProcess = true;
			int totalCount = 0; // FIX 資料檔的筆數
			int errorCount = 0; // 寫入失敗檔的筆數
			double type01TotalAmount = 0; // 計算 TRANSACTION_TYPE 01 總金額
			double type02TotalAmount = 0; // 計算 TRANSACTION_TYPE 02 總金額
			int type01TotalCount = 0; // 計算 TRANSACTION_TYPE 01 次數
			int type02TotalCount = 0; // 計算 TRANSACTION_TYPE 02 次數

			try {
				// 將 status 改為 W
				@SuppressWarnings("unused")
				int checkResultCount = trainingDao.updateStatusToW(fileSeq);
			} catch (JdbcUpdateAffectedIncorrectNumberOfRowsException e) {
				log.error(ExceptionUtils.getStackTrace(e));
				trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
				continue; // 跳下一筆檔案
			}
			// 檢查 type 是否一致
			if (inputType.equals(fileType)) {

				File file = new File(inputPath + fileName + inputExtension);
				outputFile = new File(outputPath + fileName + outputExtension); // output file
				errFile = new File(errPath + fileName + errExtension); // error file

				// 檔案不存在檢核
				if (!file.exists()) {
					log.error("#### 找不到檔案 ####");
					trainingDao.updateFileCntrlError(fileSeq, "找不到檔案");
					continue; // 跳下一筆檔案
				}

				List<String> proclines = FileUtils.readLines(file, "UTF-8");

				for (String line : proclines) {
					
					int start = 0;
					int end = 0;
					// 根據設定檔長度切割資料塞到 list
					for (int j = 0; j < inputLengthInt.length; j++) {
						end += inputLengthInt[j];
						subStringLine.add(line.substring(start, end));
						start = end;
					}
					
					String[] fields = new String[inputLengthInt.length];
					
					for (int i = 0; i < subStringLine.size(); i++) {
						fields[i] = subStringLine.get(i).trim();
					}

					totalCount++; // 讀一行 +1 筆

					// 設定檔欄位數量與來源檔數量不符時容易出 exception
					try {
						// 將讀取的 csv 塞入 map 裡，設定檔 欄位名稱 : 讀取資料
						for (int i = 0; i < xmlByInputColumns.size(); i++) {
							transactionVo.put(inputName[i], fields[i]);
						}
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
						trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
					}

					List<String> fixFileList = transactionVo.values().stream().collect(Collectors.toList()); // 將讀到的資料轉為
																												// List

					// 檢核設定檔 input start
					// ================================================================================================================

					for (int i = 0; i < xmlByInputColumns.size(); i++) {

						String getValue = fixFileList.get(i);

						if (getValue.getBytes().length > inputLengthInt[i]) {
							transactionVo.put("flag", "true");
							log.error("檢核 input 長度 : 第 " + totalCount + " 筆 " + inputName[i] + " 欄位錯誤 " + getValue.getBytes().length + " > " + inputLengthInt[i]);
							break; // 邏輯:有錯誤時就跳下一筆
						} else {
							transactionVo.put("flag", "false"); // 是否寫入錯誤檔 false 不寫入
						}

						// 檢核條件若是為 false (不能為空)
						if ("false".equals(inputNullable[i])) {
							if (fixFileList.get(i) == null || fixFileList.get(i).isEmpty()) {
								transactionVo.put("flag", "true");
								log.error("檢視 output null 值 : 第 " + totalCount + " 筆 " + inputName[i] + " 欄位錯誤 " + inputNullable[i]);
								break; // 邏輯:有錯誤時就跳下一筆
							} else {
								transactionVo.put("flag", "false"); // 是否寫入錯誤檔 false 不寫入
							}
						}
					}
					// ================================================================================================================
					// 檢核設定檔 input end

					// 取出 FIX 所有值(單行)
					String transactionType = transactionVo.get("TRANSACTION_TYPE");
					LocalDateTime transactionTime = Utility.parseDateTime(transactionVo.get("TRANSACTION_TIME"));
					BigDecimal price = new BigDecimal(transactionVo.get("PRICE"));
					BigDecimal unit = new BigDecimal(transactionVo.get("UNIT"));

					Set<String> te = new HashSet<>();
					te.add(transactionType);
					
					// 取出 transactionTime 最大、最小
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

					// 取得檢核結果
					String checkflag = transactionVo.get("flag");

					// 產出正確檔並寫入 DB
					if ("false".equals(checkflag)) {

						// 計算 amount 的值
						BigDecimal amount = price.multiply(unit);

						transactionVo.put("FILE_TYPE", fileType);
						transactionVo.put("FILE_SEQ", fileSeq.toString());
						transactionVo.put("FILENAME", fileName);
						transactionVo.put("AMOUNT", amount.toString());

						// 根據 transactionType 分組，個別計算金額及筆數
						// FIXME 是否可用動態方式 ?
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
						
						// 把 transactionVo (Map) 組成 SQL 需要的值 Object[]
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

							// Rollback 時，連同檔案刪除
							if (outputFile.exists()) {
								FileUtils.deleteQuietly(outputFile);
								log.info(outputFile + "已刪除");
							}

							if (errFile.exists()) {
								FileUtils.deleteQuietly(errFile);
								log.info(errFile + "已刪除");
							}

							trainingDao.rollbackTransaction(fileSeq);
							trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
							checkProcess = false;
							break; // 跳下一筆檔案
						}

						// 檢核設定檔 output start
						// ================================================================================================================
						for (int i = 0; i < xmlByOutputColumns.size(); i++) {
							// 檢核條件若是 true (需要雙引號包住)
							if ("true".equals(outputQuoter[i])) {
								transactionVo.put(outputName[i], "\"" + fields[i] + "\"");
							}
						}
						// ================================================================================================================
						// 檢核設定檔 output end
						
						String[] outputData = new String[outputName.length];

						String results = "";

						for (int i = 0; i < outputName.length; i++) {
							outputData[i] = transactionVo.get(outputName[i]);
						}

						results = Arrays.stream(outputData).collect(Collectors.joining(delimiter));
						
						FileUtils.writeStringToFile(outputFile, results + "\n", "UTF-8", true);

					} else if ("true".equals(checkflag)) {
						// 產錯誤檔
						FileUtils.writeStringToFile(errFile, line + "\n", "UTF-8", true);
						errorCount++;
					}

					transactionVo.clear();
					subStringLine.clear();
					
				} // 讀取單筆 FIX 所有資料結束

			} else {
				trainingDao.updateFileCntrlError(fileSeq, "Type 不一致");
				continue; // 跳下一筆檔案
			}
			// 檢查單筆有無發生錯誤，無錯誤才執行
			if(checkProcess) {
				try {
	
					infoList = infoTableMap.values().stream().collect(Collectors.toList());
	
					// 寫入 FY_TB_FILE_INFO
					trainingDao.insertFileInfo(infoList);
					// 將 OutputFile 資料寫入 FY_TB_FILE_CNTRL
					trainingDao.insertFileCntrlByOutputType(outputType, fileName);
	
					// FY_TB_FILE_CNTRL 更新單檔資料為 S
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
	
					// Rollback 時，連同檔案刪除
					if (outputFile.exists()) {
						FileUtils.deleteQuietly(outputFile);
						log.info(outputFile + "已刪除");
					}
	
					if (errFile.exists()) {
						FileUtils.deleteQuietly(errFile);
						log.info(errFile + "已刪除");
					}
	
					trainingDao.rollbackTransaction(fileSeq);
					trainingDao.rollbackFileInfo(fileSeq);
					trainingDao.rollbackFileCntrlByOutputType(fileName, outputType);
					trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
					continue; // 跳下一筆檔案
				}
	
			}
		}
		return RepeatStatus.FINISHED;
	}

}
