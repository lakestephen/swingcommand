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
 * Time: 12:17:54
 * To change this template use File | Settings | File Templates.
 */
public class TestSimpleTask extends AbstractCommandTest {

    private Task<String> task;

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
        assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
        assertFalse(isBadListenerMethodCalled);
        checkFailureText();
    }

    private Task doTask() {
        final Thread startThread = Thread.currentThread();

        task = new Task<String>() {
            public void doInEventThread() throws Exception {
                assertOrdering(4, "doInEventThread");
                assertInEventThread("doInEventThread");
                assertEquals(ExecutionState.STARTED, getExecutionState());
                fireProgress(DO_IN_EVENT_THREAD_PROGRESS_TEXT);
            }
        };

        SwingCommand<String> c = new SwingCommand<String>() {
            protected Task<String> createTask() {
                assertInThread(startThread, "createTask");
                assertOrdering(1, "createTask");
                return task;
            }
        };

        c.addTaskListener(new ThreadCheckingTaskListener() {

            public void doPending(Task task) {
                assertExpectedState(Task.ExecutionState.PENDING, task.getExecutionState());
                assertOrdering(2, "pending");
            }

            public void doStarted(Task task) {
                assertExpectedState(Task.ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(3, "started");
            }

            public void doProgress(Task task, String progressDescription) {
                assertExpectedState(Task.ExecutionState.STARTED, task.getExecutionState());
                assertOrdering(5, DO_IN_EVENT_THREAD_PROGRESS_TEXT);
            }

            public void doSuccess(Task task) {
                assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
                assertOrdering(6, "success");
            }

            public void doError(Task task, Throwable error) {
                isBadListenerMethodCalled = true;
            }

            public void doFinished(Task task) {
                assertEquals(Task.ExecutionState.SUCCESS, task.getExecutionState());
                assertOrdering(7, "finished");
                latch.countDown();
            }
        });

        assertEquals(Task.ExecutionState.NOT_RUN, task.getExecutionState());
        c.execute();
        return task;
    }
}
