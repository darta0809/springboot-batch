package com.foyatech.training.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utility {
	
	/*
	 * 當傳入文字長度不足時，左邊補空格
	 * */
	public static String fillSpaceLeft(String s, int len) {
		return String.format("%1$" + len + "s", s);
	}
	
	/*
	 * 當傳入文字長度不足時，右邊補空格
	 * */
	public static String fillSpaceRight(String s, int len) {
		return String.format("%1$-" + len + "s", s);
	}
	
	/*
	 * 將字串格式 (yyyyMMddHHmmss)，轉為 LocalDateTime
	 * */
	public static LocalDateTime parseDateTime(String s) {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		
		return LocalDateTime.parse(s, formatter);
	}
	
	/*
	 * 將日期轉為字串格式 (yyyyMMddHHmmss)
	 * */
	public static String dateTimeFormat(LocalDateTime date) {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		
		return date.format(formatter);
	}
	
	/*
	 * 根據設定檔 Table 組成動態 SQL
	 * */
	public static String generateInsert(String tableName, String[] columnsName, String[] columnsType) {
		
		String columns = Arrays.stream(columnsName).collect(Collectors.joining(","));
		
		String[] values = new String[columnsName.length];
		
		for (int i = 0; i < values.length; i++) {
			values[i] = "?";
		}
		
		String value = Arrays.stream(values).collect(Collectors.joining(","));
		
		String sql = "INSERT INTO " + tableName + " ( " + columns + " ) " + "VALUES ( " + value + " ) ";
		
		return sql;
	}
}
