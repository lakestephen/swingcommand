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

    private LifeCycleMonitoringSupport<AbstractCommand> lifeCycleMonitoringSupport = new LifeCycleMonitoringSupport<AbstractCommand>();
    private String startMessage = "Starting command " + getClass().getName();
    private String stopMessage = "Stopped command " + getClass().getName();
    private String errorMessage = "Error executing command " + getClass().getName();

    protected AbstractCommand() {
    }

    public void setMessages(String startMessage, String stopMessage) {
        this.startMessage = startMessage;
        this.stopMessage = stopMessage;
    }

    public void setStartMessage(String startMessage) {
        this.startMessage = startMessage;
    }

    public void setStopMessage(String stopMessage) {
        this.stopMessage = stopMessage;
    }

    public final void execute() {
        fireStarted(startMessage);
        try {
            doExecute();
            //nb. during the execute the subclass may fire step reached events
        }
        catch (Throwable t) {
            t.printStackTrace();
            fireError(errorMessage, t);
        }
        finally {
            fireEnded(stopMessage);
        }
    }

    /**
     * Called in the Swing Event thread
     * The subclass should implement this method to execute the command
     */
    protected abstract void doExecute() throws Exception;


    ////////////////////////////////////////////////////////////////////////////
    ////////////// Now the delegate methods to the life cycle monitoring support
    private void fireEnded(String endMessage) {
        LifeCycleMonitoringSupport.fireEnded(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), this, endMessage);
    }

    private void fireError(String errorMessage, Throwable t) {
        LifeCycleMonitoringSupport.fireError(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), this, errorMessage, t);
    }

    private void fireStarted(String startMessage) {
        LifeCycleMonitoringSupport.fireStarted(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), this, startMessage);
    }

    public void fireStepReached(int currentStep, int totalStep, String stepMessage) {
        LifeCycleMonitoringSupport.fireStepReached(lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot(), this, currentStep, totalStep, stepMessage);
    }

    public void addLifeCycleMonitor(CommandLifeCycleMonitor<AbstractCommand> lifeCycleMonitor) {
        lifeCycleMonitoringSupport.addLifeCycleMonitor(lifeCycleMonitor);
    }

    public void removeLifeCycleMonitor(CommandLifeCycleMonitor<AbstractCommand> lifeCycleMonitor) {
        lifeCycleMonitoringSupport.removeLifeCycleMonitor(lifeCycleMonitor);
    }
}
