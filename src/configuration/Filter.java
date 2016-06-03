package configuration;

import org.xmappr.Element;

public class Filter {
	
	@Element
	public String data_id;
	@Element
	public String field; 
	@Element
	public String inclusion; 
	@Element
	public String exclusion; 
	
	
	public String getDataID() {
		return data_id.toUpperCase();
	}
	
	
	public String getField() {
		return field.toUpperCase();
	}
	
	public boolean hasInclusion() {
		return inclusion!=null;
	}
	
	public boolean hasExclusion() {
		return exclusion!=null;
	}
	
	public String getInclusion() {
		return inclusion;
	}
	
	public String getExclusion() {
		return exclusion;
	}
	
	
	
}
