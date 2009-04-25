/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand;

import junit.framework.TestCase;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Aug-2008
 * Time: 15:20:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractCommandTest extends TestCase {

    final StringBuffer failureText = new StringBuffer();
    final AtomicInteger counter = new AtomicInteger();
    protected boolean isBadListenerMethodCalled;
    protected String DO_IN_BACKGROUND_PROGRESS_TEXT = "doInBackground progress";
    protected final String DO_IN_EVENT_THREAD_PROGRESS_TEXT = "doInEventThread progress";
    protected CountDownLatch latch;
    protected RuntimeException testException;
    protected boolean isDoInEventThreadCalled;

    public final void setUp() {
        isDoInEventThreadCalled = false;
        testException = null;
        isBadListenerMethodCalled = false;
        failureText.setLength(0);
        counter.set(0);
        latch = new CountDownLatch(1);
        doSetUp();
    }

    /**
     * Subclasses override to perform extra setup
     */
    protected void doSetUp() {
    }

    protected void waitForLatch() {
        try {
            latch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            e.printStackTrace();
        }
    }

    protected void invokeAndWaitWithFail(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (Exception e) {
            fail(e.getCause().toString());
        }
    }

    protected synchronized void assertOrdering(int expectedValue, String testName) {
        int value = counter.incrementAndGet();
        if ( expectedValue != value) {
            failureText.append("Called out of order: ").append(testName).append(" expected ").append(expectedValue).append(" was ").append(value).append("  ");
        }
    }

    protected synchronized void assertInEventThread(String testName) {
        if ( ! SwingUtilities.isEventDispatchThread()) {
            failureText.append("Should be in event thread ").append(testName);
        }
    }

    protected synchronized void assertNotInEventThread(String testName) {
        if ( SwingUtilities.isEventDispatchThread()) {
            failureText.append("Should not be in event thread ").append(testName);
        }
    }

    protected synchronized void assertNotInThread(Thread t, String testName) {
        if ( Thread.currentThread() == t) {
            failureText.append("Should not be in thread ").append(testName);
        }
    }

    protected synchronized void assertInThread(Thread t, String testName) {
        if ( Thread.currentThread() != t) {
            failureText.append("Should be in thread ").append(testName);
        }
    }

    protected synchronized void checkOrderingFailureText() {
        if ( failureText.length() > 0) {
            fail(failureText.toString());
        }
    }


    class DummyBackgroundTask extends BackgroundTask<String> {
        public void doInBackground() throws Exception {}

        public void doInEventThread() throws Exception {}

        public String toString() {
            return "Dummy BackgroundTask";
        }
    }

    private static class TracelessRuntimeException extends RuntimeException {
        private TracelessRuntimeException(String message) {
            super(message);
        }

        public Throwable fillInStackTrace() {
            return this;
        }
    }

    class SynchronousExecutor implements Executor {
        public void execute(Runnable command) {
            command.run();
        }
    }

    class DelayedExecutor implements Executor {
        public void execute(Runnable command) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new Thread(command).start();
        }
    }

    abstract class ThreadCheckingTaskListener implements TaskListener<String> {

        private String message = "This exception is expected. " +
                "It is to verify that exceptions raised in listener callbacks do not interrupt the command processing workflow";

        public final void pending(Task commandExecution) {
            assertInEventThread("pending");
            doPending(commandExecution);
            throw new TracelessRuntimeException(message);
        }

        public abstract void doPending(Task commandExecution);

        public final void started(Task commandExecution) {
            assertInEventThread("started");
            doStarted(commandExecution);
            throw new TracelessRuntimeException(message);
        }

        public abstract void doStarted(Task commandExecution);

        public final void progress(Task commandExecution, String progressDescription) {
            assertInEventThread("progress");
            doProgress(commandExecution, progressDescription);
            throw new TracelessRuntimeException(message);
        }

        public abstract void doProgress(Task commandExecution, String progressDescription);


        public final void success(Task commandExecution) {
            assertInEventThread("success");
            doSuccess(commandExecution);
            throw new TracelessRuntimeException(message);
        }

        public abstract void doSuccess(Task commandExecution);


        public final void error(Task commandExecution, Throwable error) {
            assertInEventThread("error");
            doError(commandExecution, error);
            throw new TracelessRuntimeException(message);
        }

        public abstract void doError(Task commandExecution, Throwable error);


        public final void finished(Task commandExecution) {
            assertInEventThread("finished");
            doFinished(commandExecution);
            throw new TracelessRuntimeException(message);
        }

        public abstract void doFinished(Task commandExecution);

    }
}