package com.bladecoder.engine.model;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;

import java.util.ArrayList;

/**
 * Interface to define the methods needed to handle and execute verbs.
 *
 * @author rgarcia
 */
public interface VerbRunner extends ActionCallback {

    /**
     * Method to retrieve the action list
     *
     * @return the action list
     */
    ArrayList<Action> getActions();

    /**
     * Run the verb
     *
     * @param currentTarget the target for the 'use' verb.
     */
    void run(String currentTarget, ActionCallback cb);

    /**
     * Return the current action pointer
     */
    int getIP();

    /**
     * Sets the action pointer
     *
     * @param ip the pointer (the action index to execute)
     */
    void setIP(int ip);

    /**
     * Sets the IP to the end of the queue finishing the verb execution
     */
    void cancel();


    /**
     * The target in 'use' verb.
     *
     * @return The target actor 'id'
     */
    String getCurrentTarget();
}
