package com.unus.one.dto;

import java.util.List;

import com.unus.one.dto.SearchBenchmarkResponse.SearchPreview;

/**
 * 搜尋操作回傳的命中數與預覽結果。
 *
 * @param matchCount 命中數
 * @param preview 搜尋結果預覽
 */
public record Snapshot(long matchCount, List<SearchPreview> preview) {
}