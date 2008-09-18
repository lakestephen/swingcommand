package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 27-Jun-2008
 * Time: 17:43:57
 */
public interface AsynchronousExecution extends CommandExecution, Cancellable {

    /**
     * Called in a background thread during command execution, to do the asynchronous task processing
     * Command classes should implement this method to do the asynchronous processing required
     * before doInEventThread is called to update the UI
     *
     * @throws Exception, to abort execution if an error condition occurs
     */
    void doInBackground() throws Exception;
}
