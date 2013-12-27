package net.quasarsolutions.penlet.productivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.livescribe.afp.PageAddress;
import com.livescribe.penlet.Logger;
import com.livescribe.storage.PenletStorage;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import net.quasarsolutions.penlet.utils.PenletMenuDriver;
import net.quasarsolutions.penlet.utils.PulseIOHelper;

//TODO: Create test stubs for testing database

public class PageTagDatabase {

    PageTagItem[] Database;
    int currentOpenTagSlot, currentItemCount,currentOpenRegionId;
    Logger logger;
    //persistent data storage
    PenletStorage penstore;
    InputStream inStream;
    OutputStream outStream;
    DataInputStream dinStream;
    DataOutputStream doutStream;
    public Vector tagbrowsevectorItems,tagsearchvectorItems;
    PenletMenuDriver penletDriverForItems,penletDriverForSearch;
    boolean [] showfieldsettings;
    boolean databasedirty;
    int[] showfieldorder,showfields=null;

	public final int tagfield=0,pagenofield=1,documentidfield=2,datefield=3;
	public final int timefield=4,posfield=5,maxfieldstoshow=6;
	
	String[] fldforshowprops,fldfororderprops;

    public PageTagDatabase (int maxitems, Logger log, PenletStorage store,PenletMenuDriver pMenu,PenletMenuDriver sMenu){
    	Database = new PageTagItem[maxitems];
    	for (int i=0;i<maxitems;i++){
    		Database[i]=new PageTagItem(this);
    	}
        currentOpenTagSlot=0;
        currentItemCount=0;
        currentOpenRegionId=1;
        logger=log;
        penstore=store;
        penletDriverForItems=pMenu;
        penletDriverForSearch=sMenu;

        //create tagbrowseMenu
        tagbrowsevectorItems=new Vector();
        
        //create tagbrowseMenu
        tagsearchvectorItems=new Vector();
        
        //initialize the field names that need to be read
        fldforshowprops=new String[maxfieldstoshow];
        fldforshowprops[0]="showtagfield";
        fldforshowprops[1]="showpagenofield";
        fldforshowprops[2]="showdocumentidfield";
        fldforshowprops[3]="showdatefield";
        fldforshowprops[4]="showtimefield";
        fldforshowprops[5]="showposfield";
        
        
        
        fldfororderprops = new String[maxfieldstoshow];
        fldfororderprops[0]="tagfieldorder";
        fldfororderprops[1]="pagenofieldorder";
        fldfororderprops[2]="documentidfieldorder";
        fldfororderprops[3]="datefieldorder";
        fldfororderprops[4]="timefieldorder";
        fldfororderprops[5]="posfieldorder";
        
        databasedirty=false;

    }
    
    public void readFieldSettings(PulseIOHelper ioHelper){
        //initialize fields to be shown and their order
        
        showfieldsettings=new boolean[maxfieldstoshow];
        showfieldorder = new int[showfieldsettings.length];

        //read from config file
        for (int i=0;i<showfieldsettings.length;i++){
        	showfieldsettings[i]=ioHelper.getBoolConfigData(fldforshowprops[i]);
        	showfieldorder[i]=(int)ioHelper.getLongConfigData(fldfororderprops[i]);
        }
        
        /* read from property file for the field settings
         * propid 0 to 9 show fields
         * propid 10 to 19 order of the field
         * propid 100 implies that there is a property file
         */
        
        if (ioHelper.readBooleanProperty(100)==false){
            for (int i=0;i<showfieldsettings.length;i++){
            	showfieldsettings[i]=ioHelper.readBooleanProperty(i);
            	showfieldorder[i]=(int)ioHelper.readIntegerProperty(10+i);
            }
        	
        }       

        createShowFieldOrder();
    }

    public void writeFieldSettings(PulseIOHelper ioHelper){
        ioHelper.setPropertyHandle("PageTagPropery");
        ioHelper.updateBooleanProperty(100,false);
        for (int i=0;i<showfieldsettings.length;i++){
            	ioHelper.updateBooleanProperty(i,showfieldsettings[i]);
            	ioHelper.updateIntegerProperty(i+10,showfieldorder[i]);      	
        }       
    	
    }
    
    public int getItemCount(){
    	return currentItemCount;
    }
    
    public boolean openReadDatabase(String filename){
    	try {
            if (penstore.exists(filename)){

        			inStream=penstore.openInputStream(filename);
        	        if (inStream.available()>1){
        	        	this.logger.debug("Available="+Integer.toString(inStream.available()));
        	        	dinStream=new DataInputStream(inStream);

        	        	if (adjustReadPosition_v2()){
        	        		int version=dinStream.readByte();
        	        		int filecount=dinStream.readInt();

            	        	currentItemCount=readTags();
            	        	if (currentItemCount!=filecount){
            	        		this.logger.debug("Count Error Read Count:"+Integer.toString(currentItemCount)+"<>"+Integer.toString(filecount));         	        		
            	        	}
        	        	} else {
        	        		this.logger.debug("unable to adjust position");
        	        	}
        	        	dinStream.close();
        	        } else {
                		this.logger.debug("instream not available!");
        	        	inStream.close();
        	        }
        	        return true;
            	} else {
            		this.logger.debug(filename+" not found!");
            	}
            }  catch (IOException e) {
    			this.logger.debug("open datainstream: "+e.getMessage());
    		}
    	return false;
    }

