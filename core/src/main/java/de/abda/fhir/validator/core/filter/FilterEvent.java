package de.abda.fhir.validator.core.filter;

import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.filter.regex.FilterDefinition;

/**
 * Holds a message that was filtered out and the filter definition that was used for filtering
 * @author Frank Jesgarz
 */
public class FilterEvent {
    private final FilterDefinition filterDefinition;
    private final SingleValidationMessage message;

    /**
     * Constructor
     * @param filterDefinition the filterDefinition that was responisble for filtering out the message
     * @param message the message that was filtered
     */
    public FilterEvent(FilterDefinition filterDefinition, SingleValidationMessage message) {
        this.filterDefinition = filterDefinition;
        this.message = message;
    }

    /**
     *
     * @return the filterDefinition that was responisble for filtering out the message
     */
    public FilterDefinition getFilterDefinition() {
        return filterDefinition;
    }

    /**
     *
     * @return the message that was filtered
     */
    public SingleValidationMessage getFilteredMessage() {
        return message;
    }
}
