package configuration;

import java.util.List;
import org.xmappr.Element;

/**
 * The Class Input.
 * Elements from konfiguration.xml are loaded into here, into variables defined acc. to xml tag.
 * Class for Tags konfiguration->input
 * 
 */
public class Input {
	
	/** The datei. */
	@Element
	public List<Datei> datei; 
}
