package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.Record

data class FootprintPlace(
    val name: String,
    val count: Int,
    val gp: Int,
    val lastAt: Long,
    val records: List<Record>,
)

fun groupFootprints(records: List<Record>): List<FootprintPlace> {
    return records
        .mapNotNull { rec -> rec.locationName?.trim()?.takeIf { it.isNotEmpty() }?.let { it to rec } }
        .groupBy({ it.first }, { it.second })
        .map { (name, rows) ->
            FootprintPlace(
                name = name,
                count = rows.size,
                gp = rows.sumOf { it.gpEarned },
                lastAt = rows.maxOf { it.createdAt },
                records = rows.sortedByDescending { it.createdAt },
            )
        }
        .sortedWith(compareByDescending<FootprintPlace> { it.count }.thenByDescending { it.lastAt })
}
