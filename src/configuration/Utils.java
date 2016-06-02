package configuration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.mozilla.universalchardet.UniversalDetector;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author HellwigP
 *
 */
public final class Utils {
	private final static Logger LOGGER = Logger.getLogger(Utils.class.getName());
	
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
	
	public final static void addHeaderToCsv (String csvfilename, List<String> header, String newfilename) throws Exception {
		CSVWriter newfile = new CSVWriter(new FileWriter(newfilename), ';', CSVWriter.NO_QUOTE_CHARACTER);
		newfile.writeNext(header.toArray(new String[header.size()]));
		CSVReader tmpfile = new CSVReader(new FileReader(csvfilename), ';');
		//now write line for line
		String[] nextLine;
		String[] tmpline;
	    while ((nextLine = tmpfile.readNext()) != null) {
	    	//add missing delimiters (i.e. empty columns) -> required for some csv parsers
	    	if (nextLine.length < header.size()) {
	    		tmpline = new String[header.size()];
	    		System.arraycopy(nextLine, 0, tmpline, 0, nextLine.length);
	    		for (int i=nextLine.length;i<header.size();i++) tmpline[i]=Consts.navalue; //set navalue
	    		newfile.writeNext(tmpline);
	    	} else newfile.writeNext(nextLine);
	    }
	    //close and delete tmpfile
	    newfile.close();
	    tmpfile.close();
	    File file = new File(csvfilename);
	    file.delete();
	}
	
	
	public final static String[] concatArrays(String[] a, String[] b) {
		   int aLen = a.length;
		   int bLen = b.length;
		   String[] c= new String[aLen+bLen];
		   System.arraycopy(a, 0, c, 0, aLen);
		   System.arraycopy(b, 0, c, aLen, bLen);
		   return c;
		}
	
	
	
	//fix encoding when reading colnames from csv
	public static String checkEncoding(String fileName) throws IOException {
		byte[] buf = new byte[4096];
        java.io.FileInputStream fis = new java.io.FileInputStream(fileName);

        // (1)
        UniversalDetector detector = new UniversalDetector(null);

        // (2)
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        // (3)
        detector.dataEnd();
        fis.close();
        
     // (4)
       return detector.getDetectedCharset();
	}
}