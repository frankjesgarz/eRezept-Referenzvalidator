package de.abda.fhir.validator.core.filter.regex;

import de.abda.fhir.validator.core.filter.MessageFilter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Frank Jesgarz
 */
public class MessageFilterTypeAdapter extends XmlAdapter<String, MessageFilter> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageFilter unmarshal(String value) {
        throw new NotImplementedException();
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
