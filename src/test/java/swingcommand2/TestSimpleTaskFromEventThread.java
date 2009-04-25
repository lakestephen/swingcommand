package swingcommand2;

import junit.framework.Assert;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:17:54
 * To change this template use File | Settings | File Templates.
 */
public class TestSimpleTaskFromEventThread extends CommandTest {

    public void testSwingTaskFromEventThread() {
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {

                    final Thread startThread = Thread.currentThread();

                    final SimpleTask task = new SimpleTask() {
                        public void doInEventThread() throws Exception {
                            assertOrdering(4, "doInEventThread");
                            assertInEventThread("doInEventThread");
                            assertEquals(ExecutionState.STARTED, getState());
                            fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
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
                            Assert.assertEquals(ExecutionState.PENDING, task.getState());
                            assertOrdering(2, "pending");
                        }

                        public void doStarted(SimpleTask commandExecution) {
                            Assert.assertEquals(ExecutionState.STARTED, task.getState());
                            assertOrdering(3, "started");
                        }

                        public void doProgress(SimpleTask commandExecution, String progressDescription) {
                            Assert.assertEquals(ExecutionState.STARTED, task.getState());
                            assertOrdering(5, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                        }

                        public void doSuccess(SimpleTask commandExecution) {
                            assertEquals(ExecutionState.SUCCESS, task.getState());
                            assertOrdering(6, "success");
                        }

                        public void doError(SimpleTask commandExecution, Throwable error) {
                            isBadListenerMethodCalled = true;
                        }

                        public void doFinished(SimpleTask commandExecution) {
                            assertEquals(ExecutionState.SUCCESS, task.getState());
                            assertOrdering(7, "finished");
                        }
                    });

                    assertEquals(ExecutionState.NOT_RUN, task.getState());
                    c.execute();
                    assertOrdering(8, "after execute");
                    assertEquals(ExecutionState.SUCCESS, task.getState());
                }
            }
        );
        checkOrderingFailureText();
    }
}
