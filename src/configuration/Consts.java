package configuration;

/**
Collected constants of general utility.
**/
public final class Consts {
	
	public static final String version = "1.1";
	
	/** The Constant satzartFlag. */
	public static final String satzartFlag = "SATZART";
	
	/** The Constant csvFlag. */
	public static final String csvFlag = "CSV";
	
	/** The Constant logRegFlag. */
	public static final String logRegFlag = "LOGREG";

	
	/** The Constant modInputfileCol.
	 * 	Column of Model.config file / definition: Inputfile from konfiguration.xml*/
	public static final String modInputfileCol = "DATENTYP";
	
	/** The Constant modFieldCol.
	 * 	Column of Model.config file / definition */
	public static final String modFieldCol = "SPALTE";
	
	/** The Constant modPositionCol. 
	 * 	Column of Model.config file / definition*/
	public static final String modPositionCol = "STRINGPOSITION";
	
	/** The Constant modValueCol.
	 * 	Column of Model.config file / definition */
	public static final String modValueCol = "WERTEFILTER";
	
	/** The Constant modAggCol.
	 * 	Column of Model.config file / definition */
	public static final String modAggCol = "AGGREGATION";
	
	/** The Constant modOtrherfieldCol.
	 * 	Column of Model.config file / definition */
	public static final String modOtrherfieldCol = "ANDERESPALTEFILTER";
	
	/** The Constant modVarCol.
	 * 	Column of Model.config file / definition */
	public static final String modVarCol = "VARIABLE";
	
	public static final String modIncludeCol = "EINSCHLUSS";
	public static final String modExcludeCol = "AUSSCHLUSS";
	
	/** The Constant aggSum.
	 * Field "Aggregation" (type) in Model.config file */
	public static final String aggSum = "SUM";
	
	/** The Constant aggCount.
	 * Field "Aggregation" (type) in Model.config file */
	public static final String aggCount = "COUNT";
	
	/** The Constant aggStd. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggStd = "OCCURRENCE";
	
	/** The Constant aggMean. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggMean = "MEAN";
	
	/** The Constant aggMin. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggMin = "MIN";
	
	/** The Constant aggMax. 
	 * Field "Aggregation" (type) in Model.config file*/
	public static final String aggMax = "MAX";
	
	/** The Constant placeholder. 
	 * Placeholder in Model.config for adding Value to Variable-Name*/
	public static final String placeholder = "$";
	
	/** The Constant placeholderEsc. 
	 * Placeholder in Model.config for adding Value to Variable-Name (escaped)*/
	public static final String placeholderEsc = "\\$";
	
	/** The Constant interceptname. 
	 * Intercept Row in model.coeff(icients) file */
	public static final String interceptname = "INTERCEPT";
	
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
