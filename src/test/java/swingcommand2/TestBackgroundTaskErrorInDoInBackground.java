package swingcommand2;

import junit.framework.Assert;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:05:02
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTaskErrorInDoInBackground extends CommandTest {

    private BackgroundTask task;

    public void testBackgroundTaskErrorInBackgroundFromBackgroundThread() {
        doTest();
        checkEndStates();
    }

    public void testBackgroundTaskErrorInBackgroundFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doTest();
            }
        });
        checkEndStates();
    }

    private void checkEndStates() {
        waitForLatch();
        assertOrdering(8, "end");

        assertEquals(ExecutionState.ERROR, task.getState());
        assertFalse(isDoInEventThreadCalled);
        assertFalse(isBadListenerMethodCalled);
        assertEquals(testException, task.getExecutionException());
        checkOrderingFailureText();
    }

    private void doTest() {
        final Thread startThread = Thread.currentThread();

        task = new BackgroundTask() {
            public void doInBackground() throws Exception {
                assertNotInThread(startThread, "doInBackgroundNotInStartThread");
                assertNotInEventThread("doInBackground");
                Assert.assertEquals(ExecutionState.STARTED, getState());
                assertOrdering(4, "doInBackground");
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
                testException = new RuntimeException("ErrorInDoInBackgroundExecution");
                throw testException;
            }

            public void doInEventThread() throws Exception {
                isDoInEventThreadCalled = true;
            }
        };

        final SwingCommand dummyCommand = new SwingCommand() {
            protected SimpleTask createTask() {
                assertInThread(startThread, "createTask in start thread");
                assertOrdering(1, "createTask");
                return task;
            }

            public String toString() {
                return "testErrorInDoInBackground";
            }
        };

        dummyCommand.addTaskListener(new ThreadCheckingTaskListener() {

            public void doPending(SimpleTask commandExecution) {
                Assert.assertEquals(ExecutionState.PENDING, task.getState());
                assertOrdering(2, "pending");
            }

            public void doStarted(SimpleTask commandExecution) {
                Assert.assertEquals(ExecutionState.STARTED, task.getState());
                assertOrdering(3, "started");
            }

            public void doProgress(SimpleTask commandExecution, String progressDescription) {
                assertOrdering(5, DO_IN_BACKGROUND_PROGRESS_TEXT);
            }

            public void doSuccess(SimpleTask commandExecution) {
                isBadListenerMethodCalled = true;
            }

            public void doError(SimpleTask commandExecution, Throwable error) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(6, "error");
            }

            public void doFinished(SimpleTask commandExecution) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(7, "finished");
                latch.countDown();
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getState());
        dummyCommand.execute();
    }
}