    public boolean openWriteCloseDatabase(String filename){
    	if (databasedirty){
    		if (openDatabaseForWrite(filename)){
    			writeHeader();
        		writeTags();
        		closeWriteDatabase();    			
    		}
    		databasedirty=false;
    	}
    	return true;
    }
    
    private boolean adjustReadPosition_v1(){

		//TODO: need to understand these 2 extra bytes...
    	try {
			dinStream.skipBytes(2);
			return true;
		} catch (IOException e1) {
			this.logger.debug(e1.getMessage());
			return false;
		}

    }

    private boolean adjustReadPosition_v2(){
    	boolean codefound=false;


    	byte b1=0,b2=0;
		try {
			b1=dinStream.readByte();
		} catch (IOException e){
			this.logger.debug("reading code b1 "+e.getMessage());
			return false;
		}
  	
    	while (!codefound){
    		try {
    			b2=dinStream.readByte();
    		} catch (IOException e){
    			this.logger.debug("reading code b2 "+e.getMessage());
    			break;
    		}
    		if (b1==8 && b2==57){
    			codefound=true;
    		} else {
    			b1=b2;
    		}
    	}
    	
    	return codefound;
    }
    private boolean writeHeader(){
    	try {
    		//write the code
    		doutStream.writeByte(8);
    		doutStream.writeByte(57);
    		//write the version
    		doutStream.writeByte(1);
    		//write the count
    		doutStream.writeInt(getItemCount());
    		return true;
    	} catch (IOException e) {
    		this.logger.debug("error while writing header "+e.getMessage());
    		return false;
    		
    	}
    }
    
    private boolean openDatabaseForWrite(String filename){

    	//open dataoutput stream
        try {
            if (penstore.exists(filename)){
            	penstore.delete(filename);
            }
			outStream=penstore.openOutputStream(filename,true);
	        doutStream=new DataOutputStream(outStream);
	        return true;
		} catch (IOException e) {
			this.logger.debug("open dataoutstream: "+e.getMessage());
		}
    	return false;
    	
    }

    private boolean closeWriteDatabase(){
        try {
			doutStream.close();
			/* closing the dataoutput stream closes the output stream
			 * code commented out after firmware upgrade on 12/04/2010
	        outStream.close();
	        */
	        return true;
		} catch (IOException e) {
			this.logger.debug("error while closing file"+e.getMessage());
		}
		return false;

    }
    public PageTagItem addTagItem(String tagText, String Date, String Time, String documentTitle, int currentpageno,int x, int y,long paaddress) {
		if (tagText.length()>0){
			if (getItemCount()!= Database.length){
				//TODO: need to handle currentOpenRegionId rollover at max int
				int addslot=currentOpenTagSlot;
				Database[currentOpenTagSlot].updateData(tagText,Date,Time,documentTitle,currentpageno,x,y, currentOpenRegionId,paaddress);
				currentOpenRegionId++;
				updateTagBrowseList(currentOpenTagSlot);
				currentItemCount++;
				databasedirty=true;

				if (currentOpenTagSlot<Database.length){
					currentOpenTagSlot++;
				} 
				
				return Database[addslot];
			}
		} 
		return null;
	}

	private void writeTags(){
		int tagsWritten=0;
		for (int i=0;i<Database.length;i++){

			if (Database[i].text.length()>0){
				if (Database[i].writeItem(doutStream)){
					tagsWritten++;
				} else {
					this.logger.debug("write fail item "+Integer.toString(i));				
				}
			}
		}
		this.logger.debug("items written="+Integer.toString(tagsWritten));				
	}
	
	public int readTags(){

		
		//now read the items till there are none
		
		int databasepos=0;
		
		while (readItem(databasepos,dinStream)){
			updateTagBrowseList(databasepos);
			if (Database[databasepos].regionid>=currentOpenRegionId){
				currentOpenRegionId=Database[databasepos].regionid+1;
			}
			databasepos++;
			if (databasepos==Database.length){
				break;
			}
		}
		currentOpenTagSlot=databasepos;
		this.logger.debug("items read="+Integer.toString(databasepos));
		return databasepos;
	}

	/**
	 * @param pos
	 */
	private void updateTagBrowseList(int pos) {
		// add item to the browselist
		TagBrowseListItem item=new TagBrowseListItem(logger,true,null,null,Database[pos].toString(),penletDriverForItems,pos);
		tagbrowsevectorItems.addElement(item);
	}
	
	public void updateTagBrowseList(){
		int elementsadded=0;

		for (int i=0;i<Database.length;i++){
			if (!Database[i].isEmpty()){
				TagBrowseListItem item=((TagBrowseListItem)tagbrowsevectorItems.elementAt(elementsadded));
				item.setText(Database[i].toString());
				elementsadded++;
				if (elementsadded==getItemCount()){
					return;
				}
			}
		}
	}

	public boolean updateTagSearchList(String text){
		int elementsadded=0;

		tagsearchvectorItems.removeAllElements();
		for (int i=0;i<Database.length;i++){
			if (!Database[i].isEmpty()){
				if (Database[i].match(text)){
					TagBrowseListItem item=new TagBrowseListItem(logger,true,null,null,Database[i].toString(),penletDriverForSearch,i);
					tagsearchvectorItems.addElement(item);
					elementsadded++;
					if (elementsadded==getItemCount()){
						break;
					}
				}
			}
		}
		return  (elementsadded>0);
	}

	private boolean readItem(int itempos,DataInputStream dstream){
		try {
			Database[itempos].text=dstream.readUTF();
			Database[itempos].date=dstream.readUTF();
			Database[itempos].time=dstream.readUTF();
			Database[itempos].documentid=dstream.readUTF();
			Database[itempos].pageno=dstream.readInt();
			Database[itempos].posx=dstream.readInt();
			Database[itempos].posy=dstream.readInt();
			Database[itempos].regionid=dstream.readInt();
			Database[itempos].pageaddress=dstream.readLong();
			return true;
		} catch (IOException e) {
			this.logger.debug("read items: "+e.getMessage());
			return false;
		}
	}

	public int getDatabaseEntryForTagBrowseList(int itemtofind){
		//find the item that had focus
		TagBrowseListItem item=(TagBrowseListItem) tagbrowsevectorItems.elementAt(itemtofind);
		return item.DatabaseEntry;		
	}

	public void deleteItemFromTagBrowseList(int itemtodelete){
		//find the item that had focus
		TagBrowseListItem item=(TagBrowseListItem) tagbrowsevectorItems.elementAt(itemtodelete);
		
		Database[item.DatabaseEntry].initData();
		currentItemCount--;
		databasedirty=true;
		tagbrowsevectorItems.removeElementAt(itemtodelete);
	}
	
	public int getDatabaseEntryForSearchBrowseList(int itemtofind){
		//find the item that had focus
		TagBrowseListItem item=(TagBrowseListItem) tagsearchvectorItems.elementAt(itemtofind);
		return item.DatabaseEntry;		
	}
	
	public void deleteItemFromSearchBrowseList(int itemtodelete){
		//find the item that had focus
		TagBrowseListItem item=(TagBrowseListItem) tagsearchvectorItems.elementAt(itemtodelete);
		removeDatabaseEntryFromBrowseList(item.DatabaseEntry);

		Database[item.DatabaseEntry].initData();
		currentItemCount--;
		databasedirty=true;
		tagsearchvectorItems.removeElementAt(itemtodelete);
	}

	public void removeDatabaseEntryFromBrowseList(int entryNos){
	
		for (int i=0;i<tagbrowsevectorItems.size();i++){
			if (((TagBrowseListItem) tagbrowsevectorItems.elementAt(i)).DatabaseEntry==entryNos){
				tagbrowsevectorItems.removeElementAt(i);
				return;
			}
		}
	}
	
	public void createShowFieldOrder(){
		
		int fieldstoshowcount=0,whichpos;
		showfields=null;
		
		for (int i=0;i<maxfieldstoshow;i++){
			if (showfieldsettings[i]){
				fieldstoshowcount++;
			}
		}
		if (fieldstoshowcount>0){
			showfields=new int[fieldstoshowcount];
			int[] tempshowfields=new int[maxfieldstoshow];
			int fieldcount=0;
			//first transpose all the fields
			for (int i=0;i<maxfieldstoshow;i++){
				whichpos=showfieldorder[i];
				tempshowfields[whichpos]=i;
			}			
			//now filter out the fields that are hidden
			for (int i=0;i<maxfieldstoshow;i++){
				if (showfieldsettings[tempshowfields[i]]){
					showfields[fieldcount]=tempshowfields[i];
					fieldcount++;
				}
			}		
		}
	}

	public String getTagForRegionID(int id) {
		for (int i=0;i<Database.length;i++){
			if (!Database[i].isEmpty()){
				if (id==Database[i].regionid){
					return Database[i].text;
				}
			}
		}
		return null;
	}

	public void updatePageNumbers(String documentTitle, int i) {

		/*loop over the database, compare the document and update
		 * logical page number is retrieved from long page address
		 * that is stored in the database
		 */

		databasedirty=true;

		for (int j=0;j<Database.length;j++){
			if (!Database[j].isEmpty()){
				if (Database[j].documentid.compareTo(documentTitle)==0){
					PageAddress pa=new PageAddress(Database[j].pageaddress);
					Database[j].pageno=pa.getPageInstance().getPage()+i;
				}
			}
		}
		
		// update the browse list if the page number field is shown
		if (showfieldsettings[pagenofield]){
			updateTagBrowseList();		
		}
	}

	public boolean hasSpace() {
		return (getItemCount()!=Database.length && currentOpenTagSlot!=Database.length);
	}	
}
