package de.abda.fhir.validator.core.filter.regex;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Testet die JaxB- Implementierung von {@link FilterDefinition}
 * @author Dzmitry Liashenka
 *
 */
public class FilterBeschreibungTest {
	
	FilterDefinition filterBeschreibung;
	
	@BeforeEach
	public void setup() {
		filterBeschreibung = new FilterDefinition();
	}
	
	@Test
	public void testToString() {
		filterBeschreibung.setLocationPattern(Pattern.compile("location"));
		filterBeschreibung.setMessagePattern(Pattern.compile("message"));
		filterBeschreibung.setSeverityPattern(Pattern.compile("information"));
		
		assertEquals("FilterBeschreibung [ severityPattern='information', locationPattern='location', messagePattern='message' ]", filterBeschreibung.toString());
	}

}
