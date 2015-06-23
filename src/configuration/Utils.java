package configuration;

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
	        DateTimeFormat.forPattern( "ddMMMyyy" ).getParser(),
	        DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser(),
	        DateTimeFormat.forPattern( "dd.MM.yyyy" ).getParser(),
	        DateTimeFormat.forPattern( "dd-MM-yyyy" ).getParser()};
	
	private final static DateTimeFormatter formatter = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter();

	public Utils() {	
		//this prevents even the native class from 
	    //calling this ctor as well :
	    throw new AssertionError();
	}

	/**
	 * @param board in format: "wBA1 for "weiﬂer Bauer auf A2" and sSA7 for "schwarzer Springer auf A7"
	 */
	public final static LocalDate parseDate (String date) {
		return new LocalDate(formatter.parseDateTime(date));
	}
}