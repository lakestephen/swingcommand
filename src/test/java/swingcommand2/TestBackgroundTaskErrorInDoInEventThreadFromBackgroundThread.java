package swingcommand2;

import junit.framework.Assert;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:06:08
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTaskErrorInDoInEventThreadFromBackgroundThread extends CommandTest {

    public void testErrorInDoInEventThread() {

        final Thread startThread = Thread.currentThread();

        final BackgroundTask task = new BackgroundTask() {
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
                testException = new RuntimeException("ErrorInDoInEventThread");
                throw testException;
            }
        };

        final SwingCommand dummyCommand = new SwingCommand() {
            protected SimpleTask createTask() {
                assertInThread(startThread, "createTask in start thread");                
                assertOrdering(1, "createTask");
                return task;
            }

            public String toString() {
                return "testErrorInDoInEventThread";
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
                isBadListenerMethodCalled = true;
            }

            public void doError(SimpleTask commandExecution, Throwable error) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(8, "error");
            }

            public void doFinished(SimpleTask commandExecution) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(9, "finished");
                latch.countDown();
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getState());
        dummyCommand.execute();
        waitForLatch();
        assertOrdering(10, "end");

        assertEquals(ExecutionState.ERROR, task.getState());
        assertFalse(isBadListenerMethodCalled);
        assertEquals(testException, task.getExecutionException());
        checkOrderingFailureText();
    }
}
