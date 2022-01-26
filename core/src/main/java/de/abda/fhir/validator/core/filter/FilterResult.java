package de.abda.fhir.validator.core.filter;

import java.util.List;

/**
 * Holds a reference to the message filter, which was responsible for creating the filter result and a List of FilterEvents.
 *
 * @author Frank Jesgarz
 */
public class FilterResult {
    private final MessageFilter messageFilter;
    private final List<FilterEvent> filterEvents;

    /**
     * Constructor
     * @param messageFilter the message filter that was responsible for this result
     * @param filterEvents List of filter events that were produced during filtering
     */
    public FilterResult(MessageFilter messageFilter, List<FilterEvent> filterEvents) {
        this.messageFilter = messageFilter;
        this.filterEvents = filterEvents;
    }

    /**
     *
     * @return the message filter that was responsible for this result
     */
    public MessageFilter getMessageFilter() {
        return messageFilter;
    }

    /**
     *
     * @return List of filter events that were produced during filtering
     */
    public List<FilterEvent> getFilterEvents() {
        return filterEvents;
    }
}
