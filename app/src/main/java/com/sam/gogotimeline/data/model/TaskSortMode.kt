package com.sam.gogotimeline.data.model

/** How rows are ordered on every board (spec 9). Only [MANUAL] lets rows be reordered by hand. */
enum class TaskSortMode {
    MANUAL,
    START_TIME,
    DURATION,
}
