package jason.mod;

/**
 * Author: Fahid
 * Note: Listens to events in the transition system
 */
public interface TSListener {
    /**
     * log
     * Logs and event that occurred in the Transition System
     *
     * @param agent                 Agent being observed
     * @param circumstance          Circumstance / Event that occurred
     * @param circumstanceMetaData  Meta Data associated with the circumstance
     * @param reasoningCycle        Reasoning cycle no.
     */
    void log(String agent, String circumstance, Object[] circumstanceMetaData, int reasoningCycle);
}
