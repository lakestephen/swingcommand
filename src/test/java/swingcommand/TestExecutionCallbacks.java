package swingcommand;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 17-Aug-2008
 * Time: 18:08:30
 * To change this template use File | Settings | File Templates.
 */
public class TestExecutionCallbacks extends AsyncCommandTest {

    private boolean isdoInBackgroundCalledInSubThread;
    private boolean isDoneCalledInEventThread;
    private boolean isDoneCalled;

    public void doSetUp() {
        isDoneCalledInEventThread = false;
        isDoneCalled = false;
        isdoInBackgroundCalledInSubThread = false;
    }

    //test the correct threads receive the callbacks
    public void testExecutionCallbacksNormalProcessing() {
        AsynchronousExecution dummyExecution = new NormalExecution();

        final DummyAsynchronousCommand dummyCommand = new DummyAsynchronousCommand(dummyExecution) {
            public String toString() {
                return "testExecutionCallbacksNormalProcessing";
            }
        };

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertTrue(isdoInBackgroundCalledInSubThread);
        assertTrue(isDoneCalled);
        assertTrue(isDoneCalledInEventThread);
    }

    public void testDoneShouldNotBeCalledIfExceptionThrownInDoInBackground() {

        AsynchronousExecution dummyExecution = new ErrorInDoInBackgroundExecution();

        final DummyAsynchronousCommand dummyCommand = new DummyAsynchronousCommand(dummyExecution) {
            public String toString() {
                return "testDoneShouldNotBeCalledIfExceptionThrownInDoInBackground";
            }
        };

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertFalse(isDoneCalled);
    }

    //executor map must be cleared down otherwise memory leak will occur
    public void testExecutorMapClearedAfterNormalExecution() {
        AsynchronousExecution dummyExecution = new NormalExecution();
        final DummyAsynchronousCommand dummyCommand = new DummyAsynchronousCommand(dummyExecution) {
            public String toString() {
                return "testExecutorMapClearedAfterNormalExecution";
            }
        };

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
        AsynchronousExecution dummyExecution = new ErrorInDoInBackgroundExecution();
        final DummyAsynchronousCommand dummyCommand = new DummyAsynchronousCommand(dummyExecution) {
            public String toString() {
                return "testExecutorMapClearedAfterExecutionWithError";
            }
        };

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

    private class NormalExecution implements AsynchronousExecution {

        public void doInBackground() throws Exception {
            isdoInBackgroundCalledInSubThread = ! SwingUtilities.isEventDispatchThread();
        }

        public void done() throws Exception {
            isDoneCalled = true;
            isDoneCalledInEventThread = SwingUtilities.isEventDispatchThread();
        }
    }

    private class ErrorInDoInBackgroundExecution implements AsynchronousExecution {

        public void doInBackground() throws Exception {
            throw new RuntimeException("ErrorInDoInBackgroundExecution");
        }

        public void done() throws Exception {
            isDoneCalled = true;
        }
    }
}
