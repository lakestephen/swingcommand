package swingcommand2;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 25-Apr-2009
 * Time: 12:07:07
 * To change this template use File | Settings | File Templates.
 */
public class TestBackgroundTaskPendingState extends CommandTest {

     public void testPendingState() {

        final DummyBackgroundTask task = new DummyBackgroundTask();

        final SwingCommand dummyCommand = new SwingCommand() {
            public String toString() {
                return "testPendingState";
            }

            protected SimpleTask createTask() {
                assertOrdering(1, "createTask");
                return task;
            }
        };

        TaskListener l = new TaskListenerAdapter() {
            public void started(SimpleTask commandExecution) {
                assertOrdering(3, "started");
                latch.countDown();
            }
        };

        //just delay the start of the execution, simulate an executor with a queue delay, so we can test the pending state
        //before the command starts
        SimpleTask t = dummyCommand.execute(new DelayedExecutor(), l);
        assertEquals(ExecutionState.PENDING, t.getState());
        assertOrdering(2, "pending");
        assertTrue(t == task);
        waitForLatch();
        assertOrdering(4, "end");
        checkOrderingFailureText();
    }
}
