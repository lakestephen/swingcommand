/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand2;

import junit.framework.Assert;

import javax.swing.*;
import java.util.concurrent.Executor;


/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 17-Aug-2008
 * Time: 18:08:30
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTask extends CommandTest {

    private RuntimeException testException;
    private boolean isDoInBackgroundCalledInSubThread;
    private boolean isDoneCalledInEventThread;
    private boolean isDoInEventThreadCalled;
    private boolean isBadListenerMethodCalled;
    private String DO_IN_BACKGROUND_PROGRESS_TEXT = "doInBackground progress";
    private final String DO_IN_EVENT_THREAD_PROGRESS_TEXT = "doInEventThread progress";

    public void doSetUp() {
        isDoneCalledInEventThread = false;
        isDoInEventThreadCalled = false;
        isDoInBackgroundCalledInSubThread = false;
        isBadListenerMethodCalled = false;
        testException = null;
    }

    //test the correct threads receive the callbacks
    public void testBackgroundTaskSuccessLifecycle() {

        final BackgroundTask task = new BackgroundTask() {

            public void doInBackground() throws Exception {
                assertOrdering(3, "doInBackground");
                isDoInBackgroundCalledInSubThread = ! SwingUtilities.isEventDispatchThread();
                Assert.assertEquals(ExecutionState.STARTED, getState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
            }

            public void doInEventThread() throws Exception {
                assertOrdering(5, "doInEventThread");
                isDoInEventThreadCalled = true;
                isDoneCalledInEventThread = SwingUtilities.isEventDispatchThread();
                Assert.assertEquals(ExecutionState.STARTED, getState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
            }
        };

        final TestThreadExecutorCommand dummyCommand = new TestThreadExecutorCommand(task) {
            public String toString() {
                return "testExecutionCallbacksNormalProcessing";
            }
        };

        dummyCommand.addTaskListener(new TaskListener() {

            public void pending(SwingTask commandExecution) {
                Assert.assertEquals(ExecutionState.PENDING, task.getState());
                assertOrdering(1, "pending");
            }

            public void started(SwingTask commandExecution) {
                Assert.assertEquals(ExecutionState.STARTED, task.getState());
                assertOrdering(2, "started");
            }

            public void progress(SwingTask commandExecution, String progressDescription) {
                if ( progressDescription.equals(DO_IN_BACKGROUND_PROGRESS_TEXT)) {
                    assertOrdering(4, DO_IN_BACKGROUND_PROGRESS_TEXT);
                } else {
                    assertOrdering(6, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                }
            }

            public void success(SwingTask commandExecution) {
                assertEquals(ExecutionState.SUCCESS, task.getState());
                assertOrdering(7, "success");
            }

            public void error(SwingTask commandExecution, Throwable error) {
                isBadListenerMethodCalled = true;
            }

            public void finished(SwingTask commandExecution) {
                assertEquals(ExecutionState.SUCCESS, task.getState());
                assertOrdering(8, "finished");
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getState());
        dummyCommand.execute();
        joinTestThread();
        assertOrdering(9, "end");

        assertEquals(ExecutionState.SUCCESS, task.getState());
        assertFalse(isBadListenerMethodCalled);
        assertTrue(isDoInBackgroundCalledInSubThread);
        assertTrue(isDoInEventThreadCalled);
        assertTrue(isDoneCalledInEventThread);
        checkOrderingFailureText();
    }

    public void testErrorInDoInBackground() {
        final BackgroundTask task = new BackgroundTask() {
            public void doInBackground() throws Exception {
                Assert.assertEquals(ExecutionState.STARTED, getState());
                assertOrdering(3, "doInBackground");
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
                testException = new RuntimeException("ErrorInDoInBackgroundExecution");
                throw testException;
            }

            public void doInEventThread() throws Exception {
                isDoInEventThreadCalled = true;
            }
        };

        final TestThreadExecutorCommand dummyCommand = new TestThreadExecutorCommand(task) {
            public String toString() {
                return "testErrorInDoInBackground";
            }
        };

        dummyCommand.addTaskListener(new TaskListener() {

            public void pending(SwingTask commandExecution) {
                Assert.assertEquals(ExecutionState.PENDING, task.getState());
                assertOrdering(1, "pending");
            }

            public void started(SwingTask commandExecution) {
                Assert.assertEquals(ExecutionState.STARTED, task.getState());
                assertOrdering(2, "started");
            }

            public void progress(SwingTask commandExecution, String progressDescription) {
                assertOrdering(4, DO_IN_BACKGROUND_PROGRESS_TEXT);
            }

            public void success(SwingTask commandExecution) {
                isBadListenerMethodCalled = true;
            }

            public void error(SwingTask commandExecution, Throwable error) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(5, "error");
            }

            public void finished(SwingTask commandExecution) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(6, "finished");
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getState());
        dummyCommand.execute();
        joinTestThread();
        assertOrdering(7, "end");

        assertEquals(ExecutionState.ERROR, task.getState());
        assertFalse(isDoInEventThreadCalled);
        assertFalse(isBadListenerMethodCalled);
        assertEquals(testException, task.getExecutionException());
        checkOrderingFailureText();
    }


    public void testErrorInDoInEventThread() {
        final BackgroundTask task = new BackgroundTask() {
            public void doInBackground() throws Exception {
                assertOrdering(3, "doInBackground");
                isDoInBackgroundCalledInSubThread = ! SwingUtilities.isEventDispatchThread();
                Assert.assertEquals(ExecutionState.STARTED, getState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
            }

            public void doInEventThread() throws Exception {
                assertOrdering(5, "doInEventThread");
                isDoInEventThreadCalled = true;
                isDoneCalledInEventThread = SwingUtilities.isEventDispatchThread();
                Assert.assertEquals(ExecutionState.STARTED, getState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                testException = new RuntimeException("ErrorInDoInEventThread");
                throw testException;
            }
        };



        final TestThreadExecutorCommand dummyCommand = new TestThreadExecutorCommand(task) {
            public String toString() {
                return "testErrorInDoInEventThread";
            }
        };

        dummyCommand.addTaskListener(new TaskListener() {

            public void pending(SwingTask commandExecution) {
                Assert.assertEquals(ExecutionState.PENDING, task.getState());
                assertOrdering(1, "pending");
            }

            public void started(SwingTask commandExecution) {
                Assert.assertEquals(ExecutionState.STARTED, task.getState());
                assertOrdering(2, "started");
            }

            public void progress(SwingTask commandExecution, String progressDescription) {
                if ( progressDescription.equals(DO_IN_BACKGROUND_PROGRESS_TEXT)) {
                    assertOrdering(4, DO_IN_BACKGROUND_PROGRESS_TEXT);
                } else {
                    assertOrdering(6, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                }
            }

            public void success(SwingTask commandExecution) {
                isBadListenerMethodCalled = true;
            }

            public void error(SwingTask commandExecution, Throwable error) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(7, "error");
            }

            public void finished(SwingTask commandExecution) {
                assertEquals(ExecutionState.ERROR, task.getState());
                assertOrdering(8, "finished");
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getState());
        dummyCommand.execute();

        joinTestThread();
        assertOrdering(9, "end");

        assertEquals(ExecutionState.ERROR, task.getState());
        assertFalse(isBadListenerMethodCalled);
        assertEquals(testException, task.getExecutionException());
        checkOrderingFailureText();
    }

    public void testPendingState() {
        DummyBackgroundTask task = new DummyBackgroundTask();

        final TestThreadExecutorCommand dummyCommand = new TestThreadExecutorCommand(task) {
            public String toString() {
                return "testPendingState";
            }
        };

        //just delay the start of the execution, simulate an executor with a queue delay, so we can test the pending state
        SwingTask t = dummyCommand.execute(new Executor() {
            public void execute(Runnable command) {
                try {
                    assertOrdering(1, "end");
                    Thread.sleep(1000);
                    assertOrdering(3, "end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new Thread(command).start();
            }
        });
        assertOrdering(2, "end");
        assertTrue(t == task);
        assertEquals(ExecutionState.PENDING, t.getState());
    }

}