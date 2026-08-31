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
        .filter { !it.locationName.isNullOrBlank() }
        .groupBy { it.locationName!!.trim() }
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
