package configuration;

import java.util.Locale;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

/**
 * @author HellwigP
 *
 */
public final class Utils {
	private final static DateTimeParser[] parsers = { 
	        //DateTimeFormat.forPattern( "ddMMMyyy" ).withLocale(Locale.GERMAN).getParser(),
	        DateTimeFormat.forPattern( "ddMMMyyy" ).getParser(),
	        DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser(),
	        DateTimeFormat.forPattern( "dd.MM.yyyy" ).getParser(),
	        DateTimeFormat.forPattern( "dd-MM-yyyy" ).getParser(),
	        DateTimeFormat.forPattern( "MM/dd/yyyy" ).getParser()};
	
	private final static DateTimeFormatter formatterUS = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter().withLocale(Locale.US);
	private final static DateTimeFormatter formatterDE = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter().withLocale(Locale.GERMAN);

	public Utils() {	
		//this prevents even the native class from 
	    //calling this ctor as well :
	    throw new AssertionError();
	}

	/**
	 * @param b
	 */
	public final static LocalDate parseDate (String date) {
		try {
			return new LocalDate(formatterUS.parseDateTime(date));
		} catch (IllegalArgumentException e) {
			return new LocalDate(formatterDE.parseDateTime(date));
		}
	}
}