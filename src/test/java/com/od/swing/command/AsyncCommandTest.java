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
public abstract class AsyncCommandTest extends TestCase {

    Mockery mockery;
    LifeCycleMonitor debuggingLifeCycleMonitor = new DebuggingLifeCycleMonitor();

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

    class DebuggingProxyCommandController<E> implements CommandController<E> {
        private CommandController<E> wrappedController;

        public DebuggingProxyCommandController(CommandController<E> wrappedController) {
            this.wrappedController = wrappedController;
        }

        public void commandStarting(String commandName, E commandExecution) throws Exception {
            System.out.println("commandStarting "  + commandName + " " + commandExecution);
            wrappedController.commandStarting(commandName, commandExecution);
        }

        public void commandStopped(String commandName, E commandExecution) {
            System.out.println("commandStopped "  + commandName + " " + commandExecution);
            wrappedController.commandStopped(commandName, commandExecution);  
        }

        public void commandError(String commandName, E commandExecution, Throwable t) {
            System.out.println("commandError "  + commandName + " " + commandExecution + " " + t);
            wrappedController.commandError(commandName, commandExecution, t);
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
