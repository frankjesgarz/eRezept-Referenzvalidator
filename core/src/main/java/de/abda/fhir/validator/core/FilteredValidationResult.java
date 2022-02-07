package de.abda.fhir.validator.core;

import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.filter.FilterEvent;
import de.abda.fhir.validator.core.filter.MessageFilter;
import de.abda.fhir.validator.core.filter.regex.MessageFilterTypeAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;

/**
 * Container object, which contains the list of ValidationMessages that remain after applying the custom filter rules
 * as well as a List of {@link de.abda.fhir.validator.core.filter.FilterEvent} which may hold a list of messages that have been filtered out
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {"valid", "validationMessages", "messageFilter", "filterEvents"})
public class FilteredValidationResult {
    private List<SingleValidationMessage> validationMessages;
    private MessageFilter messageFilter;
    private List<FilterEvent> filterEvents;
    private boolean valid;

    public FilteredValidationResult() {
    }

    public FilteredValidationResult(List<SingleValidationMessage> validationMessages, MessageFilter messageFilter, List<FilterEvent> filterEvents) {
        this.validationMessages = validationMessages;
        this.messageFilter = messageFilter;
        this.filterEvents = filterEvents;
        this.valid = validationMessages.size() == 0;
    }
    @XmlElement
    public boolean isValid() {
        return this.valid;
    }

    /**
     * List of {@link SingleValidationMessage} that remain after filtering
     *
     * @return List of messages which have not been matched by a filter
     */
    @XmlElementWrapper
    public List<SingleValidationMessage> getValidationMessages() {
        return validationMessages;
    }

    /**
     *
     * @return the message filter that was responsible for the filter events
     */
    @XmlElement
    @XmlJavaTypeAdapter(MessageFilterTypeAdapter.class)
    public MessageFilter getMessageFilter() {
        return messageFilter;
    }

    /**
     *
     * @return List of filter events that were produced during filtering
     */
    @XmlElementWrapper
    public List<FilterEvent> getFilterEvents() {
        return filterEvents;
    }
}
