package configuration;

import java.util.List;
import org.xmappr.Element;

/**
 * The Class Inputfiles.
 * Elements from konfiguration.xml are loaded into here, into variables defined acc. to xml tag.
 * Class for Tags konfiguration->inputfiles
 * 
 */
public class Inputfiles {
	
	/** The datafile. */
	@Element
	public List<Datafile> datafile; 
}
