package de.abda.fhir.validator.core.filter.regex;

import de.abda.fhir.validator.core.filter.MessageFilter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * This class is responsible for marshalling MessageFilter to Strings.
 * The unmarshalling is currently not supported.
 * @author Frank Jesgarz
 */
public class MessageFilterTypeAdapter extends XmlAdapter<String, MessageFilter> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageFilter unmarshal(String value) {
        throw new IllegalStateException("nicht implementiert");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String marshal(MessageFilter value) {
        if (null != value) {
            return value.toString();
        }
        return null;
    }
}
