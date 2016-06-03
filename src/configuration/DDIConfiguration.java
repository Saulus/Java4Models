package configuration;

import org.xmappr.Element;

public class DDIConfiguration {
	
	/*
	 * DDI Data from here
	 */
	@Element
	public String data_id;
	
	@Element
	public String keep_fields;
	
	@Element(defaultValue="false")
	public boolean createStatistics;
	
	
	@Element
	public Datafile_references datafile_references;
	
	@Element
	public Fieldnames fieldnames;

	
	public String getDatentyp() {
		return data_id.toUpperCase();
	}
	
	public String getFilePatient2drug() {
		return datafile_references.patient2drug.toUpperCase();
	}
	
	public String getFileDruginfo() {
		return datafile_references.druginfo.toUpperCase();
	}
	
	public String getFileDrug2substance() {
		return datafile_references.drug2substance.toUpperCase();
	}
	
	public String getFileSubstance2interaction() {
		return datafile_references.substance2interaction.toUpperCase();
	}
	
	public String getFileInteractioninfo() {
		return datafile_references.interactioninfo.toUpperCase();
	}
	
	public String getFileInteraction2meta() {
		return datafile_references.interaction2meta.toUpperCase();
	}
	
	public String getDrugfield() {
		return fieldnames.drug.toUpperCase();
	}
	
	public String getDatefield() {
		return fieldnames.date.toUpperCase();
	}
	
	public String getDrugreachfield() {
		return fieldnames.drugreach.toUpperCase();
	}
	
	public String getDrugreachStandard() {
		return fieldnames.drugreach_standard.toUpperCase();
	}
	
	public String getSubstancefield() {
		return fieldnames.substance.toUpperCase();
	}
	
	public String getDosefield() {
		if (fieldnames.dose==null) return null;
		return fieldnames.dose.toUpperCase();
	}
	
	public String getInteractionfield() {
		return fieldnames.interaction.toUpperCase();
	}
	
	public String getMeta_interactionfield() {
		return fieldnames.meta_interaction.toUpperCase();
	}
	
	public String getStartfield() {
		return fieldnames.start.toUpperCase();
	}
	
	public String getEndfield() {
		return fieldnames.end.toUpperCase();
	}
	
	public String[] getKeepFields() {
		if (keep_fields==null) return null;
		String[] tokens = keep_fields.toUpperCase().split(Consts.idfieldseparator);
		return tokens;
	} 
}
