package de.abda.fhir.validator.core.filter.regex;

import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.FilteredValidationResult;
import de.abda.fhir.validator.core.filter.FilterResult;
import de.abda.fhir.validator.core.filter.MessageFilter;

import java.util.Collections;
import java.util.List;

/**
 * This implementation simply returns a {@link FilteredValidationResult} without applying any filtering
 */
public class NonFilteringMessageFilter implements MessageFilter {
    private final FilterResult filterResult = new FilterResult(this, Collections.emptyList());

    /**
     *
     * @param messages The List of messages to be filtered. Must not be <code>null</code>.
     * @return unfiltered Result
     */
    @Override
    public FilteredValidationResult filter(List<SingleValidationMessage> messages)  {
        return new FilteredValidationResult(messages, filterResult);
    }
}