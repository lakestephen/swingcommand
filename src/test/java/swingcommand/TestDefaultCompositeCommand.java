package swingcommand;

import org.jmock.Expectations;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 30-Aug-2008
 * Time: 00:16:27
 * To change this template use File | Settings | File Templates.
 */
public class TestDefaultCompositeCommand extends AsyncCommandTest {

    private boolean failed;

    public void doSetUp() {
        failed = false;
    }

    public void testDefaultCompositeWithAsyncCommands() {
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    final AsynchronousExecution execution1 = mockery.mock(AsynchronousExecution.class, "commandExecution1");
                    final AsynchronousExecution execution2 = mockery.mock(AsynchronousExecution.class, "commandExecution2");

                    final DefaultCompositeCommand<AsynchronousExecution> compositeCommand = new DefaultCompositeCommand<AsynchronousExecution>(new DefaultTestExecutor());
                    compositeCommand.addCommand(new DummyAsynchronousCommand(execution1));
                    compositeCommand.addCommand(new DummyAsynchronousCommand(execution2));

                    mockery.checking(new Expectations() {{
                        try {
                            one(execution1).doInBackground();
                            one(execution1).doInEventThread();
                            one(execution1).isCancelled();
                            one(execution2).doInBackground();
                            one(execution2).doInEventThread();
                            one(execution2).isCancelled();
                        } catch (Exception e) {
                            e.printStackTrace();
                            failed = true;
                        }
                    }});
                    compositeCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertFalse(failed);
        validateMockeryAssertions();
    }

    public void testDefaultCompositeWithMixtureOfCommands() {
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    final AsynchronousExecution execution1 = mockery.mock(AsynchronousExecution.class, "commandExecution1");
                    final AsynchronousExecution execution2 = mockery.mock(AsynchronousExecution.class, "commandExecution2");
                    @SuppressWarnings("unchecked")
                    final Command<CommandExecution> execution3 = (Command<CommandExecution>)mockery.mock(Command.class, "swingcommand");

                    final DefaultCompositeCommand<CommandExecution> compositeCommand = new DefaultCompositeCommand<CommandExecution>(new DefaultTestExecutor());
                    compositeCommand.addCommand(new DummyAsynchronousCommand(execution1));
                    compositeCommand.addCommand(new DummyAsynchronousCommand(execution2));
                    compositeCommand.addCommand(execution3);

                    mockery.checking(new Expectations() {{
                        try {
                            one(execution1).doInBackground();
                            one(execution1).doInEventThread();
                            one(execution1).isCancelled();
                            one(execution2).doInBackground();
                            one(execution2).doInEventThread();
                            one(execution2).isCancelled();
                            one(execution3).execute(with(any(ExecutionObserver[].class))); //help ;./!...
                        } catch (Exception e) {
                            e.printStackTrace();
                            failed = true;
                        }
                    }});
                    compositeCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertFalse(failed);
        validateMockeryAssertions();
    }

    public void testDefaultCompositeCancellingChildExecution() {
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {

                    AsynchronousExecution execution1 = new DefaultExecution(ExecutionAttribute.Cancellable);

                    DummyAsynchronousCommand command1 = new DummyAsynchronousCommand(execution1);
                    DummyAsynchronousCommand command2 = new DummyAsynchronousCommand(new DefaultExecution());


                    final DefaultCompositeCommand<CommandExecution> compositeCommand = new DefaultCompositeCommand<CommandExecution>(new DefaultTestExecutor());
                    compositeCommand.addCommand(command1);
                    compositeCommand.addCommand(command2);

                    command1.addExecutionObserver(
                            new ExecutionObserverAdapter<AsynchronousExecution>() {
                                public void started(AsynchronousExecution commandExecution) {
                                    commandExecution.cancelExecution();
                                }
                            }
                    );

                    command2.addExecutionObserver(
                            new ExecutionObserverAdapter<AsynchronousExecution>() {
                                public void started(AsynchronousExecution commandExecution) {
                                    failed = true;
                                }
                            }
                    );


                    compositeCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertFalse("Failed to cancel", failed);
    }


    public void testDefaultCompositeExceptionInChildDoInBackgroundAbortsProcessing() {
        final AsynchronousExecution execution1 = new DefaultExecution() {
                public void doInBackground() throws Exception {
                    throw new Exception("testDefaultCompositeExeceptionInChildExecutionAbortsProcessing");
                }
        };
        testErrorInChildExecution(execution1);
    }

    public void testDefaultCompositeExceptionInChildDoInEventThreadAbortsProcessing() {
        final AsynchronousExecution execution1 = new DefaultExecution() {
                public void doInEventThread() throws Exception {
                    throw new Exception("testDefaultCompositeExceptionInChildDoInEventThreadAbortsProcessing");
                }
        };
        testErrorInChildExecution(execution1);
    }

    private void testErrorInChildExecution(final AsynchronousExecution execution1) {
        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {

                    DummyAsynchronousCommand command1 = new DummyAsynchronousCommand(execution1);
                    DummyAsynchronousCommand command2 = new DummyAsynchronousCommand(new DefaultExecution());

                    final DefaultCompositeCommand<CommandExecution> compositeCommand = new DefaultCompositeCommand<CommandExecution>(new DefaultTestExecutor());
                    compositeCommand.addCommand(command1);
                    compositeCommand.addCommand(command2);

                    command2.addExecutionObserver(
                            new ExecutionObserverAdapter<AsynchronousExecution>() {
                                public void started(AsynchronousExecution commandExecution) {
                                    failed = true;
                                }
                            }
                    );
                    compositeCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        assertFalse("Failed to cancel", failed);
    }
}
