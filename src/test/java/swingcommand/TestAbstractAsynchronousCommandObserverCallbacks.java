/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand;

import org.jmock.Expectations;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Aug-2008
 * Time: 14:41:41
 * To change this template use File | Settings | File Templates.
 */
public class TestAbstractAsynchronousCommandObserverCallbacks extends CommandTest {

    public void testLifecycleStartAndStopAreCalledDuringNormalExecution() {
        final String name = "testLifecycleStartAndStopAreCalledDuringNormalExecution";
        testNormalExecution(name);
    }

    /**
     * If an error is thrown during executeAsync, executionObserver.error() should be called,
     * but you should still get a call to executionObserver.stopped()
     */
    public void testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDoInBackground() {
        String name = "testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDoInBackground";
        testErrorExecution(
            name,
            new DummyExecution() {
                public void doInBackground() throws Exception {
                    throw new Exception();
                }
            }
        );
    }

    /**
     * If an error is thrown during done, executionObserver.error() should be called,
     * but you should still get a call to executionObserver.stopped()
     */
    public void testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDone() {
        String name = "testLifecycleStartErrorAndStopAreCalledIfExceptionThrownDuringDone";
        testErrorExecution(
            name,
            new DummyExecution() {
                public void doInEventThread() throws Exception {
                    throw new Exception();
                }
            }
        );
    }

    /**
     * These observer callbacks probably change things on the UI and exceptions may occur -
     * but this should not affect the lifecycle of the command
     */
    public void testErrorsInLifecycleObserverMethodsDontInterruptNormalProcessing() {
        final String name = "testErrorsInLifecycleObserverMethodsDontStopNormalProcessing";
        testNormalExecution(name, new RuntimeExceptionThrowingExecutionObserver());
    }

     /**
     * These observer callbacks probably change things on the UI and exceptions may occur -
     * but this should not affect the lifecycle of the command
     */
    public void testErrorsInLifecycleObserverMethodsDontInterruptErrorProcessing() {
        final String name = "testErrorsInLifecycleObserverMethodsDontStopNormalProcessing";
        testErrorExecution(
            name,
            new DummyExecution() {
                public void doInEventThread() throws Exception {
                    throw new Exception();
                }
            },
            new RuntimeExceptionThrowingExecutionObserver()
        );
    }

    private void testNormalExecution(final String name, final ExecutionObserver<AsynchronousExecution>... extraLifeCycleObserver) {
        final DummyExecution dummyExecution = new DummyExecution();
        final DummyAsynchronousCommand dummyCommand = new DummyAsynchronousCommand(dummyExecution) {
            public String toString() {
                return name;
            }
        };

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    @SuppressWarnings("unchecked")
                    final ExecutionObserver<AsynchronousExecution> observer = (ExecutionObserver<AsynchronousExecution>)mockery.mock(ExecutionObserver.class);

                    if ( extraLifeCycleObserver != null ) {
                        dummyCommand.addExecutionObserver(extraLifeCycleObserver);
                    }

                    dummyCommand.addExecutionObserver(observer, debuggingExecutionObserver);

                    mockery.checking(new Expectations() {{
                        one(observer).pending(dummyExecution);
                        one(observer).started(dummyExecution);
                        one(observer).success(dummyExecution);
                        one(observer).done(dummyExecution);
                    }});

                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        validateMockeryAssertions();
    }

    private void testErrorExecution(final String name, final DummyExecution dummyExecution, final ExecutionObserver<AsynchronousExecution>... extraLifeCycleObserver) {
        final DummyAsynchronousCommand dummyCommand = new DummyAsynchronousCommand(dummyExecution) {
            public String toString() {
                return name;
            }
        };

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    @SuppressWarnings("unchecked")
                    final ExecutionObserver<AsynchronousExecution> observer = (ExecutionObserver<AsynchronousExecution>)mockery.mock(ExecutionObserver.class);

                    if ( extraLifeCycleObserver != null ) {
                        dummyCommand.addExecutionObserver(extraLifeCycleObserver);
                    }

                    dummyCommand.addExecutionObserver(observer, debuggingExecutionObserver);

                    mockery.checking(new Expectations() {{
                        one(observer).pending(dummyExecution);
                        one(observer).started(dummyExecution);
                        one(observer).error(with(equal(dummyExecution)), with(any(Exception.class)));
                        one(observer).done(dummyExecution);
                    }});

                    dummyCommand.execute();
                }
            }
        );
        joinLastExecutorThread();
        validateMockeryAssertions();
    }


}