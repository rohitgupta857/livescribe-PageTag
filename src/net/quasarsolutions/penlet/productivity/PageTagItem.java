package net.quasarsolutions.penlet.productivity;

import java.io.DataOutputStream;
import java.io.IOException;

import com.livescribe.afp.PageAddress;
import com.livescribe.afp.PageInstance;

public class PageTagItem {
	public String text,date,time,documentid;
	public int pageno,posx,posy,regionid;
	public long pageaddress;
	private PageTagDatabase parentDB;
	
	/*Anychange in these variables need to be copied into the
	 * PageTagDatabase class as well
	 */
	private final int tagfield=0,pagenofield=1,documentidfield=2,datefield=3;
	private final int timefield=4,posfield=5,maxfieldstoshow=6;
	
	public PageTagItem(PageTagDatabase db)
	{
		initData();
		parentDB=db;
	}

	public boolean isEmpty(){
		return (text.length()==0);
	}
	public void updateData(String Text,String Date,String Time,String DocumentID,int page,int x, int y,int region, long paaddress){

		text=Text.toUpperCase();
		date=Date;
		time=Time;
		documentid=DocumentID;
		pageno=page;
		posx=x;
		posy=y;		
		regionid=region;
		pageaddress=paaddress;
	}
	
	public void initData(){
		text=date=time=documentid="";
		pageno=posx=posy=regionid=0;
		pageaddress=0;
	}
	
	public boolean match(String MatchText){
		
		//search string.ignore case
		MatchText=MatchText.toUpperCase();
		return (text.indexOf(MatchText)!= -1);
	}
	
	public boolean writeItem(DataOutputStream dstream){
		try {
			dstream.writeUTF(text);
			dstream.writeUTF(date);
			dstream.writeUTF(time);
			dstream.writeUTF(documentid);
			dstream.writeInt(pageno);
			dstream.writeInt(posx);
			dstream.writeInt(posy);
			dstream.writeInt(regionid);
			dstream.writeLong(pageaddress);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public String toString(){
		
		int [] fields=parentDB.showfields;
		String displayString="";
		if (fields!=null){
			for (int i=0;i<fields.length;i++){
				if (i>0){
					displayString+=":";
				}
				switch (fields[i]) {
				case tagfield:
					displayString+=text;
					break;
				case pagenofield:
					displayString+="@"+Integer.toString(pageno);
					break;
				case documentidfield:
					displayString+="["+documentid+"]";
					break;
				case datefield:
					displayString+=date;
					break;
				case timefield:
					displayString+=time;
					break;
				case posfield:
					PageAddress pa=new PageAddress(pageaddress);
					PageInstance pg=pa.getPageInstance();
					int x=pg.getPageWidth();
					int y=pg.getPageHeight();
					String postring="";
					if (posx<x/2){
						if (posy<y/2){
							postring="Q1";
						} else {
							postring="Q3";							
						}
					} else {
						if (posy<y/2){
							postring="Q2";
						} else {
							postring="Q4";							
						}
					}
					displayString+=postring;
				}
			}
		}
		if (displayString.length()==0){
			displayString="[all fields hidden]";
		}
		return displayString;
	}
	
}
