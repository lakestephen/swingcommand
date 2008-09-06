package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 29-Aug-2008
 * Time: 23:37:05
 * To change this template use File | Settings | File Templates.
 */
public class SwingCommandRuntimeException extends RuntimeException {

    public SwingCommandRuntimeException(String message) {
        super(message);
    }

    public SwingCommandRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
