package com.flow.one.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.flow.one.dto.Snapshot;

/**
 * 搜尋效能比較中可執行的一次搜尋操作。
 */
@FunctionalInterface
interface SearchOperation {
	/**
	 * 執行一次搜尋操作。
	 *
	 * @return 搜尋結果快照
	 * @throws IOException 搜尋操作讀取 JSON 失敗時
	 * @throws SQLException 搜尋操作執行 SQLite 查詢失敗時
	 */
	Snapshot run() throws IOException, SQLException;
}
