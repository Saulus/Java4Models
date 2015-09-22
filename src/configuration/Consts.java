package configuration;


/**
Collected constants of general utility.
**/
public final class Consts {
	
	public static final String version = "2.1";
	
	/** The Constant satzartFlag. */
	public static final String satzartFlag = "SATZART";
	
	/** The Constant csvFlag. */
	public static final String csvFlag = "CSV";
	
	/** The Constant logRegFlag. */
	public static final String logRegFlag = "LOGREG";

	
	/** The Constant modInputfileCol.
	 * 	Column of Model.config file / definition: Inputfile from konfiguration.xml*/
	public static final String modInputfileCol = "DATEI";
	
	public static final String modVariableCol = "VARIABLE";
	

	/** The Constant modAggCol.
	 * 	Column of Model.config file / definition */
	public static final String modAggCol = "VARAGGREGATION";
	
	public static final String modCalcCol = "VARBERECHNUNG";
	
	
	/** The Constant .
	 * 	Column of Model.config file / definition */
	public static final String modFilterCol = "VARFILTER";
	
	public static final String modIncludeCol = "EINSCHLUSS";
	public static final String modExcludeCol = "AUSSCHLUSS";
	public static final String modTargetCol = "TARGET";
	public static final String modHideCol = "HIDEME";
	
	/** The Constant modColumnCol.
	 * 	Column of Model.config file / definition */
	public static final String modColumnCol = "SPALTE";
	public static final String modColfilterCol = "FILTER";
	public static final int maxcol = 5;
	
	
	
	public static final String aggValue = "VALUE";
	
	/** The Constant aggSum.
	 * Field "Aggregation" (type) in Model.config file */
	public static final String aggSum = "SUM";
	
	/** The Constant aggCount.
	 * Field "Aggregation" (type) in Model.config file */
	public static final String aggCount = "COUNT";
	
	/** The Constant aggOccurence. = default
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggOccurence = "OCCURRENCE"; 
	
	/** The Constant aggMean. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggMean = "MEAN";
	
	/** The Constant aggMin. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggMin = "MIN";
	
	/** The Constant aggMax. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggMax = "MAX";
	
	/** The Constant aggMaxdistance. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggMaxDistance = "MAXDISTANCE";
	
	public static final String aggDate = "DATE";
	

	public static final String referenceEsc = "\\$";
	public static final String reference = "$";
	public static final String varreferenceEsc = "V\\(";
	
	public static final String bracketEsc = "[\\(\\)]";
	
	public static final String wahr = "TRUE";
	public static final String idfieldseparator = ",";
	public static final String idfieldheader = "ID";
	
	
	/** The Constant interceptname. 
	 * Intercept Row in model.coeff(icients) file */
	public static final String interceptname = "INTERCEPT";
	
	public static final String reference_date = "01JAN2012";
	
	/** The Constant navalue. 
	 * for dense profile creation */
	public static final String navalue = "0";
	
	/** The Constant comment_indicator. 
	 * for Model config */
	public static final String comment_indicator = "#";
	
	
	 /**
 	 * Instantiates a new consts.
 	 */
 	private Consts(){
		    //this prevents even the native class from 
		    //calling this ctor as well :
		    throw new AssertionError();
	 }
}
