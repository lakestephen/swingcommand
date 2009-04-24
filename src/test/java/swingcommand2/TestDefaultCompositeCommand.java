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
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 30-Aug-2008
 * Time: 00:16:27
 * To change this template use File | Settings | File Templates.
 */
public class TestDefaultCompositeCommand extends CommandTest {

   public void testCompositeWithAsyncExecutions() {

       final AtomicInteger counter = new AtomicInteger();
       final StringBuffer failureText = new StringBuffer();

       final CompositeCommandTask compositeExecution = new CompositeCommandTask();

       //create and add 10 async child commands to the composite
       int startCount = 1;
       for ( int loop=1; loop<= 10; loop++) {
           compositeExecution.addCommand(createBackgroundExecutionCommand(startCount, counter, failureText));
           startCount += 4;
       }

       SwingCommand compositeCommand = new SwingCommand() {
           protected SwingTask createTask() {
               return compositeExecution;
           }
       };

       //execute the composite synchronously on this junit subthread
       compositeCommand.execute(new SynchronousExecutor());
       assertOrder(41, counter, "execute returned", failureText);

       if ( failureText.length() > 0) {
           fail(failureText.toString());
       }
   }

    private SwingCommand createBackgroundExecutionCommand(final int startCount, final AtomicInteger counter, final StringBuffer failureText) {
        return new SwingCommand() {
            protected SwingTask createTask() {
                return new TestBackgroundExecution(startCount, counter, failureText);
            }
        };
    }

    private void assertOrder(int expectedValue, AtomicInteger counter, String testName, StringBuffer failureDescription) {
        int value = counter.incrementAndGet();
        if ( expectedValue != value) {
            failureDescription.append("Called out of order: ").append(testName).append(" expected ").append(expectedValue).append(" was ").append(value).append("  ");
        }
    }

    private class TestBackgroundExecution extends BackgroundTask {
        private int counterStart;
        private final AtomicInteger counter;
        private final StringBuffer failureText;

        public TestBackgroundExecution(int counterStart, AtomicInteger counter, StringBuffer failureText) {
            this.counterStart = counterStart;
            this.counter = counter;
            this.failureText = failureText;
        }

        public void setState(ExecutionState executionState) {
            if ( executionState == ExecutionState.STARTED) {
                assertOrder(counterStart, counter, "setState(STARTED)", failureText);
            } else if ( executionState == ExecutionState.SUCCESS) {
                assertOrder(counterStart + 3, counter, "setState(SUCCESS)", failureText);
            }
        }

        public void doInBackground() throws Exception {
            assertOrder(counterStart + 1, counter, "doInBackground", failureText);
            if ( SwingUtilities.isEventDispatchThread()) {
                failureText.append("doInBackground on event thread");
            }
        }

        public void doInEventThread() throws Exception {
            assertOrder(counterStart + 2, counter, "doInEventThread", failureText);
            if ( ! SwingUtilities.isEventDispatchThread()) {
                failureText.append("doInEventThread not on event thread");
            }
        }
    }
}