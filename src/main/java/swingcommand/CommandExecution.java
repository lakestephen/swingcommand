package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 30-Aug-2008
 * Time: 00:40:16
 * To change this template use File | Settings | File Templates.
 */
public interface CommandExecution extends Cancellable, Undoable {

     /**
     * Called in the Swing event thread to execute the command
      *
     * For asynchronous commands this gives the command a chance to update the
     * Swing UI views & models once asynchronous processing is completed
     *
     * @throws Exception, to abort execution if an error condition occurs
     */
    void doInEventThread() throws Exception;

}
