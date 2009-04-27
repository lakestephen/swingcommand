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

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:02:40
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTask extends AbstractCommandTest {

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

        assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
        assertFalse(isBadListenerMethodCalled);
        checkFailureText();
    }

    private void doTest() {
        final Thread startThread = Thread.currentThread();

        task = new BackgroundTask() {

            public void doInBackground() throws Exception {
                assertNotInThread(startThread, "doInBackground");
                assertNotInEventThread("doInBackground");
                assertOrdering(4, "doInBackground");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
            }

            public void doInEventThread() throws Exception {
                assertInEventThread("doInEventThread");
                assertOrdering(6, "doInEventThread");
                assertExpectedState(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
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
    }
}
