package de.haw.misc.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;

@Slf4j
public class MemoryUtils {

    public static void logMemoryStats() {
        Runtime runtime = Runtime.getRuntime();

        // Heap memory stats from Runtime
        long maxHeapSize = runtime.maxMemory();
        long allocatedHeapSize = runtime.totalMemory();
        long freeHeapSize = runtime.freeMemory();
        long usedHeapSize = allocatedHeapSize - freeHeapSize;

        // JMX MemoryMXBean for detailed heap and non-heap memory stats
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        // Thread stack memory stats (approximation)
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadMXBean.getThreadCount();
        long approximateStackMemory = threadCount * ( 512 * 1024 );

        // Logging all memory stats
        log.info( "==== Memory Statistics ====" );

        log.info( "Heap Memory Stats:" );
        log.info( "  Max Heap Size: {} MB", ( maxHeapSize / ( 1024 * 1024 ) ) );
        log.info( "  Allocated Heap Size: {} MB", ( allocatedHeapSize / ( 1024 * 1024 ) ) );
        log.info( "  Free Heap Size in Allocated: {} MB", ( freeHeapSize / ( 1024 * 1024 ) ) );
        log.info( "  Used Heap Size: {} MB", ( usedHeapSize / ( 1024 * 1024 ) ) );

        log.info( "Non-Heap Memory Stats:" );
        log.info( "  Max Non-Heap Size: {} MB", ( nonHeapMemoryUsage.getMax() / ( 1024 * 1024 ) ) );
        log.info( "  Used Non-Heap Size: {} MB", ( nonHeapMemoryUsage.getUsed() / ( 1024 * 1024 ) ) );

        log.info( "Stack Memory Stats:" );
        log.info( "  Thread Count: {}", threadCount );
        log.info( "  Approximate Stack Memory Usage: {} MB", ( approximateStackMemory / ( 1024 * 1024 ) ) );

        log.info( "===========================" );
    }

}
