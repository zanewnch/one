package com.flow.one.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.flow.one.dto.SearchBenchmarkResponse;
import com.flow.one.dto.SearchBenchmarkResponse.MethodResult;
import com.flow.one.dto.SearchBenchmarkResponse.SearchPreview;
import com.flow.one.dto.Snapshot;
import com.flow.one.dto.TimedSnapshot;
import com.flow.one.repository.SearchBenchmarkRepository;
import com.flow.one.repository.SearchBenchmarkRepository.SearchSnapshot;
import com.flow.one.service.SearchBenchmarkService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service

public class SearchBenchmarkServiceImpl implements SearchBenchmarkService {

  private static final int PREVIEW_LIMIT = 5;

  private static final int EXCERPT_LENGTH = 240;

  private static final String METHODOLOGY = "One warm-up per method is excluded. Timed runs alternate order. "
      + "SQLite timing includes JDBC connection and query; JSON timing includes file read, parse, and scan. "
      + "HTTP serialization is excluded. Results report server-side elapsed time, not a stable performance guarantee.";

  private final Path jsonPath;
  private final ObjectMapper objectMapper;
  private final SearchBenchmarkRepository searchBenchmarkRepository;

  /**
   * 建立 benchmark service，並注入資料路徑、JSON 解析器與 SQLite repository。
   *
   * @param jsonPath                  JSON 資料檔路徑
   * @param objectMapper              Jackson JSON 解析器
   * @param searchBenchmarkRepository SQLite 搜尋 repository
   */
  public SearchBenchmarkServiceImpl(
      @Value("${demo.data.json-path:data/source/squad-dev-v2.0.json}") String jsonPath,
      ObjectMapper objectMapper,
      SearchBenchmarkRepository searchBenchmarkRepository) {
    this.jsonPath = Path.of(jsonPath).toAbsolutePath().normalize();
    this.objectMapper = objectMapper;
    this.searchBenchmarkRepository = searchBenchmarkRepository;
  }

  /**
   * 比較指定查詢在 SQLite 與 JSON 搜尋中的執行時間。
   *
   * @param query     要搜尋的文字
   * @param runCounts 每種搜尋方法的計時次數
   * @return 搜尋結果與計時統計
   * @throws IOException  無法讀取 JSON 資料檔時
   * @throws SQLException SQLite 查詢失敗時
   */
  @Override
  public SearchBenchmarkResponse compare(String query, int runCounts) throws IOException, SQLException {

    String normalizedQuery = query.trim();
    searchSqlite(normalizedQuery);
    searchJson(normalizedQuery);
    List<Long> sqliteSamples = new ArrayList<>(runCounts);
    List<Long> jsonSamples = new ArrayList<>(runCounts);
    Snapshot sqliteSnapshot = null;
    Snapshot jsonSnapshot = null;

    for (int i = 0; i < runCounts; i++) {

      if (i % 2 == 0) {

        TimedSnapshot sqlite = time(() -> searchSqlite(normalizedQuery));
        TimedSnapshot json = time(() -> searchJson(normalizedQuery));

        sqliteSamples.add(sqlite.elapsedNanos());
        jsonSamples.add(json.elapsedNanos());
        sqliteSnapshot = sqlite.snapshot();
        jsonSnapshot = json.snapshot();

      } else {

        TimedSnapshot json = time(() -> searchJson(normalizedQuery));

        TimedSnapshot sqlite = time(() -> searchSqlite(normalizedQuery));

        jsonSamples.add(json.elapsedNanos());
        sqliteSamples.add(sqlite.elapsedNanos());
        jsonSnapshot = json.snapshot();
        sqliteSnapshot = sqlite.snapshot();
      }
    }

    return new SearchBenchmarkResponse(
        normalizedQuery,
        runCounts,
        METHODOLOGY,
        toMethodResult("SQLite JDBC query", sqliteSamples, sqliteSnapshot),
        toMethodResult("JSON parse and scan", jsonSamples, jsonSnapshot));

  }

