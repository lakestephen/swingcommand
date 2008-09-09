package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Sep-2008
 * Time: 14:50:38
 */
public interface Undoable {

    /**
    * Call this method to request execution be undone
    */
    void undo();

    /**
     * @return true, if this execution can be undone
     */
    boolean isUndoable();

    /**
     * @return true, if the command was undone
     */
    boolean isUndone();
}
