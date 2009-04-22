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

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Aug-2008
 * Time: 15:20:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class CommandTest extends TestCase {

    Mockery mockery;
    ExecutionObserver debuggingExecutionObserver = new DebuggingExecutionObserver();
    volatile Thread lastTestExecutorThread;

    public  final void setUp() {

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

    protected void joinLastExecutorThread() {
        try {
            lastTestExecutorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    class DummyExecution extends BackgroundExecution {
        public void doInBackground() throws Exception {}

        public void doInEventThread() throws Exception {}

        public String toString() {
            return "Dummy Execution";
        }
    }


    class DummyAsynchronousCommand extends SwingCommand {
        private AsyncExecution asynchronousExecution;

        public DummyAsynchronousCommand(AsyncExecution singleExecutionForTesting) {
            super(new DefaultTestExecutor());
            this.asynchronousExecution = singleExecutionForTesting;
        }

        //n.b. you should create a new swingcommand execution for each invocation of this method
        //the instance passed to the constructor is used just for testing here, since we need a handle to the
        //execution instance for the tests
        public AsyncExecution createExecution() {
            return asynchronousExecution;
        }
    }


    class DebuggingExecutionObserver implements ExecutionObserver {

        public void pending(Execution commandExecution) {
            System.out.println("pending " + commandExecution);
        }

        public void started(Execution commandExecution) {
            System.out.println("started " + commandExecution);
        }

        public void progress(Execution commandExecution, String description) {
            System.out.println("progress "  + commandExecution);
        }

        public void done(Execution commandExecution) {
            System.out.println("done " + commandExecution);
        }

        public void success(Execution commandExecution) {
            System.out.println("success " + commandExecution);
        }

        public void error(Execution commandExecution, Throwable error) {
            System.out.println("error " + " " + commandExecution);
        }
    }

    class RuntimeExceptionThrowingExecutionObserver implements ExecutionObserver {

        private String message = "This execption is expected. It is to verify that exceptions if observers do not interrupt the command processing workflow";

        public void pending(Execution commandExecution) {
            throw new TracelessRuntimeException(message);
        }

        public void started(Execution commandExecution) {
            throw new TracelessRuntimeException(message);
        }

        public void progress(Execution commandExecution, String description) {
            throw new TracelessRuntimeException(message);
        }

        public void done(Execution commandExecution) {
            throw new TracelessRuntimeException(message);
        }

        public void success(Execution commandExecution) {
            throw new TracelessRuntimeException(message);
        }

        public void error(Execution commandExecution, Throwable error) {
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
}