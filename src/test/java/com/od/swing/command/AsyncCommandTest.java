package com.od.swing.command;

import junit.framework.TestCase;
import org.jmock.Mockery;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Aug-2008
 * Time: 15:20:04
 * To change this template use File | Settings | File Templates.
 */
public class AsyncCommandTest extends TestCase {

    Mockery mockery;
    LifeCycleMonitor debuggingLifeCycleMonitor = new DebuggingLifeCycleMonitor();

    public void setUp() {

        //mockery and JMock are not thread safe.
        //the intention is to only ever touch the JMock classes from the event thread
        invokeAndWaitWithFail(
                new Runnable() {
                    public void run() {
                        mockery = new Mockery();
                    }
                }
        );
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

    protected void joinCommandThread(AbstractAsynchronousCommand dummyCommand) {
        try {
            dummyCommand.getLastExecutingThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    class DummyExecution implements CommandExecution {
        public void doExecuteAsync() throws Exception {}

        public void doAfterExecute() throws Exception {}

        public String toString() {
            return "Dummy Execution";
        }
    }


    class DummyAsyncCommand extends AbstractAsynchronousCommand<CommandExecution> {
        private CommandExecution commandExecution;

        public DummyAsyncCommand(String name, CommandExecution singleExecutionForTesting) {
            super(name);
            this.commandExecution = singleExecutionForTesting;
        }

        //n.b. you should create a new command execution for each invocation of this method
        //the instance passed to the constructor is used just for testing here, since we need a handle to the
        //execution instance for the tests
        public CommandExecution createExecution() {
            return commandExecution;
        }
    }


    class DebuggingLifeCycleMonitor implements LifeCycleMonitor {

        public void started(String commandName, Object commandExecution) {
            System.out.println("started " + commandName + " " + commandExecution);
        }

        public void stepReached(String commandName, Object commandExecution) {
            System.out.println("stepReached "  + commandName + " " + commandExecution);
        }

        public void ended(String commandName, Object commandExecution) {
            System.out.println("started "  + commandName + " " + commandExecution);
        }

        public void error(String commandName, Object commandExecution, Throwable error) {
            System.out.println("started "  + commandName + " " + commandExecution + " " + error);
        }
    }

    class RuntimeExceptionThrowingLifecycleMonitor implements LifeCycleMonitor {

        public void started(String commandName, Object commandExecution) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }

        public void stepReached(String commandName, Object commandExecution) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }

        public void ended(String commandName, Object commandExecution) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }

        public void error(String commandName, Object commandExecution, Throwable error) {
            throw new RuntimeException("I shouldn't interrupt processing");
        }
    }
}
