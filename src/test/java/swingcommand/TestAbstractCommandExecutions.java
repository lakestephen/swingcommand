package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 24-Sep-2008
 * Time: 22:54:18
 * To change this template use File | Settings | File Templates.
 */
public class TestAbstractCommandExecutions extends CommandTest {

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
                    final CommandExecution execution = new DefaultExecution() {
                        public void doInEventThread() throws Exception {
                            doInEventThreadCalled = true;
                            assertEquals(ExecutionState.STARTED, getState());
                        }
                    };

                    DefaultCommand c = new DefaultCommand() {
                        protected CommandExecution createExecution() {
                            return execution;
                        }
                    };

                    assertEquals(ExecutionState.PENDING, execution.getState());
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
                    final CommandExecution execution = new DefaultExecution() {
                        public void doInEventThread() throws Exception {
                            doInEventThreadCalled = true;
                            assertEquals(ExecutionState.STARTED, getState());
                            executionException = new RuntimeException("testErrorProcessing");
                            throw executionException;
                        }
                    };

                    DefaultCommand c = new DefaultCommand() {
                        protected CommandExecution createExecution() {
                            return execution;
                        }
                    };

                    assertEquals(ExecutionState.PENDING, execution.getState());
                    c.execute();
                    assertEquals(ExecutionState.ERROR, execution.getState());
                    assertTrue(doInEventThreadCalled);
                    assertEquals(executionException, execution.getExecutionException());
                }
            }
        );



    }

}
