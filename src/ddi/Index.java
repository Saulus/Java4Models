package ddi;

import org.joda.time.LocalDate;

public class Index {
	//private final static Logger LOGGER = Logger.getLogger(Index.class.getName());
	
	private LocalDate start;
	private LocalDate end;
	private String interaction_id;
	private String meta_id;
	private String dose;
	
	public Index(LocalDate start,LocalDate end,String id, String meta_id) {
		this.start=start;
		this.end=end;
		this.interaction_id=id;
		this.meta_id=meta_id;
	}
	
	public LocalDate getStartDate() {
		return start;
	}
	
	public LocalDate getEndDate() {
		return end;
	}
	
	public String getMeta() {
		return meta_id;
	}
	
	public String getInteraction() {
		return interaction_id;
	}
	
	public void setEnd(LocalDate end) {
		this.end=end;
	}

	public String getDose() {
		return dose;
	}

	public void setDose(String dose) {
		this.dose = dose;
	}
	

}
