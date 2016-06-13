package configuration;

import org.xmappr.Element;

public class DDIConfiguration {
	
	/*
	 * DDI Data from here
	 */
	@Element
	public String data_id;
	
	@Element(defaultValue="false")
	public String writeRowData;
	
	//@Element
	//public String writeDebugData;
	
	@Element(defaultValue="false")
	public boolean createStatistics;
	
	@Element(defaultValue="false")
	public boolean samplePatientsWithout;
	
	
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
	
	public String getDrugreachMin() {
		return fieldnames.drugreach_min;
	}
	
	public String getDrugreachMax() {
		return fieldnames.drugreach_max;
	}
	
	public String getDrugreachStandard() {
		return fieldnames.drugreach_standard;
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
	
	/*
	 * arbitrary field for combining multiple interactions
	 */
	public String getInteractionfieldCombined() {
		return fieldnames.interaction.toUpperCase() + "s";
	}
	
	
	public String getMeta_interactionfield() {
		return fieldnames.meta_interaction.toUpperCase();
	}
	
	/*
	 * arbitrary field for combining multiple interactions
	 */
	public String getMeta_interactionfieldCombined() {
		return fieldnames.meta_interaction.toUpperCase() + "s";
	}
	
	
	public String getStartfield() {
		return fieldnames.start.toUpperCase();
	}
	
	public String getEndfield() {
		return fieldnames.end.toUpperCase();
	}
	
	public String getRowDataFile() {
		return this.writeRowData;
	} 
	
	/*public String getDebugDataFile() {
		return this.writeDebugData;
	} */
}
