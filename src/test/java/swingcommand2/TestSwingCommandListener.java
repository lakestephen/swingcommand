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

import org.jmock.Expectations;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Sep-2008
 * Time: 09:15:34
 */
public class TestSwingCommandListener extends CommandTest {

    private Throwable runtimeException;
    private boolean calledInEventThread;

    public void doSetUp() {
        runtimeException = null;
        calledInEventThread = false;
    }

    public void testNormalExecutionObserverCallbacks() {

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    final TaskListener observer =  mockery.mock(TaskListener.class);

                    final SwingTask execution = new SwingTask() {
                        public void doInEventThread() throws Exception {
                        }
                    };

                    mockery.checking(new Expectations() {{
                            one(observer).pending(execution);
                            one(observer).started(execution);
                            one(observer).success(execution);
                            one(observer).finished(execution);
                        }
                    });

                    SwingCommand c = new SwingCommand() {
                        protected SwingTask createTask() {
                            return execution;
                        }
                    };
                    c.execute();
                }
            }
        );
    }

    public void testErrorExecutionObserverCallbacks() {

        invokeAndWaitWithFail(
            new Runnable() {
                public void run() {
                    final TaskListener observer = mockery.mock(TaskListener.class);

                    final SwingTask execution = new SwingTask() {
                        public void doInEventThread() throws Exception {
                            runtimeException = new RuntimeException("testErrorExecutionObserverCallbacks");
                        }
                    };

                    mockery.checking(new Expectations() {{
                            one(observer).pending(execution);
                            one(observer).started(execution);
                            one(observer).error(execution, runtimeException);
                            one(observer).finished(execution);
                        }
                    });

                    SwingCommand c = new SwingCommand() {
                        protected SwingTask createTask() {
                            return execution;
                        }
                    };
                    c.execute();
                }
            }
        );
    }

    public void testDoInEventThreadCalledInEventThread() {

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final SwingTask execution = new SwingTask() {
            public void doInEventThread() throws Exception {
                calledInEventThread = SwingUtilities.isEventDispatchThread();
                countDownLatch.countDown();
            }
        };

         SwingCommand c = new SwingCommand() {
            protected SwingTask createTask() {
                return execution;
            }
         };
        c.execute();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("doInEventThread should be called in event thread", calledInEventThread);
    }
}