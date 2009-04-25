/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand2;

import junit.framework.TestCase;
import org.jmock.Mockery;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Aug-2008
 * Time: 15:20:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class CommandTest extends TestCase {

    Mockery mockery;
    TaskListener dummyTaskListener = new DebuggingTaskListener();
    volatile Thread lastTestExecutorThread;
    final StringBuffer failureText = new StringBuffer();
    final AtomicInteger counter = new AtomicInteger();

    public  final void setUp() {
        failureText.setLength(0);
        counter.set(0);

        //mockery and JMock are not thread safe.
        //the intention is to only ever touch the JMock classes from the event thread
        invokeAndWaitWithFail(
                new Runnable() {
                    public void run() {
                        mockery = new Mockery();
                    }
                }
        );
        doSetUp();
    }

    /**
     * Subclasses override to perform extra setup
     */
    protected void doSetUp() {
    }

    protected void invokeAndWaitWithFail(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (Exception e) {
            fail(e.getCause().toString());
        }
    }

    protected void validateMockeryAssertions() {
        invokeAndWaitWithFail(
                new Runnable() {
                    public void run() {
                        mockery.assertIsSatisfied();
                    }
                }
        );
    }

    protected void joinTestThread() {
        try {
            lastTestExecutorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void assertOrdering(int expectedValue, String testName) {
        int value = counter.incrementAndGet();
        if ( expectedValue != value) {
            failureText.append("Called out of order: ").append(testName).append(" expected ").append(expectedValue).append(" was ").append(value).append("  ");
        }
    }

    protected void checkOrderingFailureText() {
        if ( failureText.length() > 0) {
            fail(failureText.toString());
        }
    }


    class DummyBackgroundTask extends BackgroundTask {
        public void doInBackground() throws Exception {}

        public void doInEventThread() throws Exception {}

        public String toString() {
            return "Dummy BackgroundTask";
        }
    }


    class TestThreadExecutorCommand extends SwingCommand {
        private BackgroundTask asynchronousExecution;

        public TestThreadExecutorCommand(BackgroundTask singleExecutionForTesting) {
            super(new DefaultTestExecutor());
            this.asynchronousExecution = singleExecutionForTesting;
        }

        //n.b. you should create a new swingcommand execution for each invocation of this method
        //the instance passed to the constructor is used just for testing here, since we need a handle to the
        //execution instance for the tests
        public BackgroundTask createTask() {
            return asynchronousExecution;
        }
    }


    class DebuggingTaskListener implements TaskListener {

        public void pending(SwingTask task) {
            System.out.println("pending " + task);
        }

        public void started(SwingTask task) {
            System.out.println("started " + task);
        }

        public void progress(SwingTask task, String description) {
            System.out.println("progress "  + task);
        }

        public void finished(SwingTask task) {
            System.out.println("finished " + task);
        }

        public void success(SwingTask task) {
            System.out.println("success " + task);
        }

        public void error(SwingTask task, Throwable error) {
            System.out.println("error " + " " + task);
        }
    }

    class RuntimeExceptionThrowingTaskListener implements TaskListener {

        private String message = "This exception is expected. It is to verify that exceptions if observers do not interrupt the command processing workflow";

        public void pending(SwingTask task) {
            throw new TracelessRuntimeException(message);
        }

        public void started(SwingTask task) {
            throw new TracelessRuntimeException(message);
        }

        public void progress(SwingTask task, String description) {
            throw new TracelessRuntimeException(message);
        }

        public void finished(SwingTask task) {
            throw new TracelessRuntimeException(message);
        }

        public void success(SwingTask task) {
            throw new TracelessRuntimeException(message);
        }

        public void error(SwingTask task, Throwable error) {
            throw new TracelessRuntimeException(message);
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

    class DefaultTestExecutor implements Executor {

        public DefaultTestExecutor() {
        }

        public void execute(Runnable command) {
            lastTestExecutorThread = new Thread(command);
            lastTestExecutorThread.start();
        }
    }

    class SynchronousExecutor implements Executor {
        public void execute(Runnable command) {
            command.run();
        }
    }
}