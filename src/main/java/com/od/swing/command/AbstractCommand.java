package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * A simple command designed to run synchronously in the Swing event thread
 * Logic wrapped as a command can easily be shared between different builders/adapters
 * <p/>
 * Supports LifeCycleMonitor listener instances
 */
public abstract class AbstractCommand implements Command<AbstractCommand> {

    private final LifeCycleMonitoringSupport<AbstractCommand> lifeCycleMonitoringSupport = new LifeCycleMonitoringSupport<AbstractCommand>();
    private final String commandName;

    public AbstractCommand(String commandName) {
        this.commandName = commandName;
    }

    public final void execute() {
        fireStarted();
        try {
            doExecute();
            //nb. during the execute the subclass may fire step reached events
        }
        catch (Throwable t) {
            t.printStackTrace();
            fireError(t);
        }
        finally {
            fireEnded();
        }
    }

    /**
     * Called in the Swing Event thread
     * The subclass should implement this method to execute the command
     */
    protected abstract void doExecute() throws Exception;


    ////////////////////////////////////////////////////////////////////////////
    ////////////// Now the delegate methods to the life cycle monitoring support
    private void fireEnded() {
        LifeCycleMonitoringSupport.fireEnded(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), commandName, this);
    }

    private void fireError(Throwable t) {
        LifeCycleMonitoringSupport.fireError(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), commandName, this, t);
    }

    private void fireStarted() {
        LifeCycleMonitoringSupport.fireStarted(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), commandName, this);
    }

    protected void fireStepReached() {
        LifeCycleMonitoringSupport.fireStepReached(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), commandName, this);
    }

    public void addLifeCycleMonitor(LifeCycleMonitor<AbstractCommand> lifeCycleMonitor) {
        lifeCycleMonitoringSupport.addLifeCycleMonitor(lifeCycleMonitor);
    }

    public void removeLifeCycleMonitor(LifeCycleMonitor<AbstractCommand> lifeCycleMonitor) {
        lifeCycleMonitoringSupport.removeLifeCycleMonitor(lifeCycleMonitor);
    }
}
