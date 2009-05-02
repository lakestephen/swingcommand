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
 * Date: 30-Aug-2008
 * Time: 00:16:27
 * To change this template use File | Settings | File Templates.
 */
public class TestCompositeTask extends AbstractCommandTest {

    public void testComposite() {

       final DefaultCompositeCommandTask compositeTask = new DefaultCompositeCommandTask();

       //create and add 10 async child commands to the composite
       int startCount = 1;
       for ( int loop=1; loop<= 10; loop++) {
           compositeTask.addCommand(createBackgroundExecutionCommand(startCount));
           startCount += 4;
       }

       SwingCommand<Object,String> compositeCommand = new SwingCommand<Object,String>() {
           protected Task<Object,String> createTask() {
               return compositeTask;
           }
       };

       //execute the composite synchronously on this junit subthread
       compositeCommand.execute(new SynchronousExecutor());
       assertOrdering(41, "execute returned");
       checkFailureText();
    }

     public void testCompositeFailsOnChildFailure() {

       final DefaultCompositeCommandTask compositeTask = new DefaultCompositeCommandTask();

       compositeTask.addCommand(new SwingCommand() {
           protected Task createTask() {
               return new Task() {
                   protected void doInEventThread() throws Exception {
                       assertOrdering(1, "First child");
                   }
               };
           }
       });

       compositeTask.addCommand(new SwingCommand() {
           protected Task createTask() {
               return new BackgroundTask() {
                   protected void doInEventThread() throws Exception {
                       assertOrdering(2, "Second child");
                       throw new Exception("Child fails");
                   }

                   protected void doInBackground() throws Exception {
                   }
               };
           }
       });

       compositeTask.addCommand(new SwingCommand() {
           protected Task createTask() {
               return new Task() {
                   protected void doInEventThread() throws Exception {
                       assertIsTrue(false, "This child should not execute, the previous one failed");
                   }
               };
           }
       });

       SwingCommand compositeCommand = new SwingCommand() {
           protected Task createTask() {
               return compositeTask;
           }
       };

       //execute the composite synchronously on this junit subthread
       Task t = compositeCommand.execute(new SynchronousExecutor());
       assertOrdering(3, "execute returned");
       checkFailureText();
       assertEquals(Task.ExecutionState.ERROR, t.getExecutionState());
    }

    private SwingCommand createBackgroundExecutionCommand(final int startCount) {
        return new SwingCommand() {
            protected Task createTask() {
                return new TestBackgroundExecution(startCount);
            }
        };
    }

    private class TestBackgroundExecution extends BackgroundTask {
        private int currentCount;

        public TestBackgroundExecution(int counterStart) {
            this.currentCount = counterStart;
        }

        public void setExecutionState(ExecutionState executionState) {
            if ( executionState == ExecutionState.STARTED) {
                assertOrdering(currentCount, "setExecutionState(STARTED)");
            } else if ( executionState == ExecutionState.SUCCESS) {
                assertOrdering(currentCount + 3, "setExecutionState(SUCCESS)");
            }
        }

        public void doInBackground() throws Exception {
            assertOrdering(currentCount + 1, "doInBackground");
            if ( SwingUtilities.isEventDispatchThread()) {
                failureText.append("doInBackground on event thread");
            }
        }

        public void doInEventThread() throws Exception {
            assertOrdering(currentCount + 2, "doInEventThread");
            if ( ! SwingUtilities.isEventDispatchThread()) {
                failureText.append("doInEventThread not on event thread");
            }
        }
    }
}