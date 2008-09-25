/**
 *  Copyright (C) Nick Ebbutt September 2009
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/filtertable
 *
 *  This file is part of ObjectDefinitions Ltd. FilterTable.
 *
 *  FilterTable is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ObjectDefinitions Ltd. FilterTable is distributed in the hope that it will
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with ObjectDefinitions Ltd. FilterTable.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package swingcommand;

import org.jmock.Expectations;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Sep-2008
 * Time: 09:15:34
 */
public class TestAbstractCommandObservers extends CommandTest {

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
                    final ExecutionObserver<CommandExecution> observer = (ExecutionObserver<CommandExecution>) mockery.mock(ExecutionObserver.class);

                    final CommandExecution execution = new DefaultExecution() {
                        public void doInEventThread() throws Exception {
                        }
                    };

                    mockery.checking(new Expectations() {{
                            one(observer).pending(execution);
                            one(observer).started(execution);
                            one(observer).success(execution);
                            one(observer).done(execution);
                        }
                    });

                    DefaultCommand c = new DefaultCommand() {
                        protected CommandExecution createExecution() {
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
                    final ExecutionObserver<CommandExecution> observer = (ExecutionObserver<CommandExecution>) mockery.mock(ExecutionObserver.class);

                    final CommandExecution execution = new DefaultExecution() {
                        public void doInEventThread() throws Exception {
                            runtimeException = new RuntimeException("testErrorExecutionObserverCallbacks");
                        }
                    };

                    mockery.checking(new Expectations() {{
                            one(observer).pending(execution);
                            one(observer).started(execution);
                            one(observer).error(execution, runtimeException);
                            one(observer).done(execution);
                        }
                    });

                    DefaultCommand c = new DefaultCommand() {
                        protected CommandExecution createExecution() {
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

        final CommandExecution execution = new DefaultExecution() {
            public void doInEventThread() throws Exception {
                calledInEventThread = SwingUtilities.isEventDispatchThread();
                countDownLatch.countDown();
            }
        };

         DefaultCommand c = new DefaultCommand() {
            protected CommandExecution createExecution() {
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
