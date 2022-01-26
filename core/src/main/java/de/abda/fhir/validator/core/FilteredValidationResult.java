package de.abda.fhir.validator.core;

import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.filter.FilterResult;

import java.util.List;

/**
 * Container object, which contains the list of ValidationMessages that remain after applying the custom filter rules
 * as well as a {@link FilterResult} which may hold a list of messages that have been filtered out
 */
public class FilteredValidationResult {
    private final FilterResult filterResult;
    private final List<SingleValidationMessage> validationMessages;

    public FilteredValidationResult(List<SingleValidationMessage> validationMessages, FilterResult filterResult) {
        this.validationMessages = validationMessages;
        this.filterResult = filterResult;
    }

    public boolean isValid() {
        return validationMessages.size() == 0;
    }

    /**
     * List of {@link SingleValidationMessage} that remain after filtering
     *
     * @return List of messages which have not been matched by a filter
     */
    public List<SingleValidationMessage> getValidationMessages() {
        return validationMessages;
    }

    public FilterResult getFilterResult() {
        return filterResult;
    }
}
