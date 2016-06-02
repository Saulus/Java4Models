package models;

import java.io.FileReader;
import java.io.Reader;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.xmappr.*;

import configuration.Consts;
import configuration.TestConfiguration;
import configuration.Configuration;

/**
 * The Class Java4Models.
 * 
 */
public class Java4Models {
	
	 private final static Logger LOGGER = Logger.getLogger(Java4Models.class.getName());
	 private static FileHandler fh = null;
		
	/**
	 * The main method.
	 * Reads in konfiguration.xml and starts worker
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// 0. Get Configs
					// Inputfiles
					// modelpath
					// Outputfile
		if (args.length != 1) {
				writeHelp();
	            System.exit(1);
	        }
		if (args[0].equals("--version")) {
			writeHelp();
			System.exit(1);
		}
		Reader reader = null;
		Configuration config = null;
		try {
			reader = new FileReader(args[0]);
			Xmappr xm = new Xmappr(Configuration.class);
			config = (Configuration) xm.fromXML(reader);
		} catch (Exception e) {
			System.err.println("Fehler: Die Konfigurationsdatei "+ args[0] + " konnte nicht eingelesen werden.");
			e.printStackTrace();
			System.out.println();
			writeHelp();
			System.exit(1);
		}
		
		TestConfiguration tester = new TestConfiguration(config);
		
		if (!tester.test()) {
			writeHelp();
			System.exit(1);
		}
		
		initLog(config.getLogfile());
		
		Worker wrk = new Worker (config);
		if ((wrk.init())
			&& (wrk.process())
			&& (wrk.finish())
			) 
			LOGGER.log(Level.INFO,"Die Patienten wurden erfolgreich bearbeitet.");
		else LOGGER.log(Level.SEVERE,"Das Programm wurde durch einen Fehler beendet.");
	}
	
	public static void writeHelp () {
		System.out.println("Java4Models. Elsevier Health Analytics. Version: " + Consts.version);
		System.out.println("Aufruf: java -jar java4models.jar <konfiguration.xml>");
        System.out.println("Für schnelle Sortierung der Inputfiles-Files mit mehr Memory:  java -Xmx2048m -Xms256m -jar java4models.jar <konfiguration.xml>");
	}

	private static void initLog(String file) {
		LogManager.getLogManager().reset();
		Logger l = Logger.getLogger("");
		if (file != null && !file.equals("")) {
			try {
				fh=new FileHandler(file, false);
				fh.setFormatter(new SimpleFormatter());
				l.addHandler(fh);
			} catch (Exception e) {
				System.err.println("Fehler bei Erstellen des Logfiles");
				e.printStackTrace();
				System.exit(9);
			}
		}

		Handler consoleHandler = new Handler(){
			@Override
			public void publish(LogRecord record) {
				if (getFormatter() == null) 
					setFormatter(new SimpleFormatter());

				try {
					String message = getFormatter().format(record);
					if (record.getLevel().intValue() >= Level.WARNING.intValue())
						System.err.write(message.getBytes());                       
					else
						System.out.write(message.getBytes());
				} catch (Exception exception) {
					reportError(null, exception, ErrorManager.FORMAT_FAILURE);
				}
			}
			@Override
			public void close() throws SecurityException {}
			@Override
			public void flush(){}
		};
		
		l.addHandler(consoleHandler);
		l.setLevel(Level.INFO);
	}

}
