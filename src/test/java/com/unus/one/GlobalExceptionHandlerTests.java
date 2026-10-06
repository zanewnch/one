package com.unus.one;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.unus.one.controller.SearchBenchmarkController;
import com.unus.one.dto.SearchBenchmarkResponse;
import com.unus.one.dto.SearchBenchmarkResponse.MethodResult;
import com.unus.one.exception.GlobalExceptionHandler;
import com.unus.one.service.SearchBenchmarkService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/*
 * WebMvcTest 只啟動 Web 層；MockitoBean 提供 Service 替身，
 * 因此錯誤情境不需要讀取真實資料集或修改資料庫。
 * ProbeController 只存在於測試，補足目前 GET API 沒有的 Request Body 等情境。
 */
@WebMvcTest(SearchBenchmarkController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTests.ProbeController.class})
class GlobalExceptionHandlerTests {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private SearchBenchmarkService service;

  @Test
  void successfulComparisonKeepsOriginalResponseShape() throws Exception {
    MethodResult method = new MethodResult("test", 1, 1, 1, 1, 1, List.of(1.0), List.of());
    when(service.compare("Normandy", 1))
        .thenReturn(new SearchBenchmarkResponse("Normandy", 1, "test", method, method));
    MvcResult result = mockMvc.perform(compare("Normandy", "1"))
        .andExpect(status().isOk()).andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertEquals("Normandy", body.path("query").asText());
    assertEquals(1, body.path("sqlite").path("matchCount").asLong());
    assertFalse(body.has("data"));
    assertFalse(body.has("success"));
    assertFalse(body.has("code"));
  }

  @Test
  void parameterValidationUsesDefaultMessages() throws Exception {
    String blankQueryDetail = problem(compare(" ", "1"), 400).path("detail").asText();
    String minimumRunsDetail = problem(compare("Normandy", "0"), 400).path("detail").asText();
    String maximumRunsDetail = problem(compare("Normandy", "101"), 400).path("detail").asText();
    assertFalse(blankQueryDetail.contains("q must not be blank"));
    assertFalse(minimumRunsDetail.contains("runCounts must be at least 1"));
    assertFalse(maximumRunsDetail.contains("runCounts must be at most 100"));
  }

  @Test
  void missingAndWrongTypeParametersUseSpringBadRequestHandling() throws Exception {
    problem(get("/api/benchmark/compare"), 400);
    problem(compare("Normandy", "private-invalid-input"), 400);
  }

  @Test
  void requestBodyValidationUsesSpringDefaultDetail() throws Exception {
    JsonNode body = problem(post("/probe/body").contentType(MediaType.APPLICATION_JSON)
        .content("{\"value\":\"\",\"name\":\"\"}"), 400);
    assertFalse(body.path("detail").asText().isBlank());
    assertEquals("Invalid request content.", body.path("detail").asText());
  }

  @Test
  void malformedAndMissingBodiesHaveSafeDetails() throws Exception {
    problem(post("/probe/body").contentType(MediaType.APPLICATION_JSON)
        .content("{private-broken-json"), 400);
    problem(post("/probe/body").contentType(MediaType.APPLICATION_JSON), 400);
  }

  @Test
  void unsupportedMediaFormatsKeepTheirHttpStatuses() throws Exception {
    problem(post("/probe/body").contentType(MediaType.TEXT_PLAIN).content("test"),
        415);
    problem(post("/probe/body").contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN)
        .content("{\"value\":\"OK\",\"name\":\"Ada\"}"), 406);
  }

  @Test
  void missingResourceOmitsQueryStringAndRejectedInput() throws Exception {
    JsonNode body = problem(get("/missing-endpoint").param("secret", "private-input"), 404);
    assertEquals("/missing-endpoint", body.path("instance").asText());
    assertFalse(body.toString().contains("private-input"));
  }

  @Test
  void wrongHttpMethodPreservesAllowHeader() throws Exception {
    problem(post("/api/benchmark/compare"), 405);
    mockMvc.perform(post("/api/benchmark/compare"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET"));
  }

  @Test
  void serviceFailuresDoNotDiscloseInternalMessages() throws Exception {
    Exception[] failures = {
        new IOException("private filesystem path"),
        new SQLException("private SQL and credentials"),
        new IllegalArgumentException("private implementation detail"),
        new NullPointerException("private null-reference detail")
    };
    for (int i = 0; i < failures.length; i++) {
      doThrow(failures[i]).when(service).compare("Normandy", 1);
      JsonNode body = problem(compare("Normandy", "1"), 500);
      assertFalse(body.toString().contains("private"));
      assertFalse(body.has("exception"));
      assertFalse(body.has("trace"));
    }
  }

  @Test
  void invalidReturnValuesAreInternalErrors() throws Exception {
    JsonNode body = problem(get("/probe/return-value"), 500);
    assertFalse(body.toString().contains("private return-value message"));
  }

  @Test
  void explicitHttpExceptionsUseSpringStatusAndProblemDetail() throws Exception {
    problem(get("/probe/conflict"), 409);
    problem(get("/probe/unavailable"), 503);
  }

  /*
   * 共用斷言檢查 RFC 9457 核心欄位以及正確的錯誤媒體型別。
   * 回傳 JSON 節點，讓各測試再檢查情境特有的 detail 或 instance。
   */
  private JsonNode problem(MockHttpServletRequestBuilder request, int statusCode) throws Exception {
    MvcResult result = mockMvc.perform(request)
        .andExpect(status().is(statusCode))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    // RFC 9457 defines an omitted type as equivalent to "about:blank".
    assertTrue(!body.has("type") || "about:blank".equals(body.path("type").asText()));
    assertEquals(HttpStatus.valueOf(statusCode).getReasonPhrase(), body.path("title").asText());
    assertEquals(statusCode, body.path("status").asInt());
    assertFalse(body.has("code"));
    assertFalse(body.path("detail").asText().isBlank());
    assertTrue(body.path("instance").asText().startsWith("/"));
    assertFalse(body.has("error"));
    return body;
  }

  private static MockHttpServletRequestBuilder compare(String query, String runCounts) {
    return get("/api/benchmark/compare").param("q", query).param("runCounts", runCounts);
  }

  @RestController
  static class ProbeController {
    @PostMapping(value = "/probe/body", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ProbeRequest body(@Valid @RequestBody ProbeRequest body) {
      return body;
    }

    @GetMapping("/probe/return-value")
    @NotBlank(message = "private return-value message")
    public String invalidReturnValue() {
      return "";
    }

    @GetMapping("/probe/conflict")
    public String conflict() {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "private conflict reason");
    }

    @GetMapping("/probe/unavailable")
    public String unavailable() {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "private server reason");
    }
  }

  record ProbeRequest(
      @NotBlank(message = "z field invalid") @Size(min = 2, message = "z field invalid") String value,
      @NotBlank(message = "a field invalid") String name) {
  }
}
