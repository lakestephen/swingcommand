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

    public void testExecutionCallbacksNormalProcessing() {        
        final CommandExecution dummyExecution = new CommandExecution() {

            public void doExecuteAsync() throws Exception {
                isDoExecuteAsyncCalledInSubThread = ! SwingUtilities.isEventDispatchThread();
            }

            public void doAfterExecute() throws Exception {
                isDoAfterExecutedCalled = true;
                isDoAfterExecuteCalledInEventThread = SwingUtilities.isEventDispatchThread();
            }
        };

        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand("testExecutionCallbacksNormalProcessing", dummyExecution);
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinCommandThread(dummyCommand);
        assertTrue(isDoExecuteAsyncCalledInSubThread);
        assertTrue(isDoAfterExecutedCalled);
        assertTrue(isDoAfterExecuteCalledInEventThread);
    }

     public void testDoAfterExecuteShouldNotBeCalledIfExceptionThrownInDoExecuteAsync() {
        final CommandExecution dummyExecution = new CommandExecution() {

            public void doExecuteAsync() throws Exception {
                throw new RuntimeException("testDoAfterExecuteShouldNotBeCalledIfExceptionThrownInDoExecuteAsync");
            }

            public void doAfterExecute() throws Exception {
                isDoAfterExecutedCalled = true;
            }
        };

        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand("testDoAfterExecuteShouldNotBeCalledIfExceptionThrownInDoExecuteAsync", dummyExecution);
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinCommandThread(dummyCommand);
        assertFalse(isDoAfterExecutedCalled);
    }
}
