/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
