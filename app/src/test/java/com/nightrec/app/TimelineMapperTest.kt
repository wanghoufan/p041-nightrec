package com.nightrec.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** T049/T050：segment + GAP 双时间映射的红/绿测试。 */
class TimelineMapperTest {

    // 录 5m -> AWAY -> 恢复 -> 录 5m：逻辑时间连续，墙钟出现真实 gap。
    private val twoSegments = listOf(
        TimedSegment("a", logicalStart = 0, duration = 300_000, wallStart = 1_000_000),
        TimedSegment("b", logicalStart = 300_000, duration = 300_000, wallStart = 1_900_000),
    )

    @Test fun logicalDurationExcludesGap() {
        val mapper = TimelineMapper(twoSegments)
        assertEquals(600_000, mapper.duration)
    }

    @Test fun seekResolvesSegmentAndOffset() {
        val mapper = TimelineMapper(twoSegments)
        assertEquals(SegmentPosition(0, 0), mapper.seek(0))
        assertEquals(SegmentPosition(0, 299_999), mapper.seek(299_999))
        assertEquals(SegmentPosition(1, 0), mapper.seek(300_000))
        assertEquals(SegmentPosition(1, 123), mapper.seek(300_123))
    }

    @Test fun wallTimeFollowsTheOwningSegmentAnchor() {
        val mapper = TimelineMapper(twoSegments)
        // 逻辑 0 = 第一段墙钟锚点；逻辑 300_000 = 第二段墙钟锚点（跨越了 600s 的真实 gap）。
        assertEquals(1_000_000, mapper.wallTime(0))
        assertEquals(300_000 - 1 + 1_000_000, mapper.wallTime(299_999))
        assertEquals(1_900_000, mapper.wallTime(300_000))
    }

    @Test fun nonContiguousSegmentsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TimelineMapper(
                listOf(
                    TimedSegment("a", 0, 1000, 0),
                    TimedSegment("b", 1500, 1000, 2000), // 与上一段之间凭空缺口：非法
                ),
            )
        }
    }

    @Test fun nonPositiveDurationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TimelineMapper(listOf(TimedSegment("a", 0, 0, 0)))
        }
    }

    @Test fun emptyTimelineHasZeroDuration() {
        val mapper = TimelineMapper(emptyList())
        assertEquals(0, mapper.duration)
    }
}