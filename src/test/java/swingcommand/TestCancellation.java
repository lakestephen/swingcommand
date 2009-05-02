/*
 * Copyright 2009 Object Definitions Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package swingcommand;

import javax.swing.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 27-Apr-2009
 * Time: 22:41:37
 * To change this template use File | Settings | File Templates.
 */
public class TestCancellation extends AbstractCommandTest {

    private BackgroundTask task;

    public void testSleepCancellationFromBackgroundThread() {
        doCancellationTest(new SleepBlockingRunnable());
        checkEndStates(true);
    }

    public void testSleepCancellationFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doCancellationTest(new SleepBlockingRunnable());
            }
        });
        checkEndStates(true);
    }

    public void testActiveCancellationFromBackgroundThread() {
        doCancellationTest(new SleepBlockingRunnable());
        checkEndStates(true);
    }

    public void testActiveCancellationFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doCancellationTest(new SleepBlockingRunnable());
            }
        });
        checkEndStates(true);
    }

    public void testSleepNonCancellationFromBackgroundThread() {
        doNoCancellationTest(new SleepBlockingRunnable());
        checkEndStates(false);
    }

    public void testSleepNonCancellationFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doNoCancellationTest(new SleepBlockingRunnable());
            }
        });
        checkEndStates(false);
    }

    public void testActiveNonCancellationFromBackgroundThread() {
        doNoCancellationTest(new SleepBlockingRunnable());
        checkEndStates(false);
    }

    public void testActiveNonCancellationFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doNoCancellationTest(new SleepBlockingRunnable());
            }
        });
        checkEndStates(false);
    }

    public void testCompositeCancelFromBackgroundThread() {
        compositeCancel();
        checkEndStates(true);
    }

    public void testCompositeCancelFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                compositeCancel();
            }
        });
        checkEndStates(true);
    }

    public void testCompositeChildCancelFromBackgroundThread() {
        compositeCancelOfChildCommand();
        checkEndStates(true);
    }

    public void testCompositeChildCancelFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                compositeCancelOfChildCommand();
            }
        });
        checkEndStates(true);
    }

    public void testCancelWithFailureFromBackgroundThread() {
        doCancelledTaskFailsIfCancelledNotCalledAndExceptionThrown();
        checkEndStates(false);
    }

    public void testCancelWithFailureFromEventThread() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doCancelledTaskFailsIfCancelledNotCalledAndExceptionThrown();
            }
        });
        checkEndStates(false);
    }



    private void checkEndStates(boolean expectCancel) {
        waitForLatch();
        if ( expectCancel) {
            assertTrue(" is cancelled", task.isCancelled());
        } else {
            assertFalse(" is cancelled", task.isCancelled());
        }
        assertFalse(isBadListenerMethodCalled);
        checkFailureText();
    }

    public void compositeCancel() {
        final CompositeCommandTask<Object,String> t = doCompositeCommand();

        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
        s.schedule(new Runnable() {
            public void run() {
                t.cancel();
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    private CompositeCommandTask<Object,String> doCompositeCommand() {
        final CompositeCommandTask<Object,String> t = new CompositeCommandTask<Object,String>() {
            protected String getProgress(int currentCommandId, int totalCommands, Task currentChildCommand) {
                return currentChildCommand.toString();
            }
        };

        task = t;
        t.addCommand(new SwingCommand() {
            protected Task createTask() {
                return new InterruptibleTask() {
                    protected void doInBackground() throws Exception {
                        new SleepBlockingRunnable().run();
                    }

                    protected void doInEventThreadIfNotCancelled() {
                    }

                };
            }
        });

        t.addCommand(new SwingCommand() {
            protected Task createTask() {
                return new Task() {
                    protected void doInEventThread() throws Exception {
                        assertIsTrue(false, "If the composite is cancelled during the first child command execution" +
                                "this should not be called.");
                    }
                };
            }
        });

        assertIsTrue(2 == t.getTotalCommands(), "Should be 2 child commands");

        final SwingCommand<Object,String> compositeCommand = new SwingCommand<Object,String>() {
            protected Task<Object,String> createTask() {
                assertOrdering(1, "createTask");
                return t;
            }

            public String toString() {
                return "TestCancellableCompositeTask";
            }
        };

        compositeCommand.addTaskListener(new TaskListenerAdapter<String>() {
            public void finished(Task task) {
                latch.countDown();
            }
        });
        compositeCommand.execute();
        return t;
    }

    public void compositeCancelOfChildCommand() {

        final CompositeCommandTask<Object,String> t = doCompositeCommand();

        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
        s.schedule(new Runnable() {
            public void run() {
                //cancel by cancelling the currently running child command
                t.getCurrentChildTask().cancel();
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void doCancellationTest(final BlockingRunnable blockingRunnable) {
        final Thread startThread = Thread.currentThread();

        task = new InterruptibleTask() {

            public void doInBackground() throws Exception {
                assertNotInThread(startThread, "doInBackground");
                assertNotInEventThread("doInBackground");
                assertOrdering(4, "doInBackground");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
                blockingRunnable.run();
            }

            @Override
            public void doEvenIfCancelled() throws Exception {
                assertIsTrue(isCancelled(), "Should be Cancelled");
                assertInEventThread("doEvenIfCancelled");
                assertOrdering(6, "doEvenIfCancelled");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
            }

            @Override
            public void doInEventThreadIfNotCancelled() {
                assertIsTrue(false, "doEvenIfNotCancelled should not be called if task is cancelled");
            }

        };

        final SwingCommand dummyCommand = new SwingCommand() {
            protected Task createTask() {
                assertOrdering(1, "createTask");
                return task;
            }

            public String toString() {
                return "TestBackgroundTask";
            }
        };

        dummyCommand.addTaskListener(new ThreadCheckingTaskListener() {

            public void doPending(Task task) {
                assertExpectedState(Task.ExecutionState.PENDING, task.getExecutionState());
                assertOrdering(2, "pending");
            }

            public void doStarted(Task task) {
                assertExpectedState(Task.ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(3, "started");
            }

            public void doProgress(Task task, String progressDescription) {
                if ( progressDescription.equals(DO_IN_BACKGROUND_PROGRESS_TEXT)) {
                    assertOrdering(5, DO_IN_BACKGROUND_PROGRESS_TEXT);
                } else {
                    assertOrdering(7, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                }
            }

            public void doSuccess(Task task) {
                assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
                assertOrdering(8, "success");
            }

            public void doError(Task task, Throwable error) {
                isBadListenerMethodCalled = true;
            }

            public void doFinished(Task task) {
                assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
                assertOrdering(9, "finished");
                latch.countDown();
            }
        });

        assertEquals(Task.ExecutionState.NOT_RUN, task.getExecutionState());
        dummyCommand.execute();

        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
        s.schedule(new Runnable() {
            public void run() {
                task.cancel();
            }
        }, 500, TimeUnit.MILLISECONDS);
    }


    private void doNoCancellationTest(final BlockingRunnable blockingRunnable) {
        final Thread startThread = Thread.currentThread();

        task = new InterruptibleTask() {

            public void doInBackground() throws Exception {
                assertNotInThread(startThread, "doInBackground");
                assertNotInEventThread("doInBackground");
                assertOrdering(4, "doInBackground");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
                blockingRunnable.run();
            }

            @Override
            public void doEvenIfCancelled() throws Exception {
                assertIsTrue(! isCancelled(), "Should not be Cancelled");
                assertInEventThread("doEvenIfCancelled");
                assertOrdering(6, "doEvenIfCancelled");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT + 1);
            }

            @Override
            public void doInEventThreadIfNotCancelled() {
                assertIsTrue(! isCancelled(), "Should not be Cancelled");
                assertInEventThread("doInEventThreadIfNotCancelled");
                assertOrdering(8, "doInEventThreadIfNotCancelled");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT + 2);
            }

        };

        final SwingCommand dummyCommand = new SwingCommand() {
            protected Task createTask() {
                assertOrdering(1, "createTask");
                return task;
            }

            public String toString() {
                return "TestBackgroundTask";
            }
        };

        dummyCommand.addTaskListener(new ThreadCheckingTaskListener() {

            public void doPending(Task task) {
                assertExpectedState(Task.ExecutionState.PENDING, task.getExecutionState());
                assertOrdering(2, "pending");
            }

            public void doStarted(Task task) {
                assertExpectedState(Task.ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(3, "started");
            }

            public void doProgress(Task task, String progressDescription) {
                if ( progressDescription.equals(DO_IN_BACKGROUND_PROGRESS_TEXT)) {
                    assertOrdering(5, DO_IN_BACKGROUND_PROGRESS_TEXT);
                } else if ( progressDescription.equals(DO_IN_EVENT_THREAD_PROGRESS_TEXT + 1)){
                    assertOrdering(7, DO_IN_EVENT_THREAD_PROGRESS_TEXT + 1);
                } else {
                    assertOrdering(9, DO_IN_EVENT_THREAD_PROGRESS_TEXT + 2);
                }
            }

            public void doSuccess(Task task) {
                assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
                assertOrdering(10, "success");
            }

            public void doError(Task task, Throwable error) {
                isBadListenerMethodCalled = true;
            }

            public void doFinished(Task task) {
                assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
                assertOrdering(11, "finished");
                latch.countDown();
            }
        });

        assertEquals(Task.ExecutionState.NOT_RUN, task.getExecutionState());
        dummyCommand.execute();
    }

    private void doCancelledTaskFailsIfCancelledNotCalledAndExceptionThrown() {
        final Thread startThread = Thread.currentThread();

        task = new InterruptibleTask() {

            public void doInBackground() throws Exception {
                assertNotInThread(startThread, "doInBackground");
                assertNotInEventThread("doInBackground");
                assertOrdering(4, "doInBackground");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
                throw new Exception("Failed without cancel");
            }

            @Override
            public void doEvenIfCancelled() throws Exception {
                assertIsTrue(false, "The task failed, doEvenIfCancelled() should not be called");
            }

            @Override
            public void doInEventThreadIfNotCancelled() {
                assertIsTrue(false, "The task failed, doInEventThreadIfNotCancelled() should not be called");
            }

        };

        final SwingCommand dummyCommand = new SwingCommand() {
            protected Task createTask() {
                assertOrdering(1, "createTask");
                return task;
            }

            public String toString() {
                return "TestBackgroundTask";
            }
        };

        dummyCommand.addTaskListener(new ThreadCheckingTaskListener() {

            public void doPending(Task task) {
                assertExpectedState(Task.ExecutionState.PENDING, task.getExecutionState());
                assertOrdering(2, "pending");
            }

            public void doStarted(Task task) {
                assertExpectedState(Task.ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(3, "started");
            }

            public void doProgress(Task task, String progressDescription) {
                if ( progressDescription.equals(DO_IN_BACKGROUND_PROGRESS_TEXT)) {
                    assertOrdering(5, DO_IN_BACKGROUND_PROGRESS_TEXT);
                }
            }

            public void doSuccess(Task task) {
                isBadListenerMethodCalled = true;
            }

            public void doError(Task task, Throwable error) {
                assertEquals(Task.ExecutionState.ERROR, task.getExecutionState());
                assertOrdering(6, "doError");
            }

            public void doFinished(Task task) {
                assertEquals(Task.ExecutionState.ERROR, task.getExecutionState());
                assertOrdering(7, "finished");
                latch.countDown();
            }
        });

        assertEquals(Task.ExecutionState.NOT_RUN, task.getExecutionState());
        dummyCommand.execute();
    }

    interface BlockingRunnable {
        public void run() throws InterruptedException;
    }

    class SleepBlockingRunnable implements BlockingRunnable {
        public void run() throws InterruptedException {
            Thread.sleep(1000);
        }
    }

    class ActiveBlockingRunnable implements BlockingRunnable {
        public void run() throws InterruptedException {
            long startTime = System.currentTimeMillis();
            for ( int loop=0; loop < 100000000; loop++) {
                if ( Thread.currentThread().isInterrupted() || ((System.currentTimeMillis() - startTime) > 1000)) {
                    break;
                }
            }
        }
    }



}
