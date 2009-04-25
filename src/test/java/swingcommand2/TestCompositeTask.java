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

import javax.swing.*;


/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 30-Aug-2008
 * Time: 00:16:27
 * To change this template use File | Settings | File Templates.
 */
public class TestCompositeTask extends CommandTest {

    public void testCompositeWithAsyncExecutions() {

       final CompositeCommandTask compositeExecution = new CompositeCommandTask();

       //create and add 10 async child commands to the composite
       int startCount = 1;
       for ( int loop=1; loop<= 10; loop++) {
           compositeExecution.addCommand(createBackgroundExecutionCommand(startCount));
           startCount += 4;
       }

       SwingCommand compositeCommand = new SwingCommand() {
           protected SwingTask createTask() {
               return compositeExecution;
           }
       };

       //execute the composite synchronously on this junit subthread
       compositeCommand.execute(new SynchronousExecutor());
       assertOrdering(41, "execute returned");
       checkOrderingFailureText();
    }

    private SwingCommand createBackgroundExecutionCommand(final int startCount) {
        return new SwingCommand() {
            protected SwingTask createTask() {
                return new TestBackgroundExecution(startCount);
            }
        };
    }

    private class TestBackgroundExecution extends BackgroundTask {
        private int currentCount;

        public TestBackgroundExecution(int counterStart) {
            this.currentCount = counterStart;
        }

        public void setState(ExecutionState executionState) {
            if ( executionState == ExecutionState.STARTED) {
                assertOrdering(currentCount, "setState(STARTED)");
            } else if ( executionState == ExecutionState.SUCCESS) {
                assertOrdering(currentCount + 3, "setState(SUCCESS)");
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