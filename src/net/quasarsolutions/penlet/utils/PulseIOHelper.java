package net.quasarsolutions.penlet.utils;

import com.livescribe.afp.PropertyCollection;
import com.livescribe.configuration.Config;
import com.livescribe.penlet.Penlet;
import com.livescribe.penlet.PenletContext;

public class PulseIOHelper {

	PropertyCollection props;
	Config ConfigData;
	PenletContext context;
	
	public PulseIOHelper(Penlet mpenlet){
		context=mpenlet.getContext();
		ConfigData=context.getAppConfiguration();
		props=null;
	}
	
	public boolean getBoolConfigData(String configname){
		return ConfigData.getBooleanValue(configname);
	}

	public long getLongConfigData(String configname){
		return ConfigData.getLongValue(configname);
	}

	public void setPropertyHandle(String filename){
		
		//get the new property file. create if it does not exist
		props=PropertyCollection.getInstance(context, filename, true);

	}
	public void updateBooleanProperty(long propid, boolean value) {
		if (value)
        {
        	props.setProperty(propid, "on", true);
        } 
        else
        {
        	props.setProperty(propid, "off", true);
        	
        }
	}

	public void updateIntegerProperty(long propid, long value) {

		props.setProperty(propid, String.valueOf(value), true);
	}

	public boolean readBooleanProperty(long propid) {

		String propertyvalue;
		propertyvalue= props.getProperty(propid);

		if (propertyvalue!=null){
			if (propertyvalue.equals("off"))
			{
				return false;
			}
		}
	    return true;
	}
		
	public long readIntegerProperty(long propid) {

		String propertyvalue;
		long returnvalue=0;
		
		propertyvalue= props.getProperty(propid);
		if (propertyvalue!=null)
		{
			try {

				returnvalue=Long.parseLong(propertyvalue);

			} catch (NumberFormatException e){

				returnvalue=0;			

			}
		}
		
		return returnvalue;
	}

}
