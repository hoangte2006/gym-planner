
package com.gym.scheduling;

import org.junit.jupiter.api.Test;

import com.gym.scheduling.service.FreeSlotFinder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreeSlotFinderTest {

    private static final LocalDateTime DAY =
            LocalDateTime.of(2026, 10, 19, 0, 0);

    private LocalDateTime at(int hour, int minute) {
        return DAY.withHour(hour).withMinute(minute);
    }

    private FreeSlotFinder.Interval interval(
            int startHour, int startMinute,
            int endHour, int endMinute) {
        return new FreeSlotFinder.Interval(
                at(startHour, startMinute),
                at(endHour, endMinute)
        );
    }

    private List<FreeSlotFinder.Interval> find(
            int startHour, int endHour,
            FreeSlotFinder.Interval... busy) {
        return FreeSlotFinder.findFreeSlots(
                at(startHour, 0),
                at(endHour, 0),
                List.of(busy)
        );
    }

    @Test
    void noBusyIntervalsReturnsEntireWindow() {
        assertEquals(
                List.of(interval(8, 0, 12, 0)),
                find(8, 12)
        );
    }

    @Test
    void fullyBusyWindowReturnsNoFreeSlots() {
        assertTrue(find(8, 12, interval(8, 0, 12, 0)).isEmpty());
    }

    @Test
    void busyIntervalInMiddleReturnsTwoFreeSlots() {
        assertEquals(
                List.of(
                        interval(8, 0, 9, 0),
                        interval(10, 0, 12, 0)
                ),
                find(8, 12, interval(9, 0, 10, 0))
        );
    }

    @Test
    void busyIntervalAtBeginning() {
        assertEquals(
                List.of(interval(9, 0, 12, 0)),
                find(8, 12, interval(8, 0, 9, 0))
        );
    }

    @Test
    void busyIntervalAtEnd() {
        assertEquals(
                List.of(interval(8, 0, 11, 0)),
                find(8, 12, interval(11, 0, 12, 0))
        );
    }

    @Test
    void overlappingIntervalsAreMerged() {
        assertEquals(
                List.of(
                        interval(8, 0, 8, 30),
                        interval(10, 0, 12, 0)
                ),
                find(
                        8, 12,
                        interval(8, 30, 9, 30),
                        interval(9, 0, 10, 0)
                )
        );
    }
    
    @Test
    void nestedIntervalsAreMerged() {
        assertEquals(
                List.of(
                        interval(8, 0, 9, 0),
                        interval(10, 0, 12, 0)
                ),
                find(
                        8, 12,
                        interval(9, 0, 10, 0),
                        interval(9, 15, 9, 45)
                )
        );
    }

    @Test
    void adjacentBusyIntervalsAreMerged() {
        assertEquals(
                List.of(
                        interval(8, 0, 9, 0),
                        interval(10, 0, 12, 0)
                ),
                find(
                        8, 12,
                        interval(9, 0, 9, 30),
                        interval(9, 30, 10, 0)
                )
        );
    }

    @Test
    void unsortedIntervalsStillProduceCorrectResult() {
        assertEquals(
                List.of(
                        interval(8, 0, 9, 0),
                        interval(10, 0, 11, 0),
                        interval(11, 30, 12, 0)
                ),
                find(
                        8, 12,
                        interval(11, 0, 11, 30),
                        interval(9, 0, 10, 0)
                )
        );
    }

    @Test
    void intervalsOutsideWindowAreIgnored() {
        assertEquals(
                List.of(interval(8, 0, 12, 0)),
                find(
                        8, 12,
                        interval(6, 0, 7, 0),
                        interval(13, 0, 14, 0)
                )
        );
    }

    @Test
    void intervalCrossingWindowStartIsClipped() {
        assertEquals(
                List.of(interval(9, 0, 12, 0)),
                find(8, 12, interval(7, 0, 9, 0))
        );
    }

    @Test
    void intervalCrossingWindowEndIsClipped() {
        assertEquals(
                List.of(interval(8, 0, 11, 0)),
                find(8, 12, interval(11, 0, 13, 0))
        );
    }

    @Test
    void intervalsCoveringWholeWindowLeaveNoFreeTime() {
        assertTrue(
                find(
                        8, 12,
                        interval(7, 0, 9, 0),
                        interval(9, 0, 13, 0)
                ).isEmpty()
        );
    }

    @Test
    void equalWindowBoundariesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FreeSlotFinder.findFreeSlots(
                        at(8, 0), at(8, 0), List.of()
                )
        );
    }

    @Test
    void reversedIntervalIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FreeSlotFinder.Interval(at(10, 0), at(9, 0))
        );
    }
}
