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

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:07:07
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTaskPendingState extends AbstractCommandTest {

     public void testPendingState() {

        final DummyBackgroundTask task = new DummyBackgroundTask();

        final SwingCommand<String> dummyCommand = new SwingCommand<String>() {
            public String toString() {
                return "testPendingState";
            }

            protected Task<String> createTask() {
                assertOrdering(1, "createTask");
                return task;
            }
        };

        TaskListener l = new TaskListenerAdapter() {
            public void started(Task task) {
                assertOrdering(3, "started");
                latch.countDown();
            }
        };

        //just delay the start of the execution, simulate an executor with a queue delay, so we can test the pending state
        //before the command starts
        Task t = dummyCommand.execute(new DelayedExecutor(), l);
        assertEquals(Task.ExecutionState.PENDING, t.getExecutionState());
        assertOrdering(2, "pending");
        assertTrue(t == task);
        waitForLatch();
        assertOrdering(4, "end");
        checkOrderingFailureText();
    }
}
