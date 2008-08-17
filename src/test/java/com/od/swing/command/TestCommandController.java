package com.od.swing.command;

import org.jmock.Expectations;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 17-Aug-2008
 * Time: 19:24:59
 * To change this template use File | Settings | File Templates.
 */
public class TestCommandController extends AsyncCommandTest {

    private volatile AbstractAsynchronousCommand<CommandExecution> lastCommand;
    private boolean isCommandStoppedCalled;
    private boolean isCommandErrorCalled;

    @Override
    public void doSetUp() {
        isCommandErrorCalled = false;
        isCommandErrorCalled = false;
        lastCommand = null;
    }

    public void testStartingAndStoppedCalledDuringNormalLifecycle() {  
        final String name = "testStartingAndStoppedCalledDuringNormalLifecycle";

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    @SuppressWarnings("unchecked")
                    final CommandController<CommandExecution> controller = (CommandController<CommandExecution>)mockery.mock(CommandController.class);
                    final CommandExecution execution = mockery.mock(CommandExecution.class);

                    lastCommand = new AbstractAsynchronousCommand<CommandExecution>(name, new DebuggingProxyCommandController<CommandExecution>(controller)) {
                        public CommandExecution createExecution() {
                            return execution;
                        }
                    };

                    mockery.checking(new Expectations() {{
                        try {
                            //little worried here because these callbacks are not all on the same thread and I'm not convinced JMock supports this,
                            // but the test seems to work OK so I'll leave it for now
                            one(controller).commandStarting(name, execution);
                            one(execution).doExecuteAsync();
                            one(execution).doAfterExecute();
                            one(controller).commandStopped(name, execution);
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail(e.toString());
                        }
                    }});

                    lastCommand.execute();
                }
            }
        );
        joinCommandThread(lastCommand);
        validateMockeryAssertions();
    }


    public void testExceptionDuringDoExecuteAsyncCausesCommandControllerToReceiveException() {
        final String name = "testExceptionDuringDoExecuteAsyncCausesCommandControllerToReceiveException";

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    @SuppressWarnings("unchecked")
                    final CommandController<CommandExecution> controller = (CommandController<CommandExecution>)mockery.mock(CommandController.class);
                    final CommandExecution execution = new CommandExecution() {

                        public void doExecuteAsync() throws Exception {
                            throw new RuntimeException(name);
                        }

                        public void doAfterExecute() throws Exception {
                        }
                    };

                    lastCommand = new AbstractAsynchronousCommand<CommandExecution>(name, new DebuggingProxyCommandController<CommandExecution>(controller)) {
                        public CommandExecution createExecution() {
                            return execution;
                        }
                    };

                    mockery.checking(new Expectations() {{
                        try {
                            //little worried here because these callbacks are not all on the same thread and I'm not convinced JMock supports this,
                            // but the test seems to work OK so I'll leave it for now
                            one(controller).commandStarting(name, execution);
                            one(controller).commandError(with(equal(name)), with(equal(execution)), with(any(Exception.class)));
                            one(controller).commandStopped(name, execution);
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail(e.toString());
                        }
                    }});

                    lastCommand.execute();
                }
            }
        );
        joinCommandThread(lastCommand);
        validateMockeryAssertions();
    }


    public void testExceptionInCommandStartingAbortsProcessing() {
        final String name = "testExceptionInCommandStartingAbortsProcessing";

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    @SuppressWarnings("unchecked")
                    final CommandController<CommandExecution> controller = new CommandController<CommandExecution>() {

                        public void commandStarting(String commandName, CommandExecution commandExecution) throws Exception {
                            throw new RuntimeException(name);
                        }

                        public void commandStopped(String commandName, CommandExecution commandExecution) {
                            isCommandStoppedCalled = true;
                        }

                        public void commandError(String commandName, CommandExecution commandExecution, Throwable t) {
                            isCommandErrorCalled = true;
                        }
                    };

                    final CommandExecution execution = mockery.mock(CommandExecution.class);

                    lastCommand = new AbstractAsynchronousCommand<CommandExecution>(name, new DebuggingProxyCommandController<CommandExecution>(controller)) {
                        public CommandExecution createExecution() {
                            return execution;
                        }
                    };

                    //an exception was thrown by the controller in starting method, so the execution callbacks should not occur
                    mockery.checking(new Expectations() {{
                        try {
                            //little worried here because these callbacks are not all on the same thread and I'm not convinced JMock supports this,
                            // but the test seems to work OK so I'll leave it for now
                            never(execution).doExecuteAsync();
                            never(execution).doAfterExecute();
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail(e.toString());
                        }
                    }});

                    lastCommand.execute();
                }
            }
        );
        joinCommandThread(lastCommand);
        validateMockeryAssertions();
        assertTrue(isCommandStoppedCalled); //commands which throw an exception in starting still have stopped called
        assertTrue(isCommandErrorCalled);   //starting threw an exception
    }

}
