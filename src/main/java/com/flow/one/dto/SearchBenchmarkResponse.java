package com.flow.one.dto;

import java.util.List;

/**
 * 搜尋效能比較 API 的回應資料。
 *
 * @param query 執行搜尋的查詢文字
 * @param runCounts 每種搜尋方法的計時次數
 * @param methodology 計時方式說明
 * @param sqlite SQLite 搜尋結果與統計
 * @param jsonParser JSON 搜尋結果與統計
 */
public record SearchBenchmarkResponse(
		String query,
		int runCounts,
		String methodology,
		MethodResult sqlite,
		MethodResult jsonParser) {

	/**
	 * 單一搜尋方法的命中數、計時統計與預覽結果。
	 *
	 * @param method 搜尋方法名稱
	 * @param matchCount 命中數
	 * @param averageMicros 平均耗時，單位為微秒
	 * @param medianMicros 中位耗時，單位為微秒
	 * @param minMicros 最短耗時，單位為微秒
	 * @param maxMicros 最長耗時，單位為微秒
	 * @param samplesMicros 每次計時結果，單位為微秒
	 * @param preview 搜尋結果預覽
	 */
	public record MethodResult(
			String method,
			long matchCount,
			double averageMicros,
			double medianMicros,
			double minMicros,
			double maxMicros,
			List<Double> samplesMicros,
			List<SearchPreview> preview) {
	}

	/**
	 * 搜尋結果中的單筆預覽。
	 *
	 * @param chunkId 內容片段識別碼
	 * @param documentTitle 文件標題
	 * @param excerpt 內容摘要
	 */
	public record SearchPreview(String chunkId, String documentTitle, String excerpt) {
	}
}
