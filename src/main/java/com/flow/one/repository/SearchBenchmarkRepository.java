package com.flow.one.repository;

import java.sql.SQLException;
import java.util.List;

import com.flow.one.dto.SearchBenchmarkResponse.SearchPreview;

public interface SearchBenchmarkRepository {
	/**
	 * 使用 SQLite 搜尋符合的內容。
	 *
	 * @param query 要搜尋的文字
	 * @return 命中數與預覽結果
	 * @throws SQLException SQLite 查詢失敗時
	 */
	SearchSnapshot search(String query) throws SQLException;

	/**
	 * SQLite 搜尋結果快照。
	 *
	 * @param matchCount 命中數
	 * @param preview 搜尋結果預覽
	 */
	record SearchSnapshot(long matchCount, List<SearchPreview> preview) {
	}
}
