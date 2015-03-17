package models;

import java.io.FileReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.xmappr.*;

import configuration.Konfiguration;

/**
 * The Class Java4Models.
 * 
 */
public class Java4Models {
		
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
	            System.err.println("Aufruf: java -jar java4models.jar <konfiguration.xml>");
	            System.err.println("Für schnelle Sortierung der Input-Files mit mehr Memory:  java -Xmx2048m -Xms256m -jar java4models.jar <konfiguration.xml>");
	            System.exit(1);
	        }
		Reader reader = null;
		Konfiguration config = null;
		try {
			reader = new FileReader(args[0]);
			Xmappr xm = new Xmappr(Konfiguration.class);
			config = (Konfiguration) xm.fromXML(reader);
		} catch (Exception e) {
			System.out.println("Fehler: Die Konfigurationsdatei "+ args[0] + " konnte nicht eingelesen werden.");
			e.printStackTrace();
			System.exit(1);
		}
		Worker wrk = new Worker (config);
		if ((wrk.init())
			&& (wrk.process())
			&& (wrk.finish())
			) {
			String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " Die Scores wurden erfolgreich berechnet. Outputdatei: "+ config.getOutputfile());
		}
	}

}
