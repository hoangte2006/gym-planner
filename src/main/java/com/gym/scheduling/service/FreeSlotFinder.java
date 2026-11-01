
package com.gym.scheduling.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Tim cac khoang thoi gian trong con trong sau khi loai bo
 * cac khoang thoi gian da ban.
 */
public final class FreeSlotFinder {

    private FreeSlotFinder() {
        // Utility class: khong tao instance.
    }

    /**
     * Mot khoang thoi gian [start, end).
     * start phai nho hon end.
     */
    public record Interval(LocalDateTime start, LocalDateTime end) {

        public Interval {
            Objects.requireNonNull(start, "start khong duoc null");
            Objects.requireNonNull(end, "end khong duoc null");

            if (!start.isBefore(end)) {
                throw new IllegalArgumentException(
                        "Thoi diem bat dau phai truoc thoi diem ket thuc"
                );
            }
        }
    }

    /**
     * Tim cac khoang trong trong [rangeStart, rangeEnd),
     * sau khi loai bo cac khoang ban.
     */
    public static List<Interval> findFreeSlots(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Collection<Interval> busyIntervals) {

        Objects.requireNonNull(rangeStart, "rangeStart khong duoc null");
        Objects.requireNonNull(rangeEnd, "rangeEnd khong duoc null");
        Objects.requireNonNull(
                busyIntervals, "busyIntervals khong duoc null"
        );

        if (!rangeStart.isBefore(rangeEnd)) {
            throw new IllegalArgumentException(
                    "Khoang can tim phai co thoi luong duong"
            );
        }

        List<Interval> clipped = new ArrayList<>();

        // Chi giu phan giao nhau giua khoang ban va khung gio tim kiem.
        for (Interval busy : busyIntervals) {
            Objects.requireNonNull(
                    busy, "busyIntervals khong duoc chua null"
            );

            if (!busy.start().isBefore(rangeEnd)
                    || !rangeStart.isBefore(busy.end())) {
                continue;
            }

            LocalDateTime start = busy.start().isBefore(rangeStart)
                    ? rangeStart : busy.start();

            LocalDateTime end = busy.end().isAfter(rangeEnd)
                    ? rangeEnd : busy.end();

            clipped.add(new Interval(start, end));
        }

        // Sap xep theo thoi diem bat dau, roi den thoi diem ket thuc.
        clipped.sort(
                Comparator.comparing(Interval::start)
                        .thenComparing(Interval::end)
        );

        // Gop cac khoang ban chong lan hoac noi tiep nhau.
        List<Interval> merged = new ArrayList<>();

        for (Interval current : clipped) {
            if (merged.isEmpty()) {
                merged.add(current);
                continue;
            }

            Interval previous = merged.get(merged.size() - 1);

            if (!current.start().isAfter(previous.end())) {
                LocalDateTime mergedEnd =
                        current.end().isAfter(previous.end())
                                ? current.end() : previous.end();

                merged.set(
                        merged.size() - 1,
                        new Interval(previous.start(), mergedEnd)
                );
            } else {
                merged.add(current);
            }
        }

        // Cac khoang nam giua khung gio va cac khoang ban la khoang trong.
        List<Interval> freeSlots = new ArrayList<>();
        LocalDateTime cursor = rangeStart;

        for (Interval busy : merged) {
            if (cursor.isBefore(busy.start())) {
                freeSlots.add(new Interval(cursor, busy.start()));
            }

            if (busy.end().isAfter(cursor)) {
                cursor = busy.end();
            }
        }

        if (cursor.isBefore(rangeEnd)) {
            freeSlots.add(new Interval(cursor, rangeEnd));
        }

        return List.copyOf(freeSlots);
    }
}
