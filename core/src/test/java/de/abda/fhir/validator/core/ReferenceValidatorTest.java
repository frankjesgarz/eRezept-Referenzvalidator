package de.abda.fhir.validator.core;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.exception.ValidatorInitializationException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.tuple.Pair.of;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unittest for {@link ReferenceValidator}
 *
 * @author RupprechJo
 */
class ReferenceValidatorTest {

    private static final Path VALID_BASE_DIR = Paths.get("src/test/resources/valid");
    private static final Path INVALID_BASE_DIR = Paths.get("src/test/resources/invalid");
    private static final Path INVALID_BULK_DIR = Paths.get("src/test/resources/invalid/bulk");
    private static final Path EXCEPTION_BASE_DIR = Paths.get("src/test/resources/exception");

    static ReferenceValidator validator;

    static Logger logger = LoggerFactory.getLogger(ReferenceValidatorTest.class);

    @BeforeAll
    static void setupClass(){
        //Locale.setDefault(new Locale("en", "EN"));
        validator = new ReferenceValidator();
    }

    /**
     * Stellt sicher das der ReferenceValidator für Beispieldateien, die wir für valide halten, keinerlei Validierungsmeldungen zurück gibt.
     * @param path Die Datei die validiert werden soll
     */
    @ParameterizedTest
    @MethodSource
    void validateValidFile(Path path) {
        Map<ResultSeverityEnum, List<SingleValidationMessage>> resultMap = validator
                .validateFile(path);

        List<SingleValidationMessage> messages = getMessagesWithSeverity(resultMap, Arrays.asList(ResultSeverityEnum.values()));
        if(messages.size() > 0){
            logger.warn("Es sollten keine Validierungsmeldungen gefunden werden, es wurden aber {} zurückgegeben.", messages.size());
            String mapAsString = resultMap.keySet().stream()
                .map(key -> key + ": " + resultMap.get(key).size())
                .collect(Collectors.joining(","));
            logger.warn("Validierungsmeldungen: {}", mapAsString);
        }
        assertEquals(0, messages.size());
    }

    /**
     * Stellt sicher das der ReferenceValidator für Beispieldateien, die wir für invalide halten Validierungsmeldungen zurück gibt.
     * @param path Die Datei die validiert werden soll
     */
    @ParameterizedTest
    @MethodSource
    void validateInvalidFiles(Path path) {
        Map<ResultSeverityEnum, List<SingleValidationMessage>> resultMap = validator
                .validateFile(path);
        List<SingleValidationMessage> messages = getMessagesWithSeverity(resultMap, Arrays.asList(ResultSeverityEnum.values()));
        assertNotEquals(0, messages.size());
    }

    /**
     * Test zum identifizieren von Info-Messages bei invaliden Dateien. Hypothese: Infomeldungen sollten nicht kritisch sein
     * und auf einer Whitelist landen
     * @param path Pfad zur Tetsdatei
     */
    @ParameterizedTest
    @MethodSource(value = "validateInvalidFiles")
    void getInfoMessagesForInvalidFiles(Path path) {
        Map<ResultSeverityEnum, List<SingleValidationMessage>> resultMap = validator
                .validateFile(path);
        List<SingleValidationMessage> messages = getMessagesWithSeverity(resultMap, Arrays.asList(ResultSeverityEnum.INFORMATION));
        messages.forEach(msg -> logger.warn("Datei:{}, Meldung:'{}', path:'{}', severity:{}, Zeile:{}, Spalte:{}", path, msg.getMessage(), msg.getLocationString(), msg.getSeverity(), msg.getLocationLine(), msg.getLocationCol()));
    }

    /**
     * Test zum identifizieren von Warn-Messages bei invaliden Dateien. Hypothese: Warnungen sollten kritisch sein
     * @param path Pfad zur Testdatei
     */
    @ParameterizedTest
    @MethodSource(value = "validateInvalidFiles")
    void getWarningMessagesForInvalidFiles(Path path) {
        logger.info("Testdatei:{}", path);
        Map<ResultSeverityEnum, List<SingleValidationMessage>> resultMap = validator
                .validateFile(path);
        List<SingleValidationMessage> messages = getMessagesWithSeverity(resultMap, Arrays.asList(ResultSeverityEnum.WARNING));
        messages.forEach(msg -> logger.warn("Datei:{}, Meldung:'{}', path:'{}', severity:{}, Zeile:{}, Spalte:{}", path, msg.getMessage(), msg.getLocationString(), msg.getSeverity(), msg.getLocationLine(), msg.getLocationCol()));
    }

