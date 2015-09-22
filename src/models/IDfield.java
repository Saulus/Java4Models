package models;

import configuration.Consts;

public class IDfield {
	private String field;
	private int addvalue = 0;

	public IDfield(String mytoken) throws Exception {
		//Split "+", then split "-" 
		String[] plustokens = mytoken.split("\\+");
		String[] minustokens;
		String[] parts;
		for (int i=0; i<plustokens.length; i++) {
			//split again by "-"
			minustokens = plustokens[i].split("\\-");
			//now: 1st is +, following are -
			for (int j=0; j<minustokens.length; j++) {
				//if starting with "value" -> look for (), otherwise use as field
				if (minustokens[j].toUpperCase().startsWith(Consts.aggValue)) {
					parts = minustokens[j].split(Consts.bracketEsc);
					if (j==0) addvalue += Integer.parseInt(parts[1]);
					else addvalue -= Integer.parseInt(parts[1]);
				} else this.field=minustokens[j].toUpperCase();
			}
		}
		
	}
	
	public String getField() {
		return field;
	}
	
	public String getFinalValue (String fieldvalue) throws Exception {
		if (addvalue!=0)
			return String.valueOf(Integer.parseInt(fieldvalue) + addvalue);
		else return fieldvalue;
	}

}
