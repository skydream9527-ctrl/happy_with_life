package com.xiaoquexing.app.util

import java.time.Instant
import java.time.ZoneId

/**
 * 发生日 epoch day（ADR D2/D3 的唯一日期口径）。
 * 必须基于本地时区的 LocalDate 计算，禁止用毫秒减法/截断（DST 安全）。
 */
object DateKeys {
    fun epochDay(timestampMs: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate().toEpochDay().toInt()
}
