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

		// 裝讀取的 CSV 資料
		Map<String, String> transactionVo = new LinkedHashMap<>();
		
		// 給 DB 用
		Fy_tb_file_info fy_tb_file_info01 = new Fy_tb_file_info();
		Fy_tb_file_info fy_tb_file_info02 = new Fy_tb_file_info();
		Fy_tb_file_cntrl fy_tb_file_cntrlForUpdate = new Fy_tb_file_cntrl();
		List<Fy_tb_file_info> infoList = new ArrayList<>();
		Map<String, Fy_tb_file_info> infoTableMap = new LinkedHashMap<String, Fy_tb_file_info>();

		// 取待處理資料
		List<Fy_tb_file_cntrl> resultList = trainingDao.findPendingData(fileType);

		// 取得設定檔檢核內容
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

		// 將檢核內容用 array 裝
		String[] inputName = new String[xmlByInputColumns.size()];
		String[] inputQuoter = new String[xmlByInputColumns.size()];
		String[] inputNullable = new String[xmlByInputColumns.size()];
		
		String[] outputName = new String[xmlByOutputColumns.size()];
		String[] outputLength = new String[xmlByOutputColumns.size()];

		String[] tableColumnName = new String[xmlByTableColumns.size()];
		String[] tableColumnType = new String[xmlByTableColumns.size()];
		String[] tableColumnValue = new String[xmlByTableColumns.size()];
		int[] argsType = new int[xmlByTableColumns.size()];			// 設定檔 table type 值轉成 sql.Type
		
		// 取出設定檔所有 input 值
		for (int i = 0; i < inputName.length; i++) {
			inputName[i] = xmlByInputColumns.get(i).getName();
			inputQuoter[i] = xmlByInputColumns.get(i).getQuoted();
			inputNullable[i] = xmlByInputColumns.get(i).getNullable();
		}

		// 取出設定檔 output 值
		for (int i = 0; i < outputName.length; i++) {
			outputName[i] = xmlByOutputColumns.get(i).getName();
			outputLength[i] = xmlByOutputColumns.get(i).getLength();
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
		
		// 待處理資料資訊(多 { (單筆) } )
		for (Fy_tb_file_cntrl fy_tb_file_cntrl : resultList) {
			BigDecimal fileSeq = fy_tb_file_cntrl.getFileSeq();
			String fileName = fy_tb_file_cntrl.getFileName();
			
			log.info("FILETYPE = " + fileType);
			log.info("FILE_SEQ = " + fileSeq);
			log.info("FILENAME = " + fileName);

			boolean checkProcess = true;
			int totalCount = 0; // 來源檔筆數
			int errorCount = 0; // 寫入失敗檔筆數
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
				continue; // 邏輯:有錯時就跳下一筆
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
					continue; // 邏輯:有錯時就跳下一筆
				}

				List<String> proclines = FileUtils.readLines(file, "UTF-8");

				for (String line : proclines) {
					String[] fields = line.split(delimiter + "(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);

					totalCount++; // 讀一行 +1 筆

					// 設定檔欄位數量 > 來源檔時會出 exception
					try {
						// 將讀取的 csv 塞入 map 裡，設定檔 欄位名稱 : 讀取資料
						for (int i = 0; i < inputName.length; i++) {
							transactionVo.put(inputName[i], fields[i]);
						}
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
						trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
					}
					
					List<String> csvfileList = transactionVo.values().stream().collect(Collectors.toList()); // 將讀到的資料轉為 List

					// 檢核設定檔 input start
					// ================================================================================================================
					for (int i = 0; i < inputName.length; i++) {

						// 檢核條件若是 true (需要雙引號包住)
						if ("true".equals(inputQuoter[i])) {

							char checkValue = '"';
							int valueLength = csvfileList.get(i).length() - 1;

							if (csvfileList.get(i).charAt(0) == checkValue && csvfileList.get(i).charAt(valueLength) == checkValue) {
								transactionVo.put("flag", "false"); // 是否寫入錯誤檔 false 不寫入
							} else {
								transactionVo.put("flag", "true");
								log.error("檢核 input 雙引號 : 第 " + totalCount + " 筆 " + inputName[i] + " 欄位錯誤 " + (csvfileList.get(i).charAt(0) == checkValue && csvfileList.get(i).charAt(valueLength) == checkValue));
								break; // 邏輯:有錯誤時就跳下一筆
							}
						}

						// 檢核條件若是為 false (不能為空)
						if ("false".equals(inputNullable[i])) {
							if (csvfileList.get(i) == null || csvfileList.get(i).isEmpty()) {
								transactionVo.put("flag", "true");
								log.error("檢核 input null 值 : 第 " + totalCount + " 筆 " + inputName[i] + " 欄位錯誤 " + inputNullable[i]);
								break; // 邏輯:有錯誤時就跳下一筆
							} else {
								transactionVo.put("flag", "false"); // 是否寫入錯誤檔 false 不寫入
							}
						}

					}
					// ================================================================================================================
					// 檢核設定檔 input end

					// 檢核設定檔 output start
					// ================================================================================================================
					// 如果 input 檢核都符合時，才檢核 output
					if ("false".equals(transactionVo.get("flag"))) { 
						int[] checkValueLength = new int[outputName.length];
						
						for (int i = 0; i < outputName.length; i++) {
							checkValueLength[i] = Integer.parseInt(outputLength[i]); // 檢核條件的長度限制

							String getValue = csvfileList.get(i);

							if (getValue.getBytes().length > checkValueLength[i]) {

								transactionVo.put("flag", "true");
								log.error("檢核 Error output 長度 : 第 " + totalCount + " 筆 " + outputName[i] + " 欄位錯誤 " + getValue.getBytes().length + " > " + checkValueLength[i]);
								break; // 邏輯:有錯誤時就跳下一筆
							} else {
								transactionVo.put("flag", "false"); // 是否寫入錯誤檔 false 不寫入
							}
						}
					}
					// ================================================================================================================
					// 檢核設定檔 output end

					// 取出 CSV 所需值(單行)
					String transactionType = transactionVo.get("TRANSACTION_TYPE");
					LocalDateTime transactionTime = Utility.parseDateTime(transactionVo.get("TRANSACTION_TIME"));
					String address = transactionVo.get("ADDRESS");
					String memo = transactionVo.get("MEMO");
					BigDecimal price = new BigDecimal(transactionVo.get("PRICE"));
					BigDecimal unit = new BigDecimal(transactionVo.get("UNIT"));

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

					// 產出正確檔就把引號去掉、並寫入 DB
					if ("false".equals(checkflag)) {
						address = address.replace("\"", "");
						memo = memo.replace("\"", "");

						// 計算 amount 的值
						BigDecimal amount = price.multiply(unit);
						
						transactionVo.put("FILE_TYPE", fileType);
						transactionVo.put("FILE_SEQ", fileSeq.toString());
						transactionVo.put("FILENAME", fileName);
						transactionVo.put("AMOUNT", amount.toString());

						// 根據 transactionType 分組，計算金額及筆數
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

						// 依照設定檔 output length 長度，組合正確檔產出字串
						String[] outputData = new String[outputName.length];
						
						String results = "";
						
						for (int i = 0; i < outputData.length; i++) {
							outputData[i] = Utility.fillSpaceRight(transactionVo.get(outputName[i]).replace("\"", ""), Integer.parseInt(outputLength[i]));
							results += outputData[i];
						}
						
						FileUtils.writeStringToFile(outputFile, results + "\n", "UTF-8", true);

					} else if ("true".equals(checkflag)) {
						// 產錯誤檔
						FileUtils.writeStringToFile(errFile, line + "\n", "UTF-8", true);
						errorCount++;
					}

					transactionVo.clear();

				} // 讀取單筆 CSV 所有資料結束

			} else {
				log.error(fileType + " 找不到對應的 type！");
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
		} // 多筆結束

		return RepeatStatus.FINISHED;

	}

}
