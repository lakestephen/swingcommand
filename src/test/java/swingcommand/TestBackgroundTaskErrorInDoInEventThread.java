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

import junit.framework.Assert;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:06:08
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTaskErrorInDoInEventThread extends AbstractCommandTest {

    private BackgroundTask<String> task;

    public void testErrorInDoInEventThreadFromBackgroundThread() {
        doTest();
        checkEndStates();
    }

    public void testErrorInDoInEventThreadFromEventThread() {
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

        assertEquals(ExecutionState.ERROR, task.getExecutionState());
        assertFalse(isBadListenerMethodCalled);
        assertEquals(testException, task.getExecutionException());
        checkOrderingFailureText();
    }

    private void doTest() {
        final Thread startThread = Thread.currentThread();

        task = new BackgroundTask<String>() {
            public void doInBackground() throws Exception {
                assertNotInThread(startThread, "doInBackground");
                assertNotInEventThread("doInBackground");
                assertOrdering(4, "doInBackground");
                Assert.assertEquals(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_BACKGROUND_PROGRESS_TEXT);
            }

            public void doInEventThread() throws Exception {
                assertInEventThread("doInEventThread");
                assertOrdering(6, "doInEventThread");
                Assert.assertEquals(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                testException = new RuntimeException("ErrorInDoInEventThread");
                throw testException;
            }
        };

        final SwingCommand<String> dummyCommand = new SwingCommand<String>() {
            protected Task<String> createTask() {
                assertInThread(startThread, "createTask in start thread");
                assertOrdering(1, "createTask");
                return task;
            }

            public String toString() {
                return "testErrorInDoInEventThreadFromBackgroundThread";
            }
        };

        dummyCommand.addTaskListener(new ThreadCheckingTaskListener() {

            public void doPending(Task commandExecution) {
                Assert.assertEquals(ExecutionState.PENDING, task.getExecutionState());
                assertOrdering(2, "pending");
            }

            public void doStarted(Task commandExecution) {
                Assert.assertEquals(ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(3, "started");
            }

            public void doProgress(Task commandExecution, String progressDescription) {
                if ( progressDescription.equals(DO_IN_BACKGROUND_PROGRESS_TEXT)) {
                    assertOrdering(5, DO_IN_BACKGROUND_PROGRESS_TEXT);
                } else {
                    assertOrdering(7, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
                }
            }

            public void doSuccess(Task commandExecution) {
                isBadListenerMethodCalled = true;
            }

            public void doError(Task commandExecution, Throwable error) {
                assertEquals(ExecutionState.ERROR, task.getExecutionState());
                assertOrdering(8, "error");
            }

            public void doFinished(Task commandExecution) {
                assertEquals(ExecutionState.ERROR, task.getExecutionState());
                assertOrdering(9, "finished");
                latch.countDown();
            }
        });

        assertEquals(ExecutionState.NOT_RUN, task.getExecutionState());
        dummyCommand.execute();
    }
}
