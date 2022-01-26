package de.abda.fhir.validator.core.filter.regex;

import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import ca.uhn.fhir.validation.SingleValidationMessage;
import de.abda.fhir.validator.core.filter.MessageFilter;

/**
 * Default implementation of {@link MessageFilter}.
 * This implementation uses regex checks to test if the list of {@link SingleValidationMessage} needs to be filtered
 * according to a list of rules defined in {@link FilterDefinitionList}
 *
 * @author Dzmitry Liashenka
 * @author Georg Tsakumagos
 * @author Frank Jesgarz
 */
public class RegExMessageFilter implements MessageFilter {
    private final static Logger LOGGER = Logger.getLogger(RegExMessageFilter.class.getName());

    private static final String ERROR_URL_NULL = "Die übergebene URL darf nicht NULL sein.";
    private static final String ERROR_MESSAGES_NULL = "Die übergebene Liste mit SingleValidationMessages darf nicht NULL sein.";
    private static final String ERROR_CANT_READ_FILE = "Fehler beim Einlesen der FilterBeschreibungsListe aus der Quelle: '%01$s'.";
    private static final String ERROR_CREATE_MARSHALLER_UNMARSHALLER = "Fehler beim Erstellen Unmarshaller.";
    private static final String DEBUG_MESSAGE_FILTERED = "'%01$s' message wird gefiltert.";
    private static final String ERROR_WRITER_NULL = "Die übergebene Writer darf nicht NULL sein.";
    private static final String ERROR_CANT_WRITE_FILE = "Fehler beim Schreiben der FilterBeschreibungsListe.";

    private final List<FilterDefinition> filterDefinitionListe;


    /**
     * Default Konstruktor
     */
    public RegExMessageFilter() throws RuntimeException {
        this.filterDefinitionListe = Collections.emptyList();
    }

    /**
     * Konstruktor
     *
     * @param url path to an XML file with a {@link FilterDefinitionList}.
     * @throws IllegalArgumentException is the parameter is <code>null</code>.
     * @throws RuntimeException         in case of an expception while reading the file
     */
    public RegExMessageFilter(URL url) throws IllegalArgumentException, RuntimeException {
        if (null == url) {
            throw new IllegalArgumentException(ERROR_URL_NULL);
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(FilterDefinitionList.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            try (InputStream is = url.openStream()) {
                FilterDefinitionList result = jaxbUnmarshaller
                        .unmarshal(new StreamSource(is), FilterDefinitionList.class).getValue();

				if ( null == result.getFilterDefinitionList()) {
					this.filterDefinitionListe = Collections.emptyList();
				} else {
					this.filterDefinitionListe = Collections.unmodifiableList(result.getFilterDefinitionList());
				}

            } catch (Exception e) {
                throw new RuntimeException(String.format(ERROR_CANT_READ_FILE, url), e);
            }

        } catch (final Throwable e) {
            throw new RuntimeException(ERROR_CREATE_MARSHALLER_UNMARSHALLER, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filter(List<SingleValidationMessage> messages) throws IllegalArgumentException {
        if (null == messages) {
            throw new IllegalArgumentException(ERROR_MESSAGES_NULL);
        }

        for (FilterDefinition fb : this.filterDefinitionListe) {
            final Iterator<SingleValidationMessage> iterator = messages.iterator();
            while (iterator.hasNext()) {
                SingleValidationMessage next = iterator.next();
                if (doFilter(fb, next)) {
                    iterator.remove();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(String.format(DEBUG_MESSAGE_FILTERED, next));
                    }
                }
            }
        }
    }

    /**
     * Checks if the message needs to be filtered regarding the {@link FilterDefinition}
     *
     * @param filterBeschreibung      The filter definitions
     * @param singleValidationMessage the validationMessage to be checked
     * @return false, if there is no rule defined in {@link FilterDefinition or if one of the defined rules is not matched, true otherwise;
     */
    private boolean doFilter(FilterDefinition filterBeschreibung, SingleValidationMessage singleValidationMessage) {
        if (!filterBeschreibung.hasPatterns()) {
            return false;
        } else {
            return patternNullOrMatching(filterBeschreibung.getSeverityPattern(), singleValidationMessage.getSeverity().name())
                    && patternNullOrMatching(filterBeschreibung.getMessagePattern(), singleValidationMessage.getMessage())
                    && patternNullOrMatching(filterBeschreibung.getLocationPattern(), singleValidationMessage.getLocationString());
        }
    }

    private boolean patternNullOrMatching(Pattern pattern, String value) {
        return pattern == null || pattern.matcher(value).matches();
    }

    /**
     * Returns an unmodifiable List of {@link FilterDefinition} objects
     *
     * @return the list. Is never <code>null</code>.
     */
    public List<FilterDefinition> getFilterDefinitionListe() {
        return this.filterDefinitionListe;
    }

    /**
     * Marshalls the {@linkplain FilterDefinitionList} to the writer
     *
     * @param writer the writer
     * @throws IllegalArgumentException if the parameter is <code>null</code>
     */
    public void marshall(Writer writer) throws IllegalArgumentException {
        if (null == writer) {
            throw new IllegalArgumentException(ERROR_WRITER_NULL);
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(FilterDefinitionList.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            FilterDefinitionList filterList = new FilterDefinitionList();
            filterList.setFilterDefinitionList(filterDefinitionListe);

            jaxbMarshaller.marshal(filterList, writer);

        } catch (final Throwable exception) {
            throw new RuntimeException(ERROR_CANT_WRITE_FILE, exception);
        }
    }

}
