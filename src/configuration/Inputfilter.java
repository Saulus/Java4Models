package configuration;

import java.util.ArrayList;
import java.util.List;

import org.xmappr.Element;

public class Inputfilter {
	
	@Element
	public List<Filter> filter; 
	
	public ArrayList<Filter> getFilters4File (String data_id) {
		if (filter==null) return null;
		ArrayList<Filter> mylist = new ArrayList<Filter>();
		for (Filter newfilter : filter) {
			if (newfilter.getDataID().equals(data_id)) mylist.add(newfilter);
		}
		return mylist;
	}

}
