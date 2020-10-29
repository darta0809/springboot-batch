package com.foyatech.training.tasklet;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.foyatech.training.model.Fy_tb_transaction;
import com.foyatech.training.model.XmlByInputColumn;
import com.foyatech.training.model.XmlByInputType;
import com.foyatech.training.model.XmlByOutputColumn;
import com.foyatech.training.model.XmlByOutputType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FixDataTasklet implements Tasklet{
	
	private String fileType;
	private String inputPath;
	private String inputExtension;
	private String outputPath;
	private String outputExtension;
	private String errPath;
	private String errExtension;
	private TrainingDao trainingDao;
	
	public FixDataTasklet(String fileType, String inputPath, String inputExtension, String outputPath, String outputExtension, String errPath, String errExtension, TrainingDao trainingDao) {
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
		Map<String,String> transactionVo = new LinkedHashMap<>();
		Fy_tb_transaction fy_tb_transaction = new Fy_tb_transaction();
		
		// 取待處理資料
		List<Fy_tb_file_cntrl> resultList = trainingDao.findPendingData(fileType);
		
		// 取得檢核內容
		XmlByInputType xmlByInputType = (XmlByInputType) chunkContext.getStepContext().getJobExecutionContext().get("xmlByInputType");
		XmlByOutputType xmlOutputType = (XmlByOutputType) chunkContext.getStepContext().getJobExecutionContext().get("xmlByOutputType");
		String inputType = xmlByInputType.getInputType();
		String delimiter = xmlOutputType.getDelimiter();
		String outputType = xmlOutputType.getOutputType();
		List<XmlByInputColumn> xmlByInputColumn = xmlByInputType.getCloumn();
		List<XmlByOutputColumn> xmlByOutputColumn = xmlOutputType.getCloumn();
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		
		String[] key = new String[xmlByInputColumn.size()];	// 欄位名稱
		String[] inputNullable = new String[xmlByInputColumn.size()];
		String[] inputLength = new String[xmlByInputColumn.size()];
		String[] outputQuoter = new String[xmlByOutputColumn.size()];
				
		// 取出所有 input 、output 檢核值
		for (int i = 0; i < key.length; i++) {
			key[i] = xmlByInputColumn.get(i).getName(); 
			inputLength[i] = xmlByInputColumn.get(i).getLength();
			inputNullable[i] = xmlByInputColumn.get(i).getNullable();
			outputQuoter[i] = xmlByOutputColumn.get(i).getQuoted();
		}

		// 取得單筆待處理資料資訊
		for (Fy_tb_file_cntrl fy_tb_file_cntrl : resultList) {
			BigDecimal fileSeq = fy_tb_file_cntrl.getFileSeq();
			String fileName = fy_tb_file_cntrl.getFileName();
			
			log.info("FILE_SEQ = " + fileSeq);
			log.info("FILENAME = " + fileName);
			
			// 寫入失敗檔的筆數
			int errorCount = 0;
			
			try {
				// 將 status 改為 W
				@SuppressWarnings("unused")
				int checkResultCount = trainingDao.updateStatusToW(fileSeq);
			} catch(JdbcUpdateAffectedIncorrectNumberOfRowsException e) {
				log.error(ExceptionUtils.getStackTrace(e));
				trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
				continue; // 跳下一筆檔案
			}
			// 檢查 type 是否一致
			if (inputType.equals(fileType)) {
				File file = new File(inputPath + fileName + inputExtension);
				
				// 檔案不存在檢核
				if (!file.exists()) {
					log.error("#### 找不到檔案 ####");
					trainingDao.updateFileCntrlError(fileSeq, "找不到檔案");
					continue; // 跳下一筆檔案
				}
				
				List<String> proclines = FileUtils.readLines(file, "UTF-8");

				for(String line : proclines) {
					String[] fields = line.split("\t");
					
					// 將讀取的 FIX 塞入 map 裡
					for (int i = 0; i < fields.length; i++) {
						transactionVo.put(key[i], fields[i]);
					}
					
					List<String> fixFileList = transactionVo.values().stream().collect(Collectors.toList());
					
					// 檢核 input 描述檔 start
					// ================================================================================================================
					
					for (int i = 0; i < xmlByInputColumn.size(); i++) {
						int[] checkValueLength = new int[inputLength.length];
						checkValueLength[i] = Integer.parseInt(inputLength[i]); // 檢核條件的長度限制
						
						String getValue = fixFileList.get(i);
						
						if(getValue.getBytes().length > checkValueLength[i]) {
							transactionVo.put("flag","true");
							log.warn("檢核 input 長度 : " + getValue.getBytes().length + " > " + checkValueLength[i]);
							break;
						}else {
							transactionVo.put("flag","false"); // 是否寫入錯誤檔 false 不寫入
						}
						
						// 檢核條件若是為 false (不能為空)
						if("false".equals(inputNullable[i])) {
							if(fixFileList.get(i) == null || fixFileList.get(i).isEmpty()) {
								transactionVo.put("flag","true");
								log.warn("檢視 output null 值 : " + inputNullable[i]);
								break;
							}else {
								transactionVo.put("flag","false"); // 是否寫入錯誤檔 false 不寫入
							}
						}
					}
					
					// ================================================================================================================
					// 檢核 input 描述檔 end
					
					// 檢核 output 描述檔 start
					// ================================================================================================================
					for (int i = 0; i < fixFileList.size(); i++) {
						// 檢核條件若是 true (需要雙引號包住)
						if("true".equals(outputQuoter[i])) {
							transactionVo.put(key[i], "\"" + fields[i] + "\"");
						}
					}
					// ================================================================================================================
					// 檢核 output 描述檔 end
					
					// 取出 FIX 所有值(單行)
					String transactionType = transactionVo.get("TRANSACTION_TYPE").toString();
					BigDecimal transactionSeq = new BigDecimal(transactionVo.get("TRANSACTION_SEQ").toString());
					LocalDateTime transactionTime = LocalDateTime.parse(transactionVo.get("TRANSACTION_TIME").toString(), formatter);
					String transactionTimeFormat = transactionTime.format(formatter);
					String custId = transactionVo.get("CUST_ID").toString();
					String storeId = transactionVo.get("STORE_ID").toString();
					String productId = transactionVo.get("PRODUCT_ID").toString();
					String phone = transactionVo.get("PHONE").toString();
					String address = transactionVo.get("ADDRESS").toString();
					String memo = transactionVo.get("MEMO").toString();
					BigDecimal price = new BigDecimal(transactionVo.get("PRICE").toString());
					BigDecimal unit = new BigDecimal(transactionVo.get("UNIT").toString());
					
					// 取得檢核結果
					String checkflag = transactionVo.get("flag").toString();
					
					// 產出正確檔就把引號去掉
					if("false".equals(checkflag)) {
						// 計算 amount 的值，存入 map
						BigDecimal amount = price.multiply(unit);
						transactionVo.put("AMOUNT", amount.toString());
						
						fy_tb_transaction.setFileSeq(fileSeq);
						fy_tb_transaction.setFileType(fileType);
						fy_tb_transaction.setFileName(fileName);
						fy_tb_transaction.setTransactionType(transactionType);
						fy_tb_transaction.setTransactionSeq(transactionSeq);
						fy_tb_transaction.setTransactionTime(transactionTime);
						fy_tb_transaction.setCustId(custId);
						fy_tb_transaction.setStoreId(storeId);
						fy_tb_transaction.setProductId(productId);
						fy_tb_transaction.setPhone(phone);
						fy_tb_transaction.setAddress(address);
						fy_tb_transaction.setMemo(memo);
						fy_tb_transaction.setPrice(price);
						fy_tb_transaction.setUnit(unit);
						fy_tb_transaction.setAmount(amount);

						try {
							trainingDao.insertTransaction(fy_tb_transaction);
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
							trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
							trainingDao.rollbackTransaction(fileSeq);
							break; // 跳下一筆檔案
						}
						
						// 產正確檔
						File outputFile = new File(outputPath + fileName + outputExtension);
						String results = transactionType + delimiter 
										+ transactionSeq + delimiter
										+ transactionTimeFormat + delimiter  
										+ custId + delimiter  
										+ storeId + delimiter  
										+ productId + delimiter  
										+ phone + delimiter  
										+ address + delimiter
										+ memo + delimiter
										+ price + delimiter 
										+ unit + "\n";
						FileUtils.writeStringToFile(outputFile, results, "UTF-8", true);
						
					}else if("true".equals(checkflag)) {
						// 產錯誤檔
						File errFile = new File(errPath + fileName + errExtension);
						FileUtils.writeStringToFile(errFile, line+"\n", "UTF-8", true);
						errorCount++;
					}
					
					transactionVo.clear();
				}
				
			}else {
				trainingDao.updateFileCntrlError(fileSeq, "Type 不一致");
				continue; // 跳下一筆檔案
			}
			
			try {
				// 寫入 FY_TB_FILE_INFO
				trainingDao.insertFileInfo(fileType, fileName);
				// 將 OutputFile 資料寫入 FY_TB_FILE_CNTRL
				trainingDao.insertFileCntrlByOutputType(outputType, fileName);
				// FY_TB_FILE_CNTRL 更新單檔資料為 S
				trainingDao.updateFileCntrl(fileName, fileSeq, errorCount);
			}catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
				trainingDao.rollbackTransaction(fileSeq);
				trainingDao.rollbackFileInfo(fileSeq);
				trainingDao.rollbackFileCntrlByOutputType(fileName, outputType);
				trainingDao.updateFileCntrlError(fileSeq, e.getMessage());
				continue; // 跳下一筆檔案
			} 
			
		}

		return RepeatStatus.FINISHED;
	}

}