  /**
   * 透過 repository 執行 SQLite 搜尋，並建立搜尋結果快照。
   *
   * @param query 要搜尋的文字
   * @return 命中數與預覽結果
   * @throws SQLException SQLite 查詢失敗時
   */
  private Snapshot searchSqlite(String query) throws SQLException {

    SearchSnapshot result = searchBenchmarkRepository.search(query);

    return new Snapshot(result.matchCount(), result.preview());

  }

  /**
   * 讀取並解析 JSON 資料，再掃描符合查詢文字的內容。
   *
   * @param query 要搜尋的文字
   * @return 命中數與預覽結果
   * @throws IOException JSON 檔案無法讀取或格式不正確時
   */
  private Snapshot searchJson(String query) throws IOException {

    String json = Files.readString(jsonPath);
    JsonNode root = objectMapper.readTree(json);
    JsonNode articles = root.path("data");

    if (!articles.isArray()) {

      throw new IOException("SQuAD JSON does not contain a data array: " + jsonPath);

    }

    String normalizedQuery = query.toLowerCase(Locale.ROOT);
    long count = 0;
    List<SearchPreview> preview = new ArrayList<>(PREVIEW_LIMIT);
    int articleIndex = 0;

    for (JsonNode article : articles) {

      String documentId = "squad-v2-dev-%03d".formatted(articleIndex++);
      String title = article.path("title").asString("");
      JsonNode paragraphs = article.path("paragraphs");

      if (!paragraphs.isArray()) {
        continue;
      }

      int chunkIndex = 0;

      for (JsonNode paragraph : paragraphs) {

        String chunkId = "%s-p%04d".formatted(documentId, chunkIndex++);
        String content = paragraph.path("context").asString("");

        if (content.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
          count++;

          if (preview.size() < PREVIEW_LIMIT) {
            preview.add(new SearchPreview(chunkId, title, excerpt(content)));
          }
        }
      }
    }

    return new Snapshot(count, List.copyOf(preview));

  }

  /**
   * 將內容截短為預覽摘要長度。
   *
   * @param content 原始內容
   * @return 完整內容或截短後的摘要
   */
  private static String excerpt(String content) {

    return content.length() <= EXCERPT_LENGTH
        ? content
        : content.substring(0, EXCERPT_LENGTH) + "…";
  }

  /**
   * 執行搜尋操作並記錄經過時間。
   *
   * @param operation 要計時的搜尋操作
   * @return 搜尋結果快照與耗時奈秒數
   * @throws IOException  搜尋操作讀取 JSON 失敗時
   * @throws SQLException 搜尋操作執行 SQLite 查詢失敗時
   */
  private static TimedSnapshot time(SearchOperation operation) throws IOException, SQLException {

    long started = System.nanoTime();
    Snapshot snapshot = operation.run();

    return new TimedSnapshot(snapshot, System.nanoTime() - started);

  }

  /**
   * 將單一搜尋方法的計時樣本整理成結果資料。
   *
   * @param method   搜尋方法名稱
   * @param samples  計時樣本，單位為奈秒
   * @param snapshot 搜尋結果快照
   * @return 包含統計值與預覽內容的方法結果
   */
  private static MethodResult toMethodResult(String method, List<Long> samples, Snapshot snapshot) {

    List<Long> sorted = new ArrayList<>(samples);

    Collections.sort(sorted);

    double average = samples.stream()
        .mapToLong(sample -> Objects.requireNonNull(sample, "Benchmark sample must not be null"))
        .average()
        .orElse(0) / 1_000.0;

    double median = sorted.size() % 2 == 0
        ? (sorted.get(sorted.size() / 2 - 1) / 1_000.0 + sorted.get(sorted.size() / 2) / 1_000.0) / 2.0
        : sorted.get(sorted.size() / 2) / 1_000.0;

    List<Double> sampleMicros = samples.stream().map(nanos -> nanos / 1_000.0).toList();

    return new MethodResult(
        method,
        snapshot.matchCount(),
        average,
        median,
        sorted.getFirst() / 1_000.0,
        sorted.getLast() / 1_000.0,
        sampleMicros,
        snapshot.preview());
  }
}
