package com.od.swing.command;

import org.jmock.Expectations;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Aug-2008
 * Time: 14:41:41
 * To change this template use File | Settings | File Templates.
 */
public class TestAbstractAsynchronousCommand extends AsyncCommandTest {

    public void testLifecycleStartAndStopAreCalledDuringNormalExecution() {
        final String name = "testLifecycleStartAndStopAreCalledDuringNormalExecution";
        final DummyExecution dummyExecution = new DummyExecution();
        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand(name, dummyExecution);

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    @SuppressWarnings("unchecked")
                    final LifeCycleMonitor<CommandExecution> monitor = (LifeCycleMonitor<CommandExecution>)mockery.mock(LifeCycleMonitor.class);
                    dummyCommand.addLifeCycleMonitor(monitor, debuggingLifeCycleMonitor);

                    mockery.checking(new Expectations() {{
                        one(monitor).started(name, dummyExecution);
                        one(monitor).ended(name, dummyExecution);
                    }});

                    dummyCommand.execute();
                }
            }
        );
        joinCommandThread(dummyCommand);
        validateMockeryAssertions();
    }

    /**
     * If an error is thrown during executeAsync, lifecycle error should be called,
     * but you should still get a call to lifecycle ended
     */
    public void testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDoExecuteAsync() {
        String name = "testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDoExecuteAsync";
        testLifecycleError(
            name,
            new DummyExecution() {
                public void doExecuteAsync() throws Exception {
                    throw new Exception();
                }
            }
        );
    }

    /**
     * If an error is thrown during doAfterExecute, lifecycle error should be called,
     * but you should still get a call to lifecycle ended
     */
    public void testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDoAfterExecute() {
        String name = "testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDoAfterExecute";
        testLifecycleError(
            name,
            new DummyExecution() {
                public void doAfterExecute() throws Exception {
                    throw new Exception();
                }
            }
        );
    }


    private void testLifecycleError(final String name, final DummyExecution dummyExecution) {
        final DummyAsyncCommand dummyCommand = new DummyAsyncCommand(name, dummyExecution);

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    @SuppressWarnings("unchecked")
                    final LifeCycleMonitor<CommandExecution> monitor = (LifeCycleMonitor<CommandExecution>)mockery.mock(LifeCycleMonitor.class);
                    dummyCommand.addLifeCycleMonitor(monitor, debuggingLifeCycleMonitor);

                    mockery.checking(new Expectations() {{
                        one(monitor).started(name, dummyExecution);
                        one(monitor).error(with(equal(name)), with(equal(dummyExecution)), with(any(Exception.class)));
                        one(monitor).ended(name, dummyExecution);
                    }});

                    dummyCommand.execute();
                }
            }
        );
        joinCommandThread(dummyCommand);
        validateMockeryAssertions();
    }


}
