package de.abda.fhir.validator.core.filter;

import ca.uhn.fhir.validation.ValidationResult;
import de.abda.fhir.validator.core.FilteredValidationResult;
import de.abda.fhir.validator.core.filter.regex.FilterDefinitionList;
import de.abda.fhir.validator.core.filter.regex.NonFilteringMessageFilter;
import de.abda.fhir.validator.core.filter.regex.RegExMessageFilter;
import de.abda.fhir.validator.core.util.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class FilterRules {
    private static final String MESSAGE_NO_FILTER_DEFINED = "Es wurde keine Filter für das Profil {} definiert.";
    public static final String VALIDATION_FILTER_PROPERTIES = "/de/abda/fhir/validator/core/filter/regex/validationFilter.properties";
    private static final String ERROR_CANT_READ_PROPERTIES = "Fehler beim Einlesen der Properties aus der Quelle:{}).";
    private static final Logger logger = LoggerFactory.getLogger(FilterRules.class);
    private Properties profileFilters;
    private final Map<Profile, MessageFilter> messageFilters = new HashMap<>();

    /**
     * Constructor
     */
    public FilterRules() {
        loadValidationFilterProperties();
    }


    /**
     * Loads a properties file which defines which {@link  FilterDefinitionList} is to be used for each FHIR profile.
     * The key of each property is the profile including the profile version (if present). The value is the path
     * to the XMl file containing the filter rules.
     */
    private void loadValidationFilterProperties() {
        profileFilters = new Properties();
        URL propertiesUrl = Objects.requireNonNull(this.getClass().getResource(VALIDATION_FILTER_PROPERTIES));
        try (InputStream is = propertiesUrl.openStream()) {
            profileFilters.load(is);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException(String.format(ERROR_CANT_READ_PROPERTIES, propertiesUrl), e);
        }
    }

    /**
     * Filters out the validation messages of the validationResult according to the defined filter rules
     * and returns a {@link FilteredValidationResult}, that contains the remaining validation messages as well as a {@link FilterResult},
     * which provides further information for the filtered out messages
     * @param profile the profile according to which the imput resource was filtered
     * @param validationResult the validation result
     * @return the result of the filtering operations
     */
    public FilteredValidationResult filterMessages(Profile profile, ValidationResult validationResult) {
        MessageFilter messageFilter = loadFilterForProfile(profile);
        return messageFilter.filter(new ArrayList<>(validationResult.getMessages()));
    }

    /**
     * Returns an {@link MessageFilter} for the passed in {@link Profile} which can be used to filter a List of
     * Validation Messages.
     *
     * @param profile the profile
     * @return a {@link MessageFilter} for the profile
     */
    private MessageFilter loadFilterForProfile(Profile profile) {
        MessageFilter filter;
        if (messageFilters.containsKey(profile)) {
            filter = messageFilters.get(profile);
        } else {
            String profileAndVersion = profile.getCanonical().substring(profile.getCanonical().lastIndexOf('/') + 1);

            String filterXML = profileFilters.getProperty(profileAndVersion);
            if (filterXML != null) {
                URL url = this.getClass().getResource(filterXML);
                filter = new RegExMessageFilter(url);
            } else {
                logger.info(MESSAGE_NO_FILTER_DEFINED, profile.getCanonical());
                filter = new NonFilteringMessageFilter();
            }
            messageFilters.put(profile, filter);
        }
        return filter;

    }
}
