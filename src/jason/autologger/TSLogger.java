package jason.mod;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Author: Fahid RM
 * Note: This class was added with the purpose of listening to activities of an Agent's transition system.
 *
 *
 */
public class TSLogger implements TSListener{

    private static TSLogger instance;
    private ArrayList<TSListener> listeners;

    /**
     * getInstance
     * Returns instance of class
     *
     * @return class instance
     */
    public static TSLogger getInstance() {
        if (instance == null) {
            instance = new TSLogger();
            instance.listeners = new ArrayList<>();
        }
        return instance;
    }


    /**
     * register
     * Registers a new listener to the logger
     *
     * @param listener listener instance
     */
    public void register (TSListener listener) {
        if (listener == null) { return; }
        listeners.add(listener);
    }

    /**
     * unregister
     * Unregisters a listener from the logger
     *
     * @param listener listener instance
     */
    public void unregister (TSListener listener) {
        listeners.remove(listener);
    }

    /**
     * @see TSListener#log(String, String, Object[], int)
     */
    public void log(String agent, String circumstance, Object[] circumstanceMetaData, int reasoningCycle) {
        for (Iterator<TSListener> iterator = listeners.iterator(); iterator.hasNext();) {
            TSListener listener = iterator.next();
            listener.log(agent, circumstance, circumstanceMetaData, reasoningCycle);
        }
    }

}
