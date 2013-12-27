package net.quasarsolutions.penlet.productivity;

public class PageTagTutor {
	String strTestString,icrContextName;
	int ilastTutorPosition,iIterationSize;
	public PageTagTutor (String parmTestString, int lasttest,int iiterationsize,String icrFilename){
		strTestString=parmTestString;
		ilastTutorPosition=lasttest;
		iIterationSize=iiterationsize;
		icrContextName=icrFilename;
	}
	
	public PageTagTutor(){
		strTestString="";
		ilastTutorPosition=0;
		iIterationSize=0;
		icrContextName="";
		
	}
	public String getNextString(){
		
		String strNextString="";
		int ilengthRemaining=strTestString.length()-ilastTutorPosition;
		if (ilengthRemaining<iIterationSize){
			strNextString=strTestString.substring(ilastTutorPosition, strTestString.length());
		} else {
			strNextString=strTestString.substring(ilastTutorPosition, ilastTutorPosition+iIterationSize);
			
		}
		ilastTutorPosition+=strNextString.length();
		return strNextString;
	}
	
	public String getPrevString(){
		
		String strPrevString="";
		int iPreviousPosition=ilastTutorPosition-2*iIterationSize;
		if (iPreviousPosition+iIterationSize>0){
			if (iPreviousPosition<0){
				if (strTestString.length()<iIterationSize){
					strPrevString=strTestString.substring(0, strTestString.length());
				} else {
					strPrevString=strTestString.substring(0, iIterationSize);
				}
				ilastTutorPosition=strPrevString.length();
			} else {
				strPrevString=strTestString.substring(iPreviousPosition, iPreviousPosition+iIterationSize);
				ilastTutorPosition=ilastTutorPosition-strPrevString.length();
			}
		}
		return strPrevString;
	}
	
	public String strGetCurrentString() {
		String strTest=strTestString.substring(ilastTutorPosition,iIterationSize);
		if (strTest.length()==0){
			ilastTutorPosition=0;
			strTest=getNextString();
		}
		return strTest;
	}
}
