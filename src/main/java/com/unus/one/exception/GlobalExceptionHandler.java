package com.unus.one.exception;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * 建立符合 RFC 9457 的安全錯誤回應，並記錄伺服器端錯誤。
   *
   * @param ex 發生的例外
   * @param headers 要保留的 HTTP 標頭
   * @param statusCode 回應的 HTTP 狀態碼
   * @param request 目前的 HTTP 請求
   * @return ProblemDetail 回應；若父類別已提交回應則為 {@code null}
   */
  private @Nullable ResponseEntity<Object> buildProblemResponse(
      Exception ex, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

    String detail = mapExceptionToDetail(ex, statusCode);

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusCode, detail);
    problem.setType(URI.create("about:blank"));
    if (request instanceof ServletWebRequest servletRequest) {
      problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
    }

    if (statusCode.is5xxServerError()) {
      logger.error("Server error (HTTP " + statusCode.value() + ")", ex);
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.putAll(headers);
    responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    return super.handleExceptionInternal(ex, problem, responseHeaders, statusCode, request);
  }

  /**
   * 處理沒有更專門 handler 接手的應用程式例外，並回傳安全的錯誤本文。
   *
   * @param ex 發生的例外
   * @param request 目前的 HTTP 請求
   * @return ProblemDetail 錯誤回應；若父類別已提交回應則為 {@code null}
   */
  @ExceptionHandler(Exception.class)
  public @Nullable ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {

    HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
    HttpHeaders headers = new HttpHeaders();

    if (ex instanceof ErrorResponse errorResponse) {
      status = errorResponse.getStatusCode();
      headers = errorResponse.getHeaders();
    }

    return buildProblemResponse(ex, headers, status, request);
  }

  /**
   * 依例外類型與 HTTP 狀態選擇不洩漏內部資訊的錯誤說明。
   *
   * @param ex 發生的例外
   * @param status 對應的 HTTP 狀態碼
   * @return 提供給 API 呼叫端的安全錯誤說明
   */
  private static String mapExceptionToDetail(Exception ex, HttpStatusCode status) {
    if (status.is5xxServerError()) {
      if (ex instanceof IOException) {
        return "Unable to read server data.";
      }
      if (ex instanceof SQLException) {
        return "Unable to access server data.";
      }
      return "An unexpected server error occurred.";
    }

    return switch (status.value()) {
      case 404 -> "The requested resource was not found.";
      case 405 -> "The HTTP method is not supported for this resource.";
      case 406 -> "The requested response format is not supported.";
      case 415 -> "The request content type is not supported.";
      default -> "The request could not be completed.";
    };
  }

}
