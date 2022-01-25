package de.abda.fhir.validator.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.filter.*;
import de.abda.fhir.validator.core.filter.regex.FilterBeschreibungsListe;
import de.abda.fhir.validator.core.filter.regex.RegExMessageFilter;
import de.abda.fhir.validator.core.util.FileHelper;
import de.abda.fhir.validator.core.util.Profile;
import de.abda.fhir.validator.core.util.ProfileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the default class to use the ABDA FHIR validator in your Java Application.
 * To validate a File, you can use the {@link #validateFile(Path)}, if the file content is
 * already read as String, you can use {@link #validateString(String)}. The profile data is loaded
 * on demand the first time a profile version is used. Further invocations use the already
 * loaded data.
 *
 * <p>ReferenceValidator is currently NOT threadsafe, but it can be reused for validating
 * further FHIR resources in the same or another thread.</p>
 */
public class ReferenceValidator {
    private static final String MESSAGE_NO_FILTER_DEFINED = "Es wurde keine Filter für das Profil {} definiert.";
    public static final String VALIDATION_FILTER_PROPERTIES = "/validationFilter/validationFilter.properties";
    static Logger logger = LoggerFactory.getLogger(Validator.class);
    private static final String ERROR_CANT_READ_PROPERTIES = "Fehler beim Einlesen der Properties aus der Quelle:{}).";
    private final Properties profileFilters;
    private final Map<Profile, Optional<MessageFilter>> messageFilters = new HashMap<>();
    private final FhirContext ctx;
    private final ValidatorHolder validatorHolder;

    /**
     * Creates a new instance without parameters.
     */
    public ReferenceValidator() {
        this(FhirContext.forR4());
    }

    /**
     * Creates a new instance using an existing FhirContext.
     *
     * @param ctx {@link FhirContext}, not null
     */
    public ReferenceValidator(FhirContext ctx) {
        this.ctx = ctx;
        validatorHolder = new ValidatorHolder(ctx);
        profileFilters = new Properties();
        loadValidationFilterProperties();
    }

    /**
     * Loads a properties file which defines which {@link  FilterBeschreibungsListe} is to be used for each FHIR profile.
     * The key of each property is the profile including the profile version (if present). The value is the path
     * to the XMl file containing the filter rules.
     */
    private void loadValidationFilterProperties() {
        URL propertiesUrl = this.getClass().getResource(VALIDATION_FILTER_PROPERTIES);
        try (InputStream is = propertiesUrl.openStream()) {
            profileFilters.load(is);
        } catch (IOException|NullPointerException e) {
            throw new RuntimeException(String.format(ERROR_CANT_READ_PROPERTIES, propertiesUrl), e);
        }
    }

    /**
     * Validates the given File
     *
     * @param inputFile Path, not null
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public Map<ResultSeverityEnum, List<SingleValidationMessage>> validateFile(String inputFile) {
        logger.debug("Start validating File {}", inputFile);
        String validatorInputAsString = FileHelper.loadValidatorInputAsString(inputFile, false);
        return this.validateImpl(validatorInputAsString);
    }

    /**
     * Validates the given File
     *
     * @param inputFile String path, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public Map<ResultSeverityEnum, List<SingleValidationMessage>> validateFile(Path inputFile) {
        return validateFile(inputFile.toString());
    }

    /**
     * Validates the given String containing a FHIR resouce
     *
     * @param validatorInputAsString String, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public Map<ResultSeverityEnum, List<SingleValidationMessage>> validateString(String validatorInputAsString) {
        logger.debug("Start validating String input");
        return validateImpl(validatorInputAsString);
    }

    /**
     * The first validation in a new validator is very slow. So this method creates validators
     * for all supported profiles and loads all necessary data, so the calls to the validator
     * will be fast afterwards.
     *
     * @param profileToPreload a varags array of profiles, that will be preloaded. If this is null or
     *                         empty, then all profiles will be preloaded
     */
    public void preloadAllSupportedValidators(ProfileForPreloading... profileToPreload) {
        validatorHolder.preloadAllSupportedValidators(profileToPreload);
    }

    private Map<ResultSeverityEnum, List<SingleValidationMessage>> validateImpl(String validatorInputAsString) {
        InputStream validatorInputStream = new ByteArrayInputStream(validatorInputAsString.getBytes(StandardCharsets.UTF_8));
        Profile profile = ProfileHelper.getProfileFromXmlStream(validatorInputStream);
        Validator validator = validatorHolder.getValidatorForProfile(profile);
        Map<ResultSeverityEnum, List<SingleValidationMessage>> messageMap = validator.validate(validatorInputAsString);

        //filter  List of ValidationMessages if a filter for the given profile is defined
        Optional<MessageFilter> filter = loadFilterForProfile(profile);
        if (filter.isPresent()) {
            List<SingleValidationMessage> filteredList = messageMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            filter.get().filter(filteredList);
            return recreateMap(filteredList);
        } else {
            return messageMap;
        }

    }

    private Map<ResultSeverityEnum, List<SingleValidationMessage>> recreateMap(List<SingleValidationMessage> messages) {
        return messages.stream().collect(Collectors.groupingBy(SingleValidationMessage::getSeverity, Collectors.toList()));
    }

    /**
     * Returns an optional {@link MessageFilter} for the passed in {@link Profile} which can be used to filter a List of
     * Validation Messages.
     * @param profile the profile
     * @return an Optional containing the {@link MessageFilter} for the profile or <code>Optional.empty</code> if no filter
     * for the profile was defined.
     */
    private Optional<MessageFilter> loadFilterForProfile(Profile profile) {
        Optional<MessageFilter> filter;
        if (messageFilters.containsKey(profile)) {
            filter = messageFilters.get(profile);
        } else {
            String profileAndVersion = profile.getCanonical().substring(profile.getCanonical().lastIndexOf('/') + 1);

            String filterXML = profileFilters.getProperty(profileAndVersion);
            if (filterXML != null) {
                URL url = this.getClass().getResource(filterXML);
                filter = Optional.of(new RegExMessageFilter(url));
            } else {
                logger.info(MESSAGE_NO_FILTER_DEFINED, profile.getCanonical());
                filter = Optional.empty();
            }
            messageFilters.put(profile, filter);
        }
        return filter;

    }
}
