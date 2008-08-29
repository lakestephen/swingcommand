package com.od.swing.command;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Aug-2008
 * Time: 17:43:43
 */
public interface CompositeExecution extends CancelableExecution {

   public CommandExecution getCurrentChildExecution();

   public int getCurrentChildId();

   public int getTotalChildren();
    
}
