package de.abda.fhir.validator.core;

import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.filter.FilterEvent;
import de.abda.fhir.validator.core.filter.MessageFilter;
import de.abda.fhir.validator.core.util.Profile;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Container object, which contains the list of ValidationMessages that remain after applying the custom filter rules
 * as well as a List of {@link de.abda.fhir.validator.core.filter.FilterEvent} which may hold a list of messages that have been filtered out.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {"valid", "validationMessages", "filterEvents", "profile"})
public class ValidationResult {
    private List<SingleValidationMessage> validationMessages;
    private MessageFilter messageFilter;
    private List<FilterEvent> filterEvents;
    private boolean valid;
    private Profile profile;

    public ValidationResult() {
    }

    public ValidationResult(List<SingleValidationMessage> validationMessages, MessageFilter messageFilter, List<FilterEvent> filterEvents, Profile profile) {
        this.validationMessages = validationMessages;
        this.messageFilter = messageFilter;
        this.filterEvents = filterEvents;
        this.valid = validationMessages.size() == 0;
        this.profile = profile;
    }

    /**
     * States if the validation result is valid. The result is valid, only if {@link #getValidationMessages()} does not contain
     * any messages. This implementation differs from {@link ca.uhn.fhir.validation.ValidationResult#isSuccessful()} where
     * the list may contain messages with a severity equal or lower than  {@link ca.uhn.fhir.validation.ResultSeverityEnum#WARNING}
     * @return true if valid, false otherwise.
     */
    @XmlElement
    public boolean isValid() {
        return this.valid;
    }

    /**
     * List of {@link SingleValidationMessage} that remain after filtering.
     *
     * @return List of messages which have not been matched by a filter.
     */
    @XmlElementWrapper
    public List<SingleValidationMessage> getValidationMessages() {
        return validationMessages;
    }

    /**
     *
     * @return the message filter that was responsible for the filter events.
     */
    public MessageFilter getMessageFilter() {
        return messageFilter;
    }

    /**
     *
     * @return List of filter events that were produced during filtering.
     */
    @XmlElementWrapper
    @XmlElement(name = "filterEvent")
    public List<FilterEvent> getFilterEvents() {
        return filterEvents;
    }

    /**
     * The profile that was used for the validation of the input.
     * @return the profile never <code>NULL</code>.
     */
    @XmlElement
    public Profile getProfile() {
        return profile;
    }
}
