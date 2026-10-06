package com.unus.one.service;

import java.io.IOException;
import java.sql.SQLException;

import com.unus.one.dto.SearchBenchmarkResponse;

public interface SearchBenchmarkService {
	/**
	 * 比較指定查詢在 SQLite 與 JSON 搜尋中的執行時間。
	 *
	 * @param query 要搜尋的文字
	 * @param runCounts 每種搜尋方法的計時次數
	 * @return 搜尋結果與計時統計
	 * @throws IOException 無法讀取 JSON 資料檔時
	 * @throws SQLException SQLite 查詢失敗時
	 */
	SearchBenchmarkResponse compare(String query, int runCounts) throws IOException, SQLException;
}
