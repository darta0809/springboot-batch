package com.example.demo.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Util {

  private Util() {
  }

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
    return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
  }
}
