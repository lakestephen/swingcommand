package swingcommand2;

import junit.framework.Assert;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:02:40
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTask extends CommandTest {

    private BackgroundTask task;

    public void testBackgroundTaskFromBackgroundThread() {
        doTest();
        checkEndStates();
    }

    public void testBackgroundTaskFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doTest();
            }
        });
        checkEndStates();
    }

    private void checkEndStates() {
        waitForLatch();
        assertOrdering(10, "end");

        assertEquals(ExecutionState.SUCCESS, task.getState());
        assertFalse(isBadListenerMethodCalled);
        checkOrderingFailureText();
    }

    private void doTest() {
        final Thread startThread = Thread.currentThread();

        task = new BackgroundTask() {

            public void doInBackground() throws Exception {
                assertNotInThread(startThread, "doInBackground");
                assertNotInEventThread("doInBackground");
                assertOrdering(4, "doInBackground");
                Assert.assertEquals(ExecutionState.STARTED, getState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
            }

            public void doInEventThread() throws Exception {
                assertInEventThread("doInEventThread");
                assertOrdering(6, "doInEventThread");
                Assert.assertEquals(ExecutionState.STARTED, getState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
            }
        };

        final SwingCommand dummyCommand = new SwingCommand() {
            protected SimpleTask createTask() {
                assertOrdering(1, "createTask");
                return task;
            }

            public String toString() {
                return "testExecutionCallbacksNormalProcessing";
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
                if ( progressDescription.equals(DO_IN_BACKGROUND_PROGRESS_TEXT)) {
                    assertOrdering(5, DO_IN_BACKGROUND_PROGRESS_TEXT);
                } else {
                    assertOrdering(7, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                }
            }

            public void doSuccess(SimpleTask commandExecution) {
                assertEquals(ExecutionState.SUCCESS, task.getState());
                assertOrdering(8, "success");
            }

            public void doError(SimpleTask commandExecution, Throwable error) {
                isBadListenerMethodCalled = true;
            }

            public void doFinished(SimpleTask commandExecution) {
                assertEquals(ExecutionState.SUCCESS, task.getState());
                assertOrdering(9, "finished");
                latch.countDown();
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getState());
        dummyCommand.execute();
    }
}
