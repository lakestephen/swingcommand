package com.od.swing.command;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 17-Aug-2008
 * Time: 18:08:30
 * To change this template use File | Settings | File Templates.
 */
public class TestExecutionCallbacks extends AsyncCommandTest {

    private boolean isDoExecuteAsyncCalledInSubThread;
    private boolean isDoAfterExecuteCalledInEventThread;
    private boolean isDoAfterExecutedCalled;

    public void doSetUp() {
        isDoAfterExecuteCalledInEventThread = false;
        isDoAfterExecutedCalled = false;
        isDoExecuteAsyncCalledInSubThread = false;
    }

    //test the correct threads receive the callbacks
    public void testExecutionCallbacksNormalProcessing() {
        CommandExecution dummyExecution = new NormalExecution();

        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand("testExecutionCallbacksNormalProcessing", dummyExecution);
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertTrue(isDoExecuteAsyncCalledInSubThread);
        assertTrue(isDoAfterExecutedCalled);
        assertTrue(isDoAfterExecuteCalledInEventThread);
    }

    public void testDoAfterExecuteShouldNotBeCalledIfExceptionThrownInDoExecuteAsync() {

        CommandExecution dummyExecution = new ErrorInExecAsyncExecution();

        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand("testDoAfterExecuteShouldNotBeCalledIfExceptionThrownInDoExecuteAsync", dummyExecution);
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertFalse(isDoAfterExecutedCalled);
    }

    //executor map must be cleared down otherwise memory leak will occur
    public void testExecutorMapClearedAfterNormalExecution() {
        CommandExecution dummyExecution = new NormalExecution();
        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand("testDoAfterExecuteShouldNotBeCalledIfExceptionThrownInDoExecuteAsync", dummyExecution);
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertEquals(0, dummyCommand.executionToExecutorMap.size());
    }

    //executor map must be cleared down otherwise memory leak will occur
    public void testExecutorMapClearedAfterExecutionWithError() {
        CommandExecution dummyExecution = new ErrorInExecAsyncExecution();
        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand("testDoAfterExecuteShouldNotBeCalledIfExceptionThrownInDoExecuteAsync", dummyExecution);
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertEquals(0, dummyCommand.executionToExecutorMap.size());
    }

    private class NormalExecution implements CommandExecution {

        public void doExecuteAsync() throws Exception {
            isDoExecuteAsyncCalledInSubThread = ! SwingUtilities.isEventDispatchThread();
        }

        public void doAfterExecute() throws Exception {
            isDoAfterExecutedCalled = true;
            isDoAfterExecuteCalledInEventThread = SwingUtilities.isEventDispatchThread();
        }
    }

    private class ErrorInExecAsyncExecution implements CommandExecution {

        public void doExecuteAsync() throws Exception {
            throw new RuntimeException("ErrorInExecAsyncExecution");
        }

        public void doAfterExecute() throws Exception {
            isDoAfterExecutedCalled = true;
        }
    }
}
