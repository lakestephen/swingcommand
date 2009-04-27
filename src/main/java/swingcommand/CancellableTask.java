/*
 * Copyright 2009 Object Definitions Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 27-Apr-2009
 * Time: 22:29:28
 * To change this template use File | Settings | File Templates.
 */
public abstract class CancellableTask extends BackgroundTask {

    private volatile boolean isCancelled;
    private boolean cancelCalled;
    private boolean isStarted, isFinished;
    private Thread backgroundThread;
    private final Object localLock = new Object();

    protected void doBackgroundProcessing() throws Exception {
        try {
            synchronized (localLock) {
                backgroundThread = Thread.currentThread();
                isStarted = true;
            }
            if ( ! isCancelled() ) {
                try {
                    doInBackground();
                } catch ( Exception e ) {
                    if ( cancelCalled) {
                        isCancelled = true;
                    } else {
                        //this was not caused by a cancellation therefore
                        //we re-throw the error
                        throw e;
                    }
                }
            }
        } finally {
            synchronized (localLock) {
                backgroundThread = null;
                isCancelled |= (cancelCalled && Thread.interrupted());
                isFinished = true;
            }
        }
    }

     /**
     * The Subclass should implement this method to perform interruptible processing
     * This method is called in a background thread
     *
     * If cancel() is called on the task, the background Thread will be interrupted
     *
     * Subclasses may throw an InterruptedException if a blocking call is interrupted, or may check for cancellation programatically
     * To check for cancellation programatically, call the isInterrupted() method periodically and simply break processing and return if isInterrupted() ever returns true
     *
     * Once this method exits isCancelled() will then be true for this task either if:
     * a) This method threw InterruptedExecption
     * b) When this method returns Thread.interrupted() is true for this thread
     *
     * @throws Exception , InterruptedException
     */
    protected abstract void doInBackground() throws Exception;


    public boolean isCancelled() {
        return isCancelled;
    }

    public void cancel() {
        boolean cancelledThisTime = false;
        synchronized(localLock) {
            cancelCalled = true;
            if ( ! isStarted) {
                isCancelled = true;
            } else if ( ! isFinished ) {
                backgroundThread.interrupt();
                cancelledThisTime = true;
            }
        }

        //give the subclass a chance to do some extra interrupting
        if ( cancelledThisTime) {
            doInterrupt();
        }
    }

    /**
     * If calling Thread.interrupt() is not sufficient to interrupt the operation
     * subclasses may override this method to perform extra actions (e.g Cancel an executing Statement)
     */
    protected void doInterrupt() {
    }

    protected boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    protected final void doInEventThread() throws Exception {
        doEvenIfCancelled();
        if ( ! isCancelled()) {
            doInEventThreadIfNotCancelled();
        }
    }

    protected abstract void doInEventThreadIfNotCancelled() throws Exception;

    /**
     * Subclasses may override this method if it is necessary to do event thread work
     * even after cancellation
     */
    protected void doEvenIfCancelled() throws Exception {}

}
