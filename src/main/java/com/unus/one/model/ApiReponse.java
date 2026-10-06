package com.unus.one.model;

public class ApiReponse<T> {
  public boolean success;
  public T data;
  public ApiError error;

  /**
   * 建立 API 回應資料。
   *
   * @param success 回應是否成功
   * @param data 成功時的回應資料
   * @param error 失敗時的錯誤資料
   */
  public ApiReponse(boolean success, T data, ApiError error) {
    this.success = success;
    this.data = data;
    this.error = error;

  }

  /**
   * 建立不含資料的成功回應。
   *
   * @return 成功回應
   */
  public ApiReponse<Void> success() {
    return new ApiReponse<>(true, null, null);
  }

  /**
   * 建立包含資料的成功回應。
   *
   * @param <T> 回應資料型別
   * @param data 成功時的回應資料
   * @return 成功回應
   */
  public static <T> ApiReponse<T> success(T data) {
    return new ApiReponse<>(true, data, null);
  }

  /**
   * 建立錯誤回應。
   *
   * @param <T> 回應資料型別
   * @param code 錯誤代碼
   * @param message 錯誤摘要
   * @param details 錯誤詳細說明
   * @return 錯誤回應
   */
  public static <T> ApiReponse<T> error(Integer code, String message, String details) {
    return new ApiReponse<>(false, null, new ApiError(code, message, details));
  }
}
