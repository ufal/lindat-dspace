package org.dspace.core;

import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({Email.class, DSpace.class})
public class EmailTest{
    private ConfigurationService configurationService;

    @Before
    public void initTest() throws Exception{
        configurationService = new DSpaceConfigurationService();
        DSpace mockDSpace =  PowerMockito.spy(new DSpace());
        PowerMockito.doReturn(configurationService).when(mockDSpace, "getConfigurationService");
        PowerMockito.whenNew(DSpace.class).withNoArguments().thenReturn(mockDSpace);
    }

    @Test
    public void whenSendThenDoSendIsDelayed() throws Exception {
        long delay = 1_000;
        configurationService.setProperty("lr.email.delay", delay);
        int burst = 0;
        configurationService.setProperty("lr.email.burst", burst);
        Email.cleanup(burst);
        Email mock = PowerMockito.spy(new Email());
        PowerMockito.doNothing().when(mock, "doSend");

        mock.send();
        //if we get here then send() does not block; and if we pass past the verifyPrivate doSend has not been called yet
        PowerMockito.verifyPrivate(mock, times(0)).invoke("doSend");
        //Sleeping might make the test behave differently on different machines, but we have to wait somehow
        Thread.sleep(delay + delay/10);
        PowerMockito.verifyPrivate(mock, times(1)).invoke("doSend");
    }

    @Test
    public void whenSendThenARateLimitIsUsed() throws Exception{
        long delay = 1_000;
        configurationService.setProperty("lr.email.delay", delay);
        int burst = 0;
        configurationService.setProperty("lr.email.burst", burst);
        Email.cleanup(burst);
        Email mock = PowerMockito.spy(new Email());
        PowerMockito.doNothing().when(mock, "doSend");

        int emails = 10;
        for(int i = 0; i < emails; i++){
            mock.send();
        }

        Thread.sleep(delay * emails / 2);

        PowerMockito.verifyPrivate(mock, atLeast(emails / 3)).invoke("doSend");
        PowerMockito.verifyPrivate(mock, atMost(emails / 2)).invoke("doSend");

    }

    @Test
    public void whenSendThenDontBlockThread() throws Exception{
        long delay = 1_000;
        configurationService.setProperty("lr.email.delay", delay);
        int burst = 0;
        configurationService.setProperty("lr.email.burst", burst);
        Email.cleanup(burst);

        List<Callable> runnables = new LinkedList<>();
        final int emails = 80;
        for(int i = 0; i < emails; i++){
            runnables.add(new Callable() {
                @Override
                public Object call() throws Exception {
                    Email mock = PowerMockito.spy(new Email());
                    PowerMockito.doNothing().when(mock, "doSend");
                    long start = System.currentTimeMillis();
                    mock.send();
                    long diff = System.currentTimeMillis() - start;
                    System.out.println(diff);
                    // FIXME magic constant that might not work elsewhere
                    assertTrue("Email.send() blocked for more than " + 3 * emails + " ms",diff < 3 * emails);
                    return null;
                }
            });
        }

        // There's a delay of 1 s; if email.send was blocking; we'd encounter a timeout here
        assertConcurrent("Sending " + runnables.size() + " emails.", runnables, 1, TimeUnit.SECONDS);

    }

    @Test
    public void whenSendWithinBurstThenNoDelay() throws Exception {
        long delay = 100_000;
        configurationService.setProperty("lr.email.delay", delay);
        int burst = 10;
        configurationService.setProperty("lr.email.burst", burst);
        Email.cleanup(burst);

        Email mock = PowerMockito.spy(new Email());
        PowerMockito.doNothing().when(mock, "doSend");

        int emails = 9;
        for(int i = 0; i < emails; i++){
            mock.send();
        }

        Thread.sleep(1_000);
        PowerMockito.verifyPrivate(mock, times(emails)).invoke("doSend");
    }

    @Test
    public void whenSendOverBurstThenRateLimit() throws Exception {
        long delay = 500;
        configurationService.setProperty("lr.email.delay", delay);
        int burst = 10;
        configurationService.setProperty("lr.email.burst", burst);
        Email.cleanup(burst);

        Email mock = PowerMockito.spy(new Email());
        PowerMockito.doNothing().when(mock, "doSend");

        int emails = 15;
        for(int i = 0; i < emails; i++){
            mock.send();
        }

        Thread.sleep(6000);
        PowerMockito.verifyPrivate(mock, atLeast(burst)).invoke("doSend");
        PowerMockito.verifyPrivate(mock, atMost(12)).invoke("doSend");

        for(int i = 0; i < emails; i++){
            mock.send();
        }

        Thread.sleep(1500);
        // We should continue at the same rate
        PowerMockito.verifyPrivate(mock, times(emails)).invoke("doSend");
        Thread.sleep(delay * emails);
        PowerMockito.verifyPrivate(mock, times(2 * emails)).invoke("doSend");

        // The queue should be empty again
        for(int i = 0; i < emails; i++){
            mock.send();
        }

        Thread.sleep(6000);
        PowerMockito.verifyPrivate(mock, atLeast(2 * emails + burst)).invoke("doSend");
        PowerMockito.verifyPrivate(mock, atMost(2 * emails + 12)).invoke("doSend");
    }

    // slightly modified https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency
    public static void assertConcurrent(final String message, final List<? extends Callable> runnables, final long maxTimeout, TimeUnit unit) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Callable submittedTestRunnable : runnables) {
                threadPool.submit(new Runnable() {
                    public void run() {
                        allExecutorThreadsReady.countDown();
                        try {
                            afterInitBlocker.await();
                            submittedTestRunnable.call();
                        } catch (final Throwable e) {
                            exceptions.add(e);
                        } finally {
                            allDone.countDown();
                        }
                    }
                });
            }
            // wait until all threads are ready
            assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue(message +" timeout! More than " + maxTimeout + " " + unit.toString(), allDone.await(maxTimeout, unit));
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
    }

}