package swingcommand2;

import junit.framework.Assert;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 15:25:02
 * To change this template use File | Settings | File Templates.
 */
public class TestSimpleTaskErrorInDoInEventThread extends CommandTest {

    private SimpleTask task;

    public void testSwingTaskFromBackgroundThread() {
        doTask();
        checkPostConditions();
    }

    public void testSwingTaskFromEventThread() {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    doTask();
                }
            }
        );
        checkPostConditions();
    }

    private void checkPostConditions() {
        waitForLatch();
        assertOrdering(8, "after execute");
        assertEquals(ExecutionState.ERROR, task.getExecutionState());
        assertFalse(isBadListenerMethodCalled);
        assertEquals(testException, task.getExecutionException());
        checkOrderingFailureText();
    }

    private SimpleTask doTask() {
        final Thread startThread = Thread.currentThread();

        task = new SimpleTask() {
            public void doInEventThread() throws Exception {
                assertOrdering(4, "doInEventThread");
                assertInEventThread("doInEventThread");
                assertEquals(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                testException = new RuntimeException("ErrorInDoInEventThread");
                throw testException;
            }
        };

        SwingCommand c = new SwingCommand() {
            protected SimpleTask createTask() {
                assertInThread(startThread, "createTask");
                assertOrdering(1, "createTask");
                return task;
            }
        };

        c.addTaskListener(new ThreadCheckingTaskListener() {

            public void doPending(SimpleTask commandExecution) {
                Assert.assertEquals(ExecutionState.PENDING, task.getExecutionState());
                assertOrdering(2, "pending");
            }

            public void doStarted(SimpleTask commandExecution) {
                Assert.assertEquals(ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(3, "started");
            }

            public void doProgress(SimpleTask commandExecution, String progressDescription) {
                Assert.assertEquals(ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(5, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
            }

            public void doSuccess(SimpleTask commandExecution) {
                isBadListenerMethodCalled = true;
            }

            public void doError(SimpleTask commandExecution, Throwable error) {
                assertEquals(ExecutionState.ERROR, task.getExecutionState());
                assertOrdering(6, "doError");
            }

            public void doFinished(SimpleTask commandExecution) {
                assertEquals(ExecutionState.ERROR, task.getExecutionState());
                assertOrdering(7, "finished");
                latch.countDown();
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getExecutionState());
        c.execute();
        return task;
    }
}