    private static Stream<Path> validateInvalidFiles() throws IOException {
        return Files.walk(INVALID_BULK_DIR).filter(path -> path.toString().endsWith(".xml"));
    }


    private static Stream<Path> validateValidFile() throws IOException {
        return Files.walk(VALID_BASE_DIR).filter(path -> path.toString().endsWith(".xml"));
    }

    @ParameterizedTest
    @MethodSource
    void validateInvalidFile(Pair<Path, String> arguments) {
        Map<ResultSeverityEnum, List<SingleValidationMessage>> errors = validator
                .validateFile(arguments.getKey());
        String mapAsString = errors.keySet().stream()
                .map(key -> key + ": " + errors.get(key).size())
                .collect(Collectors.joining(","));
        System.out.println(mapAsString);
        List<SingleValidationMessage> errorMessages = getFatalAndErrorMessages(errors);
        assertNotEquals(0, errorMessages.size());
        assertTrue(errorMessages.stream()
                .anyMatch(message -> message.getMessage().contains(arguments.getValue())));
    }

    private static Stream<Pair<Path, String>> validateInvalidFile() {
        return Stream.of(
                of(INVALID_BASE_DIR.resolve("InvalidEprescriptionBundle1.xml"),
                        "Der Wert ist \"https://fhir.kbv.de/CodeSystem/Wrong\", muss aber \"https://fhir.kbv.de/CodeSystem/KBV_CS_ERP_Section_Type\" sein")
        );
    }

    @ParameterizedTest
    @MethodSource
    void validateInvalidFileBulk(Path path) {
        Map<ResultSeverityEnum, List<SingleValidationMessage>> resultMap = validator
                .validateFile(path);
//        String mapAsString = resultMap.keySet().stream()
//                .map(key -> key + ": " + resultMap.get(key).size())
//                .collect(Collectors.joining(","));
//        System.out.println(mapAsString);
        List<SingleValidationMessage> messages = getMessagesWithSeverity(resultMap, Arrays.asList(ResultSeverityEnum.ERROR, ResultSeverityEnum.FATAL));
        if(messages.size() == 0){
            logger.warn("Es sollten Validierungsmeldungen gefunden werden.");
        }
        assertNotEquals(0, messages.size());
    }

    private static Stream<Path> validateInvalidFileBulk() throws IOException {
        return Files.walk(INVALID_BULK_DIR).filter(path -> path.toString().endsWith(".xml"));
    }

    @ParameterizedTest
    @MethodSource
    void validateFileWithException(Pair<Path, Class<? extends Exception>> arguments) {
        Assertions.assertThrows(arguments.getRight(), () -> validator.validateFile(arguments.getKey()));
    }

    private static Stream<Pair<Path, Class<? extends Exception>>> validateFileWithException() {
        return Stream.of(
                of(EXCEPTION_BASE_DIR.resolve("NotExistingBundle.xml"),
                        ValidatorInitializationException.class),
                of(EXCEPTION_BASE_DIR.resolve("NotExistingBundleVersion.xml"),
                        ValidatorInitializationException.class)

        );
    }

    private List<SingleValidationMessage> getFatalAndErrorMessages(
            Map<ResultSeverityEnum, List<SingleValidationMessage>> errors) {
        List<SingleValidationMessage> result = new ArrayList<>(
                errors.getOrDefault(ResultSeverityEnum.ERROR,
                        Collections.emptyList()));
        result.addAll(errors.getOrDefault(ResultSeverityEnum.FATAL, Collections.emptyList()));
        return result;
    }

    /**
     * Gibt alle SingleValidationMessages in der Map mit der gesuchten Severity zurück
     *
     * @param resultMap  die Map die durchsucht werden soll
     * @param severities Die gesuchten Severities
     * @return Liste mit SingleValidationMessages
     */
    private List<SingleValidationMessage> getMessagesWithSeverity(
            Map<ResultSeverityEnum, List<SingleValidationMessage>> resultMap, List<ResultSeverityEnum> severities) {
        return severities.stream().flatMap(severity -> resultMap.getOrDefault(severity, Collections.emptyList()).stream()).collect(Collectors.toList());

    }

}
