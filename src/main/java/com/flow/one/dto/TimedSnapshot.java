package com.flow.one.dto;

/**
 * 儲存搜尋結果快照與操作耗時。
 *
 * @param snapshot 搜尋結果快照
 * @param elapsedNanos 經過時間，單位為奈秒
 */
public record TimedSnapshot(Snapshot snapshot, long elapsedNanos) {
}