package com.alirabiee.data.statistics;

import com.alirabiee.apistats.data.Transaction;
import com.alirabiee.apistats.data.statistics.ExpiredTransactionException;
import com.alirabiee.apistats.data.statistics.FutureStampedTransactionException;
import com.alirabiee.apistats.data.statistics.StatisticsImpl;
import com.alirabiee.apistats.data.statistics.StatisticsReport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StatisticsImpl.class)
public class StatisticsImplTest {
    @Autowired
    StatisticsImpl statistics;

    @Value("${history.max-seconds}")
    Integer maxSeconds;

    @Test
    public void testGetReport() throws InterruptedException {
        final int n = 10000;
        final Collection<Callable<Void>> tasks = new ArrayList<>();
        final ExecutorService pool = Executors.newFixedThreadPool(n);
        final Callable<Void> task = () -> {
            statistics.add(new Transaction(10.001, (long) (System.currentTimeMillis() - Math.random()*maxSeconds)));
            return null;
        };

        for (int i = 0; i < n; i++) {
            tasks.add(task);
        }

        statistics.clear();
        pool.invokeAll(tasks);
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        final StatisticsReport report = statistics.getReport();

        Assert.assertEquals("Sum is correct", 100010D, report.getSum(), 0.0001);
        Assert.assertEquals("Average is correct", 10.001D, report.getAvg(), 0.0001);
        Assert.assertEquals("Min is correct", 10.001D, report.getMin(), 0.0001);
        Assert.assertEquals("Max is correct", 10.001D, report.getMax(), 0.0001);
        Assert.assertEquals("Count is correct", n, report.getCount());
    }

    @Test
    public void testClear() throws InterruptedException {
        final int n = 3000;
        final Collection<Callable<Void>> tasks = new ArrayList<>();
        final ExecutorService pool = Executors.newFixedThreadPool(n);
        final Callable<Void> task = () -> {
            statistics.add(new Transaction(10.001, (long) (System.currentTimeMillis() - Math.random()*maxSeconds)));
            statistics.clear();
            return null;
        };

        for (int i = 0; i < n; i++) {
            tasks.add(task);
        }

        statistics.clear();
        pool.invokeAll(tasks);
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        final StatisticsReport report = statistics.getReport();

        Assert.assertEquals("Sum is correct", 0D, report.getSum(), 0.0001);
        Assert.assertEquals("Average is correct", 0D, report.getAvg(), 0.0001);
        Assert.assertEquals("Count is correct", 0, report.getCount());
    }

    @Test(expected = ExpiredTransactionException.class)
    public void testExpiredTransactionException() throws ExpiredTransactionException, FutureStampedTransactionException {
        statistics.add(new Transaction(100D, 100L));
    }

    @Test
    public void testMoveTimeline() throws ExpiredTransactionException, FutureStampedTransactionException {
        statistics.clear();
        statistics.add(new Transaction(100D, System.currentTimeMillis() - 59000));
        statistics.moveTimeline();

        final StatisticsReport report = statistics.getReport();

        Assert.assertEquals("Sum is correct", 0D, report.getSum(), 0.0001);
        Assert.assertEquals("Average is correct", 0D, report.getAvg(), 0.0001);
        Assert.assertEquals("Count is correct", 0, report.getCount());
    }
}
