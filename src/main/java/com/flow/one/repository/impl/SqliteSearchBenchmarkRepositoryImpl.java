package com.flow.one.repository.impl;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.flow.one.dto.SearchBenchmarkResponse.SearchPreview;
import com.flow.one.repository.SearchBenchmarkRepository;
import com.flow.one.repository.SearchBenchmarkRepository.SearchSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class SqliteSearchBenchmarkRepositoryImpl implements SearchBenchmarkRepository {
	private static final int PREVIEW_LIMIT = 5;
	private static final int EXCERPT_LENGTH = 240;
	private static final String SQLITE_QUERY = """
			SELECT c.chunk_id, d.title, c.content
			FROM chunks c
			JOIN documents d ON d.document_id = c.document_id
			WHERE instr(lower(c.content), lower(?)) > 0
			ORDER BY c.document_id, c.chunk_index
			""";

	private final Path sqlitePath;

	/**
	 * 建立 SQLite repository，並將資料庫路徑轉成正規化的絕對路徑。
	 *
	 * @param sqlitePath SQLite 資料庫路徑設定
	 */
	public SqliteSearchBenchmarkRepositoryImpl(
			@Value("${demo.data.sqlite-path:data/squad-demo.sqlite3}") String sqlitePath) {
		this.sqlitePath = Path.of(sqlitePath).toAbsolutePath().normalize();
	}

	/**
	 * 搜尋 SQLite 中符合查詢文字的內容，並保留前幾筆預覽。
	 *
	 * @param query 要搜尋的文字
	 * @return 命中數與預覽結果
	 * @throws SQLException SQLite 查詢失敗時
	 */
	@Override
	public SearchSnapshot search(String query) throws SQLException {
		String jdbcUrl = "jdbc:sqlite:" + sqlitePath;
		try (Connection connection = DriverManager.getConnection(jdbcUrl);
				PreparedStatement statement = connection.prepareStatement(SQLITE_QUERY)) {
			statement.setString(1, query);
			try (ResultSet results = statement.executeQuery()) {
				long count = 0;
				List<SearchPreview> preview = new ArrayList<>(PREVIEW_LIMIT);
				while (results.next()) {
					count++;
					if (preview.size() < PREVIEW_LIMIT) {
						preview.add(new SearchPreview(
								results.getString("chunk_id"),
								results.getString("title"),
								excerpt(results.getString("content"))));
					}
				}
				return new SearchSnapshot(count, List.copyOf(preview));
			}
		}
	}

	/**
	 * 將內容截短為 API 預覽文字長度。
	 *
	 * @param content 原始內容
	 * @return 完整內容或附省略號的截短內容
	 */
	private static String excerpt(String content) {
		return content.length() <= EXCERPT_LENGTH
				? content
				: content.substring(0, EXCERPT_LENGTH) + "…";
	}
}
