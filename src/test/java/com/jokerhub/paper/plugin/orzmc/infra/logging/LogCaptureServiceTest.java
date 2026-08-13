package com.jokerhub.paper.plugin.orzmc.infra.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LogCaptureServiceTest {

    @Test
    void capture_singleLine_watermarkAndDrain() {
        LogCaptureService service = new LogCaptureService(10);
        long watermark = service.watermark();
        assertEquals(0, watermark);

        service.capture("hello");

        assertEquals(1, service.watermark());
        assertEquals(List.of("hello"), service.drainSince(watermark));
    }

    @Test
    void capture_multilineText_splitsIntoLines() {
        LogCaptureService service = new LogCaptureService(10);
        long watermark = service.watermark();

        service.capture("line1\nline2\nline3");

        assertEquals(List.of("line1", "line2", "line3"), service.drainSince(watermark));
    }

    @Test
    void capture_nullAndBlankLines_ignored() {
        LogCaptureService service = new LogCaptureService(10);
        long watermark = service.watermark();

        service.capture(null);
        service.capture("");
        service.capture("   ");
        service.capture("\n\n");

        assertEquals(List.of(), service.drainSince(watermark));
        assertEquals(0, service.watermark());
    }

    @Test
    void capture_crlfNormalized() {
        LogCaptureService service = new LogCaptureService(10);
        long watermark = service.watermark();

        service.capture("a\r\nb\rc");

        assertEquals(List.of("a", "b", "c"), service.drainSince(watermark));
    }

    @Test
    void capture_stripsAnsiColorCodes() {
        LogCaptureService service = new LogCaptureService(10);
        long watermark = service.watermark();

        service.capture("\u001B[33m黄色输出\u001B[0m plain");

        assertEquals(List.of("黄色输出 plain"), service.drainSince(watermark));
    }

    @Test
    void drainSince_onlyReturnsNewerThanWatermark() {
        LogCaptureService service = new LogCaptureService(10);
        service.capture("before");

        long watermark = service.watermark();
        service.capture("after1");
        service.capture("after2");

        assertEquals(List.of("after1", "after2"), service.drainSince(watermark));
    }

    @Test
    void drainSince_noNewLinesSinceNewWatermark_returnsEmpty() {
        LogCaptureService service = new LogCaptureService(10);
        service.capture("before");

        long watermark = service.watermark();
        service.capture("after");

        // drainSince 是幂等查询（按水位），取走后缓冲仍在；用新水位查询才为空
        assertEquals(List.of("after"), service.drainSince(watermark));
        assertEquals(List.of(), service.drainSince(service.watermark()));
    }

    @Test
    void capture_beyondCapacity_evictsOldest() {
        LogCaptureService service = new LogCaptureService(3);
        service.capture("a");
        service.capture("b");
        service.capture("c");
        service.capture("d");

        assertEquals(3, service.size());
        long watermark = service.watermark();
        // 最老的 a 已被挤出缓冲，drainSince(0) 只能拿到仍在缓冲里的行
        assertEquals(List.of("b", "c", "d"), service.drainSince(0));
        assertEquals(4, watermark);
    }

    @Test
    void constructor_negativeCapacity_throws() {
        assertThrows(IllegalArgumentException.class, () -> new LogCaptureService(0));
        assertThrows(IllegalArgumentException.class, () -> new LogCaptureService(-1));
    }

    @Test
    void capture_fromMultipleThreads_noDataLoss() throws Exception {
        LogCaptureService service = new LogCaptureService(1000);
        int threads = 4;
        int linesPerThread = 200;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            workers[t] = new Thread(() -> {
                for (int i = 0; i < linesPerThread; i++) {
                    service.capture("t" + threadId + "-" + i);
                }
            });
            workers[t].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        assertEquals(threads * linesPerThread, service.size());
        assertEquals(threads * linesPerThread, service.watermark());
        assertEquals(threads * linesPerThread, service.drainSince(0).size());
    }
}
