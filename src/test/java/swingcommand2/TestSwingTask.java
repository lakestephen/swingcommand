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

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 24-Sep-2008
 * Time: 22:54:18
 * To change this template use File | Settings | File Templates.
 */
public class TestSwingTask extends CommandTest {

    private boolean doInEventThreadCalled;
    private RuntimeException executionException;

    public void doSetUp() {
        doInEventThreadCalled = false;
        executionException = null;
    }

    public void testNormalProcessing() {
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    final SwingTask execution = new SwingTask() {
                        public void doInEventThread() throws Exception {
                            doInEventThreadCalled = true;
                            assertEquals(ExecutionState.STARTED, getState());
                        }
                    };

                    SwingCommand c = new SwingCommand() {
                        protected SwingTask createTask() {
                            return execution;
                        }
                    };

                    assertEquals(ExecutionState.NOT_RUN, execution.getState());
                    c.execute();
                    assertEquals(ExecutionState.SUCCESS, execution.getState());
                    assertTrue(doInEventThreadCalled);
                }
            }
        );

    }

    public void testErrorProcessing() {
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    final SwingTask execution = new SwingTask() {
                        public void doInEventThread() throws Exception {
                            doInEventThreadCalled = true;
                            assertEquals(ExecutionState.STARTED, getState());
                            executionException = new RuntimeException("testErrorProcessing");
                            throw executionException;
                        }
                    };

                    SwingCommand c = new SwingCommand() {
                        protected SwingTask createTask() {
                            return execution;
                        }
                    };

                    assertEquals(ExecutionState.NOT_RUN, execution.getState());
                    c.execute();
                    assertEquals(ExecutionState.ERROR, execution.getState());
                    assertTrue(doInEventThreadCalled);
                    assertEquals(executionException, execution.getExecutionException());
                }
            }
        );



    }

}