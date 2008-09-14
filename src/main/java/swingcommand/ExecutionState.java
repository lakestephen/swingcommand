package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 14-Sep-2008
 * Time: 18:07:43
 * To change this template use File | Settings | File Templates.
 */
public enum ExecutionState {

    PENDING,
    STARTED,
    SUCCESS,
    ERROR;

    public boolean isFinalState() {
        return this == SUCCESS || this == ERROR;
    }
}
