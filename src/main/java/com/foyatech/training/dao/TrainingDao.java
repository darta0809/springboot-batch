package com.foyatech.training.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.foyatech.training.model.Fy_tb_file_cntrl;
import com.foyatech.training.model.Fy_tb_file_info;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class TrainingDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/**
	 * 找出待處理資料
	 * */
	public List<Fy_tb_file_cntrl> findPendingData(String fileType) {
		
		log.info(">>>>> find FY_TB_FILE_CNTRL <<<<<");
		try {
			List<Fy_tb_file_cntrl> resultList = jdbcTemplate.query(" SELECT * " 
																	+ " FROM FY_TB_FILE_CNTRL " 
																	+ " WHERE FILE_TYPE = '" + fileType 
																	+ "' AND STATUS = 'I' ", BeanPropertyRowMapper.newInstance(Fy_tb_file_cntrl.class));
			
			log.info("Load " + resultList.size() + " recood(s) FROM FY_TB_FILE_CNTRL");
			
			return resultList;
			
		}catch (DataAccessException e) {
			log.error("Load FY_TB_FILE_CNTRL error. Exception: " + e);
			throw e;
		}
	}
	
	/**
	 * 將狀態改為 W
	 * */
	public int updateStatusToW(BigDecimal fileSeq) {
		
		log.info(">>>>> Update FY_TB_FILE_CNTRL.STATUS <<<<<");
		
		final int expected = 1;
		int actual = 0;
		
		String sql = "UPDATE FY_TB_FILE_CNTRL " 
					+ "SET STATUS = ? , START_TIME = ? " 
					+ "WHERE FILE_SEQ = ? "
					+ "AND STATUS = 'I' " ;
		
		actual = jdbcTemplate.update(sql, "W", LocalDateTime.now(), fileSeq);
		
		if (actual != expected) {
			log.error("FY_TB_FILE_CNTRL updated rows count should be " + expected + ". actual=" + actual);
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);
		}
		return actual;
	}
	
	/**
	 * 新增 Input 讀到的資料，寫入 FY_TB_TRANSACTION
	 * */
	public void insertTransaction(String tableName, Object[] args, int[] argsType, String sql) {
		
		log.info(">>>>> Insert " + tableName + "<<<<<");
		
		jdbcTemplate.update(sql, args, argsType);
	}
	
	/**
	 * 根據 FY_TB_TRANSACTION 的資料，新增到 FY_TB_FILE_INFO
	 * */
	public void insertFileInfo(List<Fy_tb_file_info> infoList) {
		
		log.info(">>>>> Insert FY_TB_FILE_INFO <<<<<");
		
		String sql = "INSERT INTO FY_TB_FILE_INFO (FILE_SEQ, FILE_TYPE, FILENAME, TRANSACTION_TYPE, COUNT, AMOUNT) "
					+ "VALUES (?,?,?,?,?,?)";
		
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setBigDecimal(1, infoList.get(i).getFileSeq());
				ps.setString(2, infoList.get(i).getFileType());
				ps.setString(3, infoList.get(i).getFileName());
				ps.setString(4, infoList.get(i).getTransactionType());
				ps.setBigDecimal(5, infoList.get(i).getCount());
				ps.setBigDecimal(6, infoList.get(i).getAmount());
			}
			
			@Override
			public int getBatchSize() {
				return infoList.size();
			}
		});
	}
	
	/**
	 * 將產出的 output 檔新增到 FY_TB_FILE_CNTRL
	 * */
	public void insertFileCntrlByOutputType(String outputType, String fileName) {
		
		log.info(">>>>> Insert FY_TB_FILE_CNTRL <<<<<");
			
		String cntrlNextVal = "SELECT FY_SQ_FILE_CNTRL.NEXTVAL FROM DUAL";
		
		Map<String, Object> resultMap = jdbcTemplate.queryForMap(cntrlNextVal);
		
		Integer fileSeq = Integer.parseInt(resultMap.get("NEXTVAL").toString());
		
		String sql = "INSERT INTO FY_TB_FILE_CNTRL (FILE_SEQ, FILE_TYPE, FILENAME, STATUS) "
					+ "VALUES (?,?,?,?)";
		
		jdbcTemplate.update(sql, fileSeq, outputType, fileName, "I");
	}
	
	/**
	 * 將該處理的資料狀態改為 S
	 * 若修正錯誤後將 DSCR 改為 null
	 * */
	public void updateFileCntrl(Fy_tb_file_cntrl fy_tb_file_cntrl) {
		
		log.info(">>>>> Update FY_TB_FILE_CNTRL <<<<<");
		
		String sql = "UPDATE FY_TB_FILE_CNTRL "
					+ "SET STATUS = ?, END_TIME = ?, TOTAL_COUNT = ?, TOTAL_AMOUNT = ?, MIN_TRANSACTION_TIME = ?, MAX_TRANSACTION_TIME = ?, DSCR = ?, ERROR_COUNT = ? "
					+ "WHERE FILE_SEQ = ? ";

		jdbcTemplate.update(sql, fy_tb_file_cntrl.getStatus(),
									fy_tb_file_cntrl.getEndTime(),
									fy_tb_file_cntrl.getTotalCount(),
									fy_tb_file_cntrl.getTotalAmount(),
									fy_tb_file_cntrl.getMinTransactionTime(),
									fy_tb_file_cntrl.getMaxTransactionTime(),
									fy_tb_file_cntrl.getDscr(),
									fy_tb_file_cntrl.getErrorCount(),
									fy_tb_file_cntrl.getFileSeq());
	}
	
	/**
	 * 更新該筆資料狀態為 E，並新增錯誤原因
	 * */
	public void updateFileCntrlError(BigDecimal fileSeq, String dscr) {
		
		log.info(">>>>> Update FY_TB_FILE_CNTRL ERROR<<<<<");
		
		String sql = "UPDATE FY_TB_FILE_CNTRL " 
					+ "SET STATUS = 'E', END_TIME = ?, DSCR = ? " 
					+ "WHERE FILE_SEQ = ? "
					+ "AND STATUS = 'W' " ;
	
		jdbcTemplate.update(sql, LocalDateTime.now(), dscr, fileSeq);
	}
	
	/**
	 * 手動 RollBack 
	 * FY_TB_TRANSACTION
	 * */
	public void rollbackTransaction(BigDecimal fileSeq) {
		
		log.info(">>>>> ROLLBACK FY_TB_TRANSACTION <<<<<");
		
		String sql = "DELETE FROM FY_TB_TRANSACTION WHERE FILE_SEQ = ?";
		
		jdbcTemplate.update(sql, fileSeq);
	}
	
	/**
	 * 手動 RollBack 
	 * FY_TB_FILE_INFO
	 * */
	public void rollbackFileInfo(BigDecimal fileSeq) {
		
		log.info(">>>>> ROLLBACK FY_TB_FILE_INFO <<<<<");
		
		String sql = "DELETE FROM FY_TB_FILE_INFO WHERE FILE_SEQ = ?";
		
		jdbcTemplate.update(sql, fileSeq);
	}
	
	/**
	 * 將新增的 output 檔資料手動 RollBack 
	 * FY_TB_FILE_CNTRL
	 * */
	public void rollbackFileCntrlByOutputType(String fileName, String outputType) {
		
		log.info(">>>>> ROLLBACK FY_TB_FILE_CNTRL <<<<<");
		
		String sql = "DELETE FROM FY_TB_FILE_CNTRL WHERE FILENAME = ? AND FILE_TYPE = ?";
		
		jdbcTemplate.update(sql, fileName, outputType);
	}
}
