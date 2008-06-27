package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Some commands 
 * User: nick
 * Date: 24-May-2007
 * Time: 18:32:15
 *
 * Strangely it is sometimes helpful for an Asynchronous command to take no less than a defined minimum amount of time to run
 * This is for ui feedback purposes - the user gets visual feedback that the async task is executing
 * Otherwise if processing is very fast the user may be in doubt that the command actually executed.
 */
public abstract class CommandExecutionWithMinRuntime<E extends CommandExecution> implements CommandExecution {

    public static final int SHORT_MIN_RUNTIME = 300;
    public static final int DEFAULT_MIN_RUNTIME = 1500;
    private int minRuntime = DEFAULT_MIN_RUNTIME;

    public void setMinRuntime(int minRuntime) {
        this.minRuntime = minRuntime;
    }

    public final void doExecuteAsync() throws Exception {
        long startTime = System.currentTimeMillis();

        timedExecuteAsync();

        long endTime = System.currentTimeMillis();
        long diff = endTime - startTime;
        if (diff < minRuntime) {
            Thread.sleep(minRuntime - diff);
        }
    }

    protected abstract void timedExecuteAsync() throws Exception;
}
