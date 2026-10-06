package com.unus.one.model;

public class ApiError {
  public Integer code;
  public String message;
  public String details;

  /**
   * 建立 API 錯誤資料。
   *
   * @param code 錯誤代碼
   * @param message 錯誤摘要
   * @param details 錯誤詳細說明
   */
  public ApiError(Integer code, String message, String details) {
    this.code = code;
    this.message = message;
    this.details = details;
  }
}
