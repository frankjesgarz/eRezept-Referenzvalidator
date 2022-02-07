package de.abda.fhir.validator.core;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.filter.FilterEvent;
import de.abda.fhir.validator.core.filter.MessageFilter;
import de.abda.fhir.validator.core.filter.regex.FilterDefinition;
import de.abda.fhir.validator.core.filter.regex.NonFilteringMessageFilter;
import de.abda.fhir.validator.core.filter.regex.RegExMessageFilter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrationtest  for the {@link ReferenceValidator}.
 * This tests verifies, that the method {@link ReferenceValidator#validateFileX(Path)} makes use of the
 * RegexImplementation of {@link MessageFilter} and verifies that the FilterDefinitions declared by the xml files in the
 * resources' directory lead to the expected results.
 * @see RegExMessageFilter
 * @see FilterDefinition
 *
 * @author Frank Jesgarz
 */
class ReferenceValidatorFilterITTest {

    private static final Path VALID_BASE_DIR = Paths.get("src/test/resources/valid");
    private static final Path INVALID_BULK_DIR = Paths.get("src/test/resources/invalid/bulk");

    private static ReferenceValidator validator;
    private static final Logger logger = LoggerFactory.getLogger(ReferenceValidatorFilterITTest.class);
    private static final List<FilteredValidationResult> results = new ArrayList<>();

    @BeforeAll
    static void setupClass() {
        validator = new ReferenceValidator();
    }

    @AfterAll
    static void teardownClass() {
        assertAllDefinedFiltersMatched();
        results.clear();
        validator = null;
    }

    /**
     * Checks if all filter definitions defined in the xml files are matched at least once during the validation of all valid files.
     */
    private static void assertAllDefinedFiltersMatched() {
        //get filter results grouped by MessageFilters
        Map<MessageFilter, List<FilteredValidationResult>> messageFilterMap = results.stream().collect(Collectors.groupingBy(FilteredValidationResult::getMessageFilter));
        for (Map.Entry<MessageFilter, List<FilteredValidationResult>> entry : messageFilterMap.entrySet()) {
            //get stream of all encountered filter events
            Stream<FilterEvent> filterEventStream = entry.getValue().stream().flatMap(filterResult -> filterResult.getFilterEvents().stream());
            //get set of all filter definitions that were matched
            Set<FilterDefinition> matchedFilterDefinitions = filterEventStream.map(FilterEvent::getFilterDefinition).collect(Collectors.toSet());

            //get set of filters that were defined by the RegExMessageFilters and calculate relative component
            RegExMessageFilter regExMessageFilter = (RegExMessageFilter) entry.getKey();
            Set<FilterDefinition> nonMatchedFilterDefinitions = regExMessageFilter.getFilterDefinitions().stream().filter(val -> !matchedFilterDefinitions.contains(val)).collect(Collectors.toSet());
            assertTrue(() -> nonMatchedFilterDefinitions.size() == 0, () -> {
                String template = "%d unmatched FilterDefinitions found for issueIds [%s] defined id %s";
                return String.format(template, nonMatchedFilterDefinitions.size(), nonMatchedFilterDefinitions.stream().map(FilterDefinition::getIssueId).collect(Collectors.joining(", ")), regExMessageFilter.getUrl().getFile());
            });
        }
    }


    /**
     * Verifies that the {@link ReferenceValidator} does not return any {@link SingleValidationMessage} for files expected
     * to be valid.*
     *
     * @param path test file to be validated
     */
    @ParameterizedTest
    @MethodSource
    void validateValidFile(Path path) {
        FilteredValidationResult filteredValidationResult = validator
                .validateFileX(path);
        if (!filteredValidationResult.isValid()) {
            logger.warn("Es sollten keine Validierungsmeldungen gefunden werden, es wurden aber {} zurückgegeben.", filteredValidationResult.getValidationMessages().size());
            if(filteredValidationResult.getValidationMessages().size() > 0){
                logger.warn(filteredValidationResult.getValidationMessages().stream().map(SingleValidationMessage::toString).collect(Collectors.joining(",")));
            }
        }
        results.add(filteredValidationResult);
        assertTrue(filteredValidationResult.isValid());
       // marshall(filteredValidationResult);
    }

    private void marshall(FilteredValidationResult filteredValidationResult){
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(FilteredValidationResult.class, RegExMessageFilter.class, NonFilteringMessageFilter.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            jaxbMarshaller.marshal(filteredValidationResult, sw);
            logger.warn(sw.toString());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }



    private static Stream<Path> validateValidFile() throws IOException {
        return Files.walk(VALID_BASE_DIR).filter(path -> path.toString().endsWith(".xml"));
    }


    /**
     * Verifies that the {@link ReferenceValidator#validateFileX(Path)} returns at least one {@link SingleValidationMessage} for each file
     * expected to be invalid.
     *
     * @param path the file to be validated
     */
    @ParameterizedTest
    @MethodSource
    void validateInvalidFiles(Path path) {
        FilteredValidationResult filteredValidationResult = validator.validateFileX(path);
        assertFalse(filteredValidationResult.isValid());
        assertTrue(filteredValidationResult.getValidationMessages().size() > 0);
    }

    /**
     * Test case to identify results, which only contain info messages.
     *
     * @param path Path to test file
     */
    @ParameterizedTest
    @MethodSource(value = "validateInvalidFiles")
    void getInfoMessagesForInvalidFiles(Path path) {
        FilteredValidationResult filteredValidationResult = validator
                .validateFileX(path);
        List<SingleValidationMessage> messages = getMessagesWithSeverity(filteredValidationResult, Arrays.asList(ResultSeverityEnum.INFORMATION));

        //Only log messages if the result contains only info messages
        if(filteredValidationResult.getValidationMessages().size() == messages.size()){
            logMessages(path, messages);
        };

        }

    private void logMessages(Path path, List<SingleValidationMessage> messages) {
        String msgTemplate = "Meldung:'%s', Path:'%s', severity:%s, Zeile:%d, Spalte:%d";
        Stream<String> stream = messages.stream().map(m -> String.format(msgTemplate, m.getMessage(), m.getLocationString(), m.getSeverity(), m.getLocationLine(), m.getLocationCol()));
        String formattedMessages = stream.collect(Collectors.joining(", "));
        logger.warn("Datei:{}, Meldungen:[{}]" +  path, formattedMessages);
    }



    /**
     * Test case to identify results, which contain at least one warning and only contain messages <= ResultSeverityEnum.WARNING
     *
     * @param path Pfad zur Testdatei
     */
    @ParameterizedTest
    @MethodSource(value = "validateInvalidFiles")
    void getWarningMessagesForInvalidFiles(Path path) {
        logger.info("Testdatei:{}", path);
        FilteredValidationResult filteredValidationResult = validator
                .validateFileX(path);
        List<SingleValidationMessage> messages = getMessagesWithSeverity(filteredValidationResult, Arrays.asList(ResultSeverityEnum.WARNING, ResultSeverityEnum.INFORMATION));
        if(filteredValidationResult.getValidationMessages().size() == messages.size()){
            logMessages(path, messages);
            fail();
        }
    }

    private static Stream<Path> validateInvalidFiles() throws IOException {
        return Files.walk(INVALID_BULK_DIR).filter(path -> path.toString().endsWith(".xml"));
    }

    /**
     * returns all SingleValidationMessages of the provided Map that match any of the provided severities
     *
     * @param filteredValidationResult     the result
     * @param severities the list of severities that should be returned
     * @return List of SingleValidationMessages
     */
    private List<SingleValidationMessage> getMessagesWithSeverity(
            FilteredValidationResult filteredValidationResult, List<ResultSeverityEnum> severities) {
        return filteredValidationResult.getValidationMessages().stream().filter(msg -> severities.contains(msg.getSeverity())).collect(Collectors.toList());
    }

}
