/*
 * Copyright 2009 Object Definitions Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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

    protected void assertExpectedState(Task.ExecutionState state, Task.ExecutionState expectedState) {
        if ( state != expectedState) {
            failureText.append("Expected state ").append(expectedState).append(" but was ").append(state);
        }
    }

    protected void assertIsTrue(boolean val, String description) {
        if ( ! val) {
            failureText.append(description);
        }
    }

    protected synchronized void checkFailureText() {
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

    //all the tests should still pass if you uncomment throw new TracelessRuntimeException, but the stack traces get annoying!
    abstract class ThreadCheckingTaskListener implements TaskListener<String> {

        private String message = "This exception is expected. " +
                "It is to verify that exceptions raised in listener callbacks do not interrupt the command processing workflow";

        public final void pending(Task task) {
            assertInEventThread("pending");
            doPending(task);
            //throw new TracelessRuntimeException(message);
        }

        public abstract void doPending(Task task);

        public final void started(Task task) {
            assertInEventThread("started");
            doStarted(task);
            //throw new TracelessRuntimeException(message);
        }

        public abstract void doStarted(Task task);

        public final void progress(Task task, String progressDescription) {
            assertInEventThread("progress");
            doProgress(task, progressDescription);
            //throw new TracelessRuntimeException(message);
        }

        public abstract void doProgress(Task task, String progressDescription);


        public final void success(Task task) {
            assertInEventThread("success");
            doSuccess(task);
            //throw new TracelessRuntimeException(message);
        }

        public abstract void doSuccess(Task task);


        public final void error(Task task, Throwable error) {
            assertInEventThread("error");
            doError(task, error);
            //throw new TracelessRuntimeException(message);
        }

        public abstract void doError(Task task, Throwable error);


        public final void finished(Task task) {
            assertInEventThread("finished");
            doFinished(task);
            //throw new TracelessRuntimeException(message);
        }

        public abstract void doFinished(Task task);

    }

    public static class DefaultCompositeCommandTask extends CompositeCommandTask<String>{

        protected String getProgress(int currentCommandId, int totalCommands, Task currentChildCommand) {
            return currentChildCommand.toString();
        }
    }
}