

package com.flow.one.controller;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flow.one.dto.SearchBenchmarkResponse;
import com.flow.one.service.SearchBenchmarkService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController

@RequestMapping("/api/benchmark")

public class SearchBenchmarkController {

  private static final int MIN_RUNS = 1;

  private static final int MAX_RUNS = 100;

  private final SearchBenchmarkService benchmarkService;

  /**
   * 建立控制器並注入 benchmark service。
   *
   * @param benchmarkService 執行搜尋效能比較的 service
   */
  public SearchBenchmarkController(SearchBenchmarkService benchmarkService) {

    this.benchmarkService = benchmarkService;

  }

  /**
   * 比較指定查詢在 SQLite 與 JSON 搜尋中的執行時間。
   *
   * @param query 要搜尋的文字
   * @param runCounts 每種搜尋方法的計時次數
   * @return 搜尋結果與計時統計
   * @throws IOException 無法讀取 JSON 資料檔時
   * @throws SQLException SQLite 查詢失敗時
   */
  @GetMapping("/compare")
  public SearchBenchmarkResponse compare(
      @RequestParam(name = "q") @NotBlank String query,
      @RequestParam(name = "runCounts", defaultValue = "10")
      @Min(value = MIN_RUNS)
      @Max(value = MAX_RUNS) int runCounts) throws IOException, SQLException {

    return benchmarkService.compare(query, runCounts);
  }
}
