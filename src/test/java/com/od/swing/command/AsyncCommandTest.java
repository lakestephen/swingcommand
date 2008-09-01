package com.od.swing.command;

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
public abstract class AsyncCommandTest extends TestCase {

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


    class DummyExecution implements AsynchronousExecution {
        public void doInBackground() throws Exception {}

        public void done() throws Exception {}

        public String toString() {
            return "Dummy Execution";
        }
    }


    class DummyAsynchronousCommand extends AbstractAsynchronousCommand<AsynchronousExecution> {
        private AsynchronousExecution asynchronousExecution;

        public DummyAsynchronousCommand(AsynchronousExecution singleExecutionForTesting) {
            super(new DefaultTestExecutor());
            this.asynchronousExecution = singleExecutionForTesting;
        }

        //n.b. you should create a new command execution for each invocation of this method
        //the instance passed to the constructor is used just for testing here, since we need a handle to the
        //execution instance for the tests
        public AsynchronousExecution createExecution() {
            return asynchronousExecution;
        }
    }


    class DebuggingExecutionObserver implements ExecutionObserver {

        public void starting(Object commandExecution) {
            System.out.println("starting " + commandExecution);
        }

        public void started(Object commandExecution) {
            System.out.println("started " + commandExecution);
        }

        public void progress(Object commandExecution) {
            System.out.println("progress "  + commandExecution);
        }

        public void stopped(Object commandExecution) {
            System.out.println("started " + commandExecution);
        }

        public void error(Object commandExecution, Throwable error) {
            System.out.println("started " + " " + commandExecution + " " + error);
        }
    }

    class RuntimeExceptionThrowingExecutionObserver implements ExecutionObserver {

        public void starting(Object commandExecution) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }

        public void started(Object commandExecution) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }

        public void progress(Object commandExecution) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }

        public void stopped(Object commandExecution) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }

        public void error(Object commandExecution, Throwable error) {
            throw new RuntimeException("I shouldn't interrupt processing");
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
