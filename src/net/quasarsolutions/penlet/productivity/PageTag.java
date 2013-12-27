package net.quasarsolutions.penlet.productivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.livescribe.penlet.Penlet;
import com.livescribe.penlet.RegionCollection;
import com.livescribe.afp.PageAddress;
import com.livescribe.afp.PageInstance;
import com.livescribe.configuration.Config;
import com.livescribe.display.BrowseList;
import com.livescribe.display.Display;
import com.livescribe.display.Displayable;
import com.livescribe.storage.PenletStorage;
import com.livescribe.storage.StrokeStorage;
import com.livescribe.ui.DateIndicator;
import com.livescribe.ui.MediaPlayer;
import com.livescribe.ui.ScrollLabel;
import com.livescribe.ui.TimeIndicator;
import com.livescribe.event.MenuEvent;
import com.livescribe.event.MenuEventListener;
import com.livescribe.event.PenTipListener;
import com.livescribe.event.StrokeListener;
import com.livescribe.penlet.Region;
import com.livescribe.event.HWRListener;
import com.livescribe.geom.Rectangle;
import com.livescribe.icr.ICRContext;
import com.livescribe.icr.Language;
import com.livescribe.icr.Resource;
import com.livescribe.icr.WritingStyle;
import com.livescribe.display.Image;

import net.quasarsolutions.penlet.utils.MenuBrowseListItem;
import net.quasarsolutions.penlet.utils.PenletMenuDriver;
import net.quasarsolutions.penlet.utils.PulseIOHelper;
import net.quasarsolutions.penlet.utils.PulseInterfaceHelpers;

/* TODO: Create notes on why the app crashes
 * 1) incorrect data read from config file (CodeBreaker)
 * 2) switch statement : wrong order of the case statements.
 */

//TODO: Bug - app crashed while giving demo. Flick! Cannot reproduce
//TODO: Bug - tapping a region loads the app. sometime the pen down is considered a stroke and the app exits
//TODO: Feature - Search Date?
//TODO: Feature - Exit Y/N?
//TODO: Feature - Quick Tag Editing
//TODO: Feature - Saved Regions: New Tag
//TODO: Bug - overlapping regions causes a problem
//TODO: Bug - sometime the pause ack does not show. ready to add prompt


public class PageTag extends Penlet implements StrokeListener, HWRListener, MenuEventListener, PenTipListener {
    
    public Display display;
    private MediaPlayer player;
    private ScrollLabel label;
    private ICRContext icrContext=null;
    private PulseInterfaceHelpers interfaceHelper;
    private PenletMenuDriver mypenletMenuDriver,tagbrowseMenuDriver,tagbrowseoptMenuDriver,tagsearchoptMenuDriver;
    private PenletMenuDriver tagsearchMenuDriver;
    private PageTagTutor UCASEPageTagTutor,lcasePageTagTutor,numberPageTagTutor,currentTutor;
    int currentpageno;
    long currentpageaddress;
    Rectangle tagRectangle;

    public Vector tagbrowseoptvectorItems,tagsearchoptvectorItems;
    
    String documentTitle,tagText,searchText;
    DateIndicator systemdate;
    TimeIndicator systemtime;
    PageTagDatabase pagetagDatabase;
    final int appmodeMenuSelection=0,appmodeNewTag=1,appmodeSearchTag=2,appmodeBrowseTag=3,appmodeOptions=4,appmodeOptionsShowHide=5;
    final int appmodeOptionsOrderFields=6,appmodeBrowseSearchTag=7,appmodeQuickTag=8;
    final int appmodeOptionsPageOffset=9,appmodeHelp=10,appmodeNewTagFullError=11;
    final int appmodeHelpUCASE=12,appmodeHelplcase=13,appmodeHelpnumber=14,appmodeHelpDblTap=15;
    int appMode,icrMode=1;
	private PulseIOHelper ioHelper;
	private boolean propertiesdirty,pageOffsetsDirty=false;
	private boolean penStrokeFilter=false;
	private int switchToNoteModeCounter=0;
	int [] pageOffset;
	String[] pageOffsetDocumentName;
	int pageOffsetSlot;
	final int rc_addsuccess=0,rc_addnotext=1,rc_addfull=2,rc_addoverlap=3,rc=5;
	private String tagPrompt="TAG: ";
	private String srchPrompt="SRCH: ";
	private String TutorPrompt="",TutorTest="",TutorResult="";


    public PageTag() {   
    }

    /**
     * Invoked when the application is initialized.  This happens once for an application instance.
     */
    public void initApp() {
    	boolean success;
    	MenuBrowseListItem item1,item2,item3,item4,item5;
    	Config configdata;
    	
        this.display = this.context.getDisplay();
        this.label = new ScrollLabel();

        player=MediaPlayer.newInstance(this);      
        systemdate=new DateIndicator();
        systemtime=new TimeIndicator();
        configdata=this.context.getAppConfiguration();
        
        interfaceHelper=new PulseInterfaceHelpers(player,true,this.getClass(),this.context.getSystemConfiguration());
        
        updateDisplay("Loading...");

        //create menu
        mypenletMenuDriver=new PenletMenuDriver(this,display,null,appmodeMenuSelection);
        mypenletMenuDriver.makeMenu(configdata,"menuOption");

        tagbrowseMenuDriver=new PenletMenuDriver(this,display,mypenletMenuDriver,appmodeBrowseTag);
        
        //this is a standalone menu. no parent
        
        tagsearchMenuDriver=new PenletMenuDriver(this,display,null,appmodeBrowseSearchTag);

        //create tag browse operationMenu
        tagbrowseoptMenuDriver=new PenletMenuDriver(this,display,tagbrowseMenuDriver,appmodeBrowseTag);
        tagbrowseoptvectorItems=new Vector();
        MenuBrowseListItem item=new MenuBrowseListItem(logger, true, null, null, "Delete Tag?", tagbrowseMenuDriver);
        tagbrowseoptvectorItems.addElement(item);
        tagbrowseoptMenuDriver.makeMenu(tagbrowseoptvectorItems); 

        //create tag search operationMenu
        tagsearchoptMenuDriver=new PenletMenuDriver(this,display,tagsearchMenuDriver,appmodeBrowseSearchTag);
        tagsearchoptvectorItems=new Vector();
        MenuBrowseListItem item0=new MenuBrowseListItem(logger, true, null, null, "Delete Tag?", tagsearchMenuDriver);
        tagsearchoptvectorItems.addElement(item0);
        tagsearchoptMenuDriver.makeMenu(tagsearchoptvectorItems); 
        
        //create tutor helpers
        UCASEPageTagTutor=new PageTagTutor("ABCDEFGHIJKLMNOPQRSTUVWXYZ",0,3,"PageTagUAlpha");
        lcasePageTagTutor=new PageTagTutor("abcdefghijklmnopqrstuvwxyz",0,3,"PageTagLAlpha");
        numberPageTagTutor=new PageTagTutor("0123456789",0,3,"PageTagNum");
        currentTutor=new PageTagTutor();

        //createsubmenus
        
        //sub menu for quicktag
        item4=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(1);
        item4.createSubOptions(this, configdata, "menuOption1",mypenletMenuDriver,display,appmodeQuickTag);
        
        //sub menu for option
        
        item1=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(4);
        item1.createSubOptions(this, configdata, "menuOption4",mypenletMenuDriver,display,appmodeOptions);

        //sub sub menu for option - select tag data

        item2=(MenuBrowseListItem)item1.subMenuDriver.vectorMenuItems.elementAt(0);
        item2.createSubOptions(this,configdata,"menuOption40",item1.subMenuDriver,display,appmodeOptionsShowHide);
        
        //sub sub menu for option - order tag data

        item3=(MenuBrowseListItem)item1.subMenuDriver.vectorMenuItems.elementAt(1);
        item3.createSubOptions(this,configdata,"menuOption41",item1.subMenuDriver,display,appmodeOptionsOrderFields);

        //sub menu for help
        
        item5=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(5);
        item5.createSubOptions(this, configdata, "menuOption5",mypenletMenuDriver,display,appmodeHelp);

        //set browse list and search list to not selectable
        deactivateBrowseAndSearchMenu();
            
        //create database
        pagetagDatabase=new PageTagDatabase(200,this.logger,this.context.getInternalPenletStorage(),tagbrowseMenuDriver,tagsearchMenuDriver);
        
        ioHelper=new PulseIOHelper(this);
        ioHelper.setPropertyHandle("PageTagPropery");
        
        /* read from property file for the field settings
         * propid 0 to 9 show fields
         * propid 10 to 19 order of the field
         * propid 30 is ICR context
         * propid 100 implies that there is a property file
         */

        pagetagDatabase.readFieldSettings(ioHelper);

        //update the show/hide display in the field menu       
        for (int i=0;i<item2.subMenuDriver.vectorMenuItems.size();i++){
        	String suffix=":[hidden]";
        	if (pagetagDatabase.showfieldsettings[i]){
        		suffix=":[shown]";
        	}
    		((MenuBrowseListItem)item2.subMenuDriver.vectorMenuItems.elementAt(i)).setSuffix(suffix);
        }

        updateOrderFieldsSuffix(item3);

        
        //retrieve pageoffsets
        pageOffset=new int[20];
        pageOffsetDocumentName=new String[20];
        pageOffsetSlot=0;

        readPageOffsetsFromFile("pageoffsets.ptd");
        
        
        //open input stream, and read all the items
        success=pagetagDatabase.openReadDatabase("pagetagdata.ptd");

        //create the submenu for the browse items
        item1=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(3);      
        item1.createSubOptions(tagbrowseMenuDriver,pagetagDatabase.tagbrowsevectorItems);


        if (pagetagDatabase.getItemCount()>0){
            activateBrowseAndSearchMenu();        	
        }
        
        
        //create the blank search menu
        tagsearchMenuDriver.makeMenu(pagetagDatabase.tagsearchvectorItems);
        
        propertiesdirty=false;
        
    }

	/**
	 * @param item3
	 */
	private void updateOrderFieldsSuffix(MenuBrowseListItem item3) {
		//update the order tag display in the field menu
		String suffix="";
        for (int i=0;i<item3.subMenuDriver.vectorMenuItems.size();i++){
        	int elementpos=pagetagDatabase.showfieldorder[i];
        	switch (i){
        	case 0:
        		suffix="[Tag]";
        		break;
        	case 1:
        		suffix="[Page]";
        		break;
        	case 2:
        		suffix="[Doc]";
        		break;
        	case 3:
        		suffix="[Date]";
        		break;
        	case 4:
        		suffix="[Time]";
        		break;
        	case 5:
        		suffix="[Position]";
        		break;
        		
        	}
    		((MenuBrowseListItem)item3.subMenuDriver.vectorMenuItems.elementAt(elementpos)).setSuffix(suffix);
        }
	}

	/**
	 * 
	 */
	private void activateBrowseAndSearchMenu() {

		((MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(2)).setSelectable(true);
		MenuBrowseListItem item=((MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(3));
		item.setSelectable(true);
		item.setSuffix(" ["+Integer.toString(tagbrowseMenuDriver.vectorMenuItems.size())+"]");
	}

	/**
	 * 
	 */
	private void deactivateBrowseAndSearchMenu() {
		((MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(2)).setSelectable(false);
		MenuBrowseListItem item=((MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(3));
		item.setSelectable(false);
		item.setSuffix("");
	}
    
    /**
     * Invoked each time the penlet is activated.  Only one penlet is active at any given time.
     */

    public void activateApp(int reason, Object[] args) {

    	this.context.addStrokeListener(this);
		context.addPenTipListener(this);

		//retrieve ICR mode from properties
		icrMode=(int)ioHelper.readIntegerProperty(30);
		if (icrMode==0){
			icrMode=1;
		}
		
		//read the tutorial settings
		UCASEPageTagTutor.ilastTutorPosition=(int)ioHelper.readIntegerProperty(31);
		lcasePageTagTutor.ilastTutorPosition=(int)ioHelper.readIntegerProperty(32);
		numberPageTagTutor.ilastTutorPosition=(int)ioHelper.readIntegerProperty(33);

		//retrieve the menu option for ICR mode. update its text and create context
        MenuBrowseListItem item1=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(4);
        MenuBrowseListItem item2=(MenuBrowseListItem)item1.subMenuDriver.vectorMenuItems.elementAt(2);
        updateICROption(item2);

        //system ready. play a sound
        interfaceHelper.playSound("SL_CA");

        //set the main menu
        if (reason==ACTIVATED_BY_MENU){
        	/*if there are no items in the database, then play welcome
        	 * and take the user to the help menu
        	 */
        	
        	if (pagetagDatabase.getItemCount()==0){
        		int waitdivide=1;
        		if (displayMuted()){
        			waitdivide=2;
        		}
        		displayMessage("Welcome! Use Help to Learn More.","welcome",8/waitdivide);
        		mypenletMenuDriver.menuBrowseList.setFocusItem(5);
        	}
        	this.display.setCurrent(mypenletMenuDriver.menuBrowseList);
        	appMode=appmodeMenuSelection;
        } else {
        	// move to quick menu
        	gotoQuickTagMenu();
        }

        //initialize tag input data
        tagText="";
        tagRectangle=null;
        searchText="";
        documentTitle="";
        currentpageno=0;
        currentpageaddress=0;
        

    }

	/**
	 * 
	 */
	private void gotoQuickTagMenu() {
		mypenletMenuDriver.menuBrowseList.setFocusItem(1);
		MenuBrowseListItem item = (MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(1);
		this.display.setCurrent(item.subMenuDriver.menuBrowseList);  
		appMode=appmodeMenuSelection;
	}

	/**
	 * 
	 */
	private void gotoBrowseTagMenu() {
		mypenletMenuDriver.menuBrowseList.setFocusItem(3);
		MenuBrowseListItem item = (MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(3);
		if (item.isSelectable()){
			this.display.setCurrent(item.subMenuDriver.menuBrowseList);  
			appMode=appmodeBrowseTag;
		} else {
			this.display.setCurrent(mypenletMenuDriver.menuBrowseList);
		}
	}

	/**
	 * 
	 */
	private void createICRContext(String resname) {
		//delete previous context
		if (icrContext!=null){
			icrContext.dispose();
			icrContext=null;
		}
		
		// Configure the ICR context
        try {
            //this.icrContext = this.context.getICRContext(1000, this);
            this.icrContext=this.context.getICRContext(1000, this, Language.EN_US,WritingStyle.Cursive);
            Resource[] resources = {
            		icrContext.createAKSystemResource(ICRContext.SYSRES_AK_LMEM),
            		this.icrContext.createAppResource("/icr/SK_"+resname+".res"),
            		this.icrContext.createAppResource("/icr/LUDEF_"+resname+".res")
             };            
            this.icrContext.addResourceSet(resources);   
        } catch (Exception e) {
            String msg = "Error initializing handwriting recognition resources: " + e.getMessage();
            this.logger.error(msg);
            this.label.draw(msg, true);
            this.display.setCurrent(this.label);
        }
	}
    
    /**
     * Invoked when the application is deactivated.
     */
    public void deactivateApp(int reason) {
        this.context.removeStrokeListener(this);
		context.removePenTipListener(this);
        icrContext.dispose();
        icrContext = null;            

        updateDisplay("Saving.Data");

        pagetagDatabase.openWriteCloseDatabase("pagetagdata.ptd");
        

        if (propertiesdirty){
            updateDisplay("Saving.Props");
        	//save the field properties
        	pagetagDatabase.writeFieldSettings(ioHelper);
        	ioHelper.updateIntegerProperty(30, icrMode);
        	ioHelper.updateIntegerProperty(31, UCASEPageTagTutor.ilastTutorPosition);
        	ioHelper.updateIntegerProperty(32, lcasePageTagTutor.ilastTutorPosition);
        	ioHelper.updateIntegerProperty(33, numberPageTagTutor.ilastTutorPosition);
        }
        
        if (pageOffsetsDirty){
            updateDisplay("Saving.Offset");
        	//save the page offsets
        	writePageOffsetsToFile("pageoffsets.ptd");
        }

        updateDisplay("GoodBye!");

    }
    
    /**
     * Invoked when the application is destroyed.  This happens once for an application instance.  
     * No other methods will be invoked on the instance after destroyApp is called.
     */
    public void destroyApp() {
    }

                 
    /**
     * Called when a new stroke is created on the pen. 
     * The stroke information is added to the ICRContext
     */
    public void strokeCreated(long time, Region regionId, PageInstance page) {

        documentTitle=page.getDocument().getTitle();

        //if this is on the page tag paper, then ignore the stroke
        if (documentTitle.compareTo("PageTagPaper")!=0){

        	this.icrContext.addStroke(page, time);
        	currentpageno=page.getPage();
        	currentpageaddress=page.getPageAddress();
        }
    }
    
    /**
     * When the user pauses (pause time specified by the wizard),
     * all strokes in the ICRContext are cleared
     */
    public void hwrUserPause(long time, String result) {

        //if this is on the page tag paper, then ignore the stroke
    	if (documentTitle.compareTo("PageTagPaper")!=0){
        	if (penStrokeFilter){
        		/*
        		 * This code is hit when using QuickTag mode only
        		 * It filters out the phantom character due to the
        		 * double tap 
        		 */
        		penStrokeFilter=false;
        	} else {
        		if ((appMode==appmodeNewTag && tagText.length()>0) || appMode==appmodeQuickTag){
        			/* check to see if there is text that needs to be added
        			 * to the database. this ensures that phantom characters
        			 * are not treat as valid input after a double tap has
        			 * been processed while adding a tag.
        			 * 
        			 * the "hwrresult" callback filters out the character due to
        			 * double tap, but if you paused long enough it gets it to 
        			 * this hwrpause callback
        			 * 
        			 * this applies to the search text as well.
        			 */
       				tagRectangle=icrContext.getTextBoundingBox();
       				displayMessage("Dbl Tap To Add","SL_Ack",2);
        		}
        		if (appMode==appmodeSearchTag){      			
        			if (searchText.length()>0){
        				displayMessage("Dbl Tap To Srch","SL_Ack",2);
        			}
        		}
        		
        		switch (appMode){
        		case appmodeHelpUCASE:
        		case appmodeHelplcase:
        		case appmodeHelpnumber:
        			if (TutorResult.compareTo(TutorTest)==0){
        				//match found. play sound and show correct. then move to next
        				propertiesdirty=true;
        				displayMessage("Correct!","SL_Ack",2);
        				TutorTest=currentTutor.getNextString();
        				if (TutorTest.length()==0){
        					displayMessage("Congrats. All Done!","SL_Ack",2);
        					currentTutor.ilastTutorPosition=0;
        					TutorTest=currentTutor.getNextString();
        				}
        			} else {
        				//match not found. play sound, show try again.
        				displayMessage("Try Again!","SL_Error",3);
        			}
        			TutorPrompt="Write:"+TutorTest;
    				updateDisplay(TutorPrompt);
    				TutorResult="";
        		}
        	}
        }
        this.icrContext.clearStrokes();

        //logic to switch to notes mode if not accepting strokes
    	if (appMode!=appmodeNewTag && appMode!=appmodeSearchTag && appMode!=appmodeQuickTag){
        	switchToNoteModeCounter++;
			if (switchToNoteModeCounter>1){
				this.logger.debug("AutoDeact Pause - Notes Mode : "+Integer.toString(appMode));
				switchToNoteModeCounter=0;
				//TODO: implement the switch to note mode 
				//this.context.notifyStateChange(false);
				//return;
			}
        }
    }
    
    /**
     * When the ICR engine detects an acceptable series or strokes,
     * it prints the detected characters onto the Pulse display.
     */
    public void hwrResult(long time, String result) {

    	switch (appMode){
    	
    	case appmodeNewTag:
    		if (!penStrokeFilter){
    			tagText=result;
    		} else {
    			penStrokeFilter=false;
    			tagText="";
    		}
    		updateDisplay(tagPrompt+tagText);
    		switchToNoteModeCounter=0;
    		return;
    	case appmodeSearchTag:
			if (!penStrokeFilter){
				searchText=result;
			}
			penStrokeFilter=false;
    		updateDisplay(srchPrompt+searchText);
    		switchToNoteModeCounter=0;
    		return;
    	case appmodeQuickTag:
    		return;
    	case appmodeHelpUCASE:
    	case appmodeHelplcase:
    	case appmodeHelpnumber:
			if (!penStrokeFilter){
				TutorResult=result;
			}
			penStrokeFilter=false;
    		updateDisplay(TutorPrompt+"=="+TutorResult);
    		switchToNoteModeCounter=0;
    		return;
    	case appmodeHelpDblTap:
    		return;
    	default:
    		if (penStrokeFilter){
    			penStrokeFilter=false;
    			return;
    		}
			this.logger.debug("AutoDeact Result - Notes Mode : "+Integer.toString(appMode));
			switchToNoteModeCounter=0;
			this.context.notifyStateChange(false);
    		return;

    	}
/*    	if (appMode==appmodeNewTag){
    		if (!penStrokeFilter){
    			tagText=result;
    		}
			penStrokeFilter=false;
    		updateDisplay(tagPrompt+tagText);
    		switchToNoteModeCounter=0;
    	} else {
    		if (appMode==appmodeSearchTag){
    			if (!penStrokeFilter){
    				searchText=result;
    			}
    			penStrokeFilter=false;
        		updateDisplay(srchPrompt+searchText);
        		switchToNoteModeCounter=0;
    			
    		}
    	}
*/    }

	/**
	 * @param text
	 */
	private void updateDisplay(String text) {
		this.label.draw(text,true);
        this.display.setCurrent(this.label);
	}
    
    /**
     * Called when an error occurs during handwriting recognition 
     */
    public void hwrError(long time, String error) {
    	this.logger.debug("hwrError="+error);
    }
    
    /**
     * Called when the user crosses out text
     */
    public void hwrCrossingOut(long time, String result) {}
    
    /**
     * Specifies that the penlet should respond to events
     * related to open paper
     */
    public boolean canProcessOpenPaperEvents () {
        return true;
    }                 

    public void singleTap(long time, int x, int y) {
    	penStrokeFilter=true;
	}

    public void doubleTap(long time, int x, int y) {
		MenuBrowseListItem focusItem=null;
		BrowseList menuList=null;
		int focusIndex,rc=0;

		icrContext.clearStrokes();
		penStrokeFilter=true;
		if (appMode==appmodeNewTag){
			if (tagRectangle!=null){
				rc=addTagItem();
				if (rc!=rc_addfull){
					tagText="";
					tagRectangle=null;
					updateDisplay(tagPrompt);
				} else {
					displayMessage("! Full-Del+Exit !","SL_Error", 2);
				}
			} else {
				displayMessage("!! Wait 4 Prompt !!","SL_Error",2);
			}
		}

		if (appMode==appmodeSearchTag){
			if (searchText.length()>0){
				createSearchTagList();
			} else {				
				//no tag to search
				displayMessage("** No Srch Txt **","SL_Error",2);
			}
			return;
		}
		if (appMode==appmodeOptionsPageOffset){
			pageOffsetsDirty=true;
			updatePageOffsets();
			for (int i=0;i<pageOffsetSlot;i++){
				this.logger.debug("doc="+pageOffsetDocumentName[i]+" offset "+Integer.toString(pageOffset[i]));
			}
			displayMessage("** Offset Set **","SL_CA",2);
	        MenuBrowseListItem item1=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(4);
			this.display.setCurrent(item1.subMenuDriver.menuBrowseList);
			appMode=appmodeOptions;
			return;
		}
		if (appMode==appmodeHelpDblTap){
			displayMessage("Excellent!","SL_DoubleTap",2);
			return;
		}

		if (appMode != appmodeNewTag && appMode != appmodeSearchTag){
			menuList=(BrowseList) display.getCurrent();
			focusItem=(MenuBrowseListItem) menuList.getFocusItem();
			focusIndex=menuList.getFocusIndex();
			appMode=focusItem.myMenuDriver.appMode;
		}

		if (appMode==appmodeQuickTag){
			String text;
			text=(String)focusItem.getText();
			focusIndex=menuList.getFocusIndex();
			tagText=text;
			if (tagRectangle!=null){
				rc=addTagItem();
				if (rc!=rc_addfull){
					tagText="";
					tagRectangle=null;
				} else {
					displayMessage("! Full-Del+Exit !", "SL_Error",2);
				}
			}  else {
				displayMessage("!! Wait 4 Prompt !!", "SL_Error",2);
			}
		}
		

	}

	private void updatePageOffsets(){
		
		int i;
		//search for document title and update no, or add a new one
		for (i=0;i<pageOffsetSlot;i++){
			if (pageOffsetDocumentName[i].compareTo(documentTitle)==0){
				pageOffset[i]=1-currentpageno;
				break;
			}
		}
		if (i==pageOffsetSlot){
			pageOffset[i]=1-currentpageno;
			pageOffsetDocumentName[i]=documentTitle;
			pageOffsetSlot++;
		}

		if (pageOffsetSlot==pageOffset.length){
			//increase database by 5, and copy data over
			int [] newoffset=new int[pageOffsetSlot+5];
			System.arraycopy(pageOffset, 0, newoffset,0,pageOffsetSlot);
			String [] newoffsettitle=new String[pageOffsetSlot+5];
			System.arraycopy(pageOffsetDocumentName, 0, newoffsettitle,0,pageOffsetSlot);
			pageOffset=newoffset;
			pageOffsetDocumentName=newoffsettitle;
		}
		
		/* now browse over the database and adjust the page numbers
		 * need to be able to get the page number from the long page address
		 */
		
		pagetagDatabase.updatePageNumbers(documentTitle,1-currentpageno);
	}
	private int addTagItem() {
		//now add the data to the database
		
		if (tagText.length()>0){
			if (pagetagDatabase.hasSpace()){
				RegionCollection rc=this.context.getCurrentRegionCollection();
				if (rc.isOverlappingExistingRegion(tagRectangle)){
					// overlaps existing region, beep and do not do anything
					displayMessage("!! Overlap !!","SL_Error",2);
					return rc_addoverlap;
				}

				//adjust the current page no to allow for physical to logical mapping

				int adjust=convertLogicalPageToPhysical(currentpageno);

				PageTagItem item=pagetagDatabase.addTagItem(tagText, systemdate.getDate(), systemtime.getTime(), documentTitle, currentpageno+adjust,tagRectangle.getX(),tagRectangle.getY(),currentpageaddress);
				if (item!=null){
					int id=item.regionid;
					Region rg=new Region(id,false,true);
					rg=rc.addRegion(tagRectangle, rg);
					activateBrowseAndSearchMenu();
					int currentitemcount=pagetagDatabase.getItemCount();

					//if first item, then set the focus
					if (currentitemcount==1 ){
						tagbrowseMenuDriver.menuBrowseList.setFocusItem(0);
					}
					displayMessage("** Added **","SL_CA",2);
					return rc_addsuccess;
				}
			} else {
				//there is no space in the database
				return rc_addfull;
			}
		}
		displayMessage("!! No Text !!","SL_Error",2);
		return rc_addnotext;
	}

	private int convertLogicalPageToPhysical(int currentpageno2) {

		for (int i=0;i<pageOffsetSlot;i++){
			if (documentTitle.compareTo(pageOffsetDocumentName[i])==0){
				return pageOffset[i];
			}
		}
		return 0;
	}

	private void deleteTagRegion(int areaid,long paaddress){
		PageAddress thispage=new PageAddress(paaddress);
		RegionCollection rc=this.context.getRegionCollection(thispage.getPageInstance());
		Region rg=rc.getFirstRegion(areaid);
		if (rg!=null){
			if (!rc.removeRegion(rg)){
				logger.debug(Integer.toString(areaid)+" region not removed");
			}
		} else {
			logger.debug("--null region returned");
		}
	}
	
	public boolean handleMenuEvent(MenuEvent menuEvent) {
		
		MenuBrowseListItem focusItem=null;
		BrowseList menuList=null;
		int focusIndex=0;
		
		/* let the penlet menu handle the menu event
		 * if it returns true, then it implies that nothing was
		 * selected and the user is just moving around in the 
		 * menu system
		 */ 

		/* need to handle search browse in a special manner
		 * this is because the tag seach menu driver is not always the
		 * action to be performed on menu right at search main menu
		 * initially we show the "Srch" prompt. Once found, we should the
		 * search browse list if found 
		 */ 
		if (appMode==appmodeBrowseSearchTag){
			if (tagsearchMenuDriver.processMenuEvent(menuEvent)){
				return true;
			}
		} else {
			if (mypenletMenuDriver.processMenuEvent(menuEvent)){

				//TODO: updating appmode. need to test
				menuList=(BrowseList) display.getCurrent();
				focusItem=(MenuBrowseListItem) menuList.getFocusItem();
				focusIndex=menuList.getFocusIndex();
				appMode=focusItem.myMenuDriver.appMode;
				
				/* if the menu is on the quick search option
				 * then reset the tagRectangle to null and clear strokes to remove any
				 * stale data
				 */
				
				if (appMode==appmodeQuickTag){
					if (pagetagDatabase.hasSpace()){
						tagRectangle=null;
						this.icrContext.clearStrokes();
					} else {
						displayMessage("! Full-Del+Exit !", "SL_Error",2);
					}
				}
				
				return true;
			}
		}
		switch (appMode){
		case appmodeNewTag:
			break;
		case appmodeSearchTag:
			break;
		case appmodeOptionsPageOffset:
			break;
		case appmodeNewTagFullError:
			break;
		case appmodeHelpUCASE:
			break;
		case appmodeHelplcase:
			break;
		case appmodeHelpnumber:
			break;
		case appmodeHelpDblTap:
			break;
		default:
			menuList=(BrowseList) display.getCurrent();
			focusItem=(MenuBrowseListItem) menuList.getFocusItem();
			focusIndex=menuList.getFocusIndex();
			appMode=focusItem.myMenuDriver.appMode;
		}
/*		if (appMode != appmodeNewTag && appMode != appmodeSearchTag && appMode!=appmodeOptionsPageOffset){
			menuList=(BrowseList) display.getCurrent();
			focusItem=(MenuBrowseListItem) menuList.getFocusItem();
			focusIndex=menuList.getFocusIndex();
			appMode=focusItem.myMenuDriver.appMode;
		}
*/
		switch (menuEvent.eventId) {
		case MenuEvent.MENU_DOWN:

			// Should never get here
			this.logger.debug("menu down -- how?");
			return true;
				
		case MenuEvent.MENU_UP:

			// Should never get here
			this.logger.debug("menu up -- how -- Appmode="+Integer.toString(appMode));
			return true;
			
		case MenuEvent.MENU_RIGHT:
						
			switch (appMode){
				case appmodeMenuSelection:
					return handleMenuRightAtMainMenu(focusIndex);
				case appmodeBrowseTag:
					return handleMenuRightAtBrowseTags();
				case appmodeBrowseSearchTag:
					return handleMenuRightAtSearchTags();
				case appmodeOptions:
					return handleMenuRightAtOptions(focusItem,focusIndex);
				case appmodeOptionsShowHide:
					return handleMenuRightAtOptionShowHide(focusItem, focusIndex);
				case appmodeOptionsOrderFields:
					return handleMenuRightAtOptionsFieldOrder(focusItem, focusIndex);
				case appmodeHelp:
					return handleMenuRightAtHelp(focusItem, focusIndex);
			}
			return true;
		case MenuEvent.MENU_LEFT:
			if (appMode==appmodeNewTag){
				//clear the entry if there is one and update display
				if (tagText.length()>0){
					tagText="";
					tagRectangle=null;
					/*TODO: need to clear strokes so that they are
					 * not carried over to the next tag. Test 
					 */
			        this.icrContext.clearStrokes();
					updateDisplay(tagPrompt);
					return true;
				}				
			}
			if (appMode==appmodeSearchTag){
				//clear the entry if there is one and update display
				if (searchText.length()>0){
					searchText="";
					/*TODO: need to clear strokes so that they are
					 * not carried over to the next tag. Test 
					 */
			        this.icrContext.clearStrokes();
					updateDisplay(srchPrompt);
					return true;
				}				
			}
			if (appMode == appmodeBrowseTag){
				if (tagbrowseoptMenuDriver.menuBrowseList.isCurrent()){
					this.display.setCurrent(tagbrowseMenuDriver.menuBrowseList);
					return true;
				}
			}

			if (appMode == appmodeBrowseSearchTag){
				if (tagsearchoptMenuDriver.menuBrowseList.isCurrent()){
					this.display.setCurrent(tagsearchMenuDriver.menuBrowseList);
					return true;
				}
			}

			if (appMode==appmodeOptionsPageOffset){
		        MenuBrowseListItem item1=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(4);
				this.display.setCurrent(item1.subMenuDriver.menuBrowseList);
				appMode=appmodeOptions;
				return true;
			}
			
			switch (appMode){
			case appmodeHelpUCASE:
				handleMenuLeftAtHelp();
				UCASEPageTagTutor.ilastTutorPosition=currentTutor.ilastTutorPosition;
		    	updateICRContext(icrMode);
				return true;
			case appmodeHelplcase:
				handleMenuLeftAtHelp();
				lcasePageTagTutor.ilastTutorPosition=currentTutor.ilastTutorPosition;
		    	updateICRContext(icrMode);
				return true;
			case appmodeHelpnumber:
				handleMenuLeftAtHelp();
		    	updateICRContext(icrMode);
				numberPageTagTutor.ilastTutorPosition=currentTutor.ilastTutorPosition;
				return true;
			case appmodeHelpDblTap:
				handleMenuLeftAtHelp();
				return true;
			}
			
			/* appmode = appModeNewTagFullError
			 * catch to hear to come back to the main menu
			 * if in a different mode, or is the menu is not displayed when it is supposed to be
			 */ 

			if (appMode !=appmodeMenuSelection || (appMode==appmodeMenuSelection && !mypenletMenuDriver.menuBrowseList.isCurrent())){
				this.display.setCurrent(mypenletMenuDriver.menuBrowseList);
				appMode=appmodeMenuSelection;
				return true;
			}
	        return false;
		case MenuEvent.MENU_SELECT:
			return true;
		default: 
			this.display.setCurrent(mypenletMenuDriver.menuBrowseList);
			appMode=appmodeMenuSelection;
		}
		return true;
	}

    private void handleMenuLeftAtHelp(){
    	MenuBrowseListItem item1=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(5);
    	this.display.setCurrent(item1.subMenuDriver.menuBrowseList);
    	appMode=appmodeHelp;
    }

	/**
	 * @param focusItem
	 * @param focusIndex
	 */
	private boolean handleMenuRightAtOptions(MenuBrowseListItem focusItem,
			int focusIndex) {
		
		switch (focusIndex){
		case 2:
			icrMode++;
			if (icrMode>3){
				icrMode=1;
			}

			propertiesdirty=true;
			updateICROption(focusItem);
			//refresh the display
			this.display.setCurrent(focusItem.myMenuDriver.menuBrowseList);
			return true;
		case 3:
			appMode=appmodeOptionsPageOffset;
			updateDisplay("Dbl Tap on Pg #1");
		}
		return true;
	}

	private boolean handleMenuRightAtHelp(MenuBrowseListItem focusItem,
			int focusIndex) {

		int waitdivide=1;
		if (displayMuted()){
			waitdivide=2;
		}

		switch (focusIndex){
		case 0:
			displayMessage("Word, Number, Region saved for easy searching!","whatisatag",8/waitdivide);
			return true;
		case 1:
			displayMessage("Write a tag, create a region, double tap!","createatag",10/waitdivide);
			return true;
		case 2:
			displayMessage("Use the Search & Browse option to find a tag!","findingatag",10/waitdivide);
			return true;
		case 3:
			interfaceHelper.playSound("ucasetutor");
			UCASETutor();
			return true;
		case 4:
			interfaceHelper.playSound("lcasetutor");
			lcaseTutor();
			return true;
		case 5:
			interfaceHelper.playSound("numerictutor");
			numberTutor();
			return true;
		case 6:
			interfaceHelper.playSound("doubletaptutor");
			appMode=appmodeHelpDblTap;
			updateDisplay("Dbl Tap - Try it!");
		}
		return true;
	}

	/**
	 * 
	 */
	private boolean displayMuted() {
		//display mute warning
		if (this.context.getSystemConfiguration().isMuted()){
			displayMessage("[MUTE]..Unmute for details!","",3);
			return true;
		}
		return false;
	}
	/**
	 * @param focusItem
	 */
	private void updateICROption(MenuBrowseListItem focusItem) {
		switch (icrMode){
		case 1:
			focusItem.setPrefix("Upper Case ");
			tagPrompt="TAG: ";
			srchPrompt="SRCH: ";
			break;
		case 2:
			focusItem.setPrefix("Lower Case ");
			tagPrompt="tag: ";
			srchPrompt="srch: ";
			break;
		case 3:
			focusItem.setPrefix("Numeric ");
			tagPrompt="#Tag: ";
			srchPrompt="#Srch: ";
			break;
		}
		updateICRContext(icrMode);
	}

	private void updateICRContext(int mode) {
		switch (mode){
		case 1:
			createICRContext("PageTagUAlpha");
			break;
		case 2:
			createICRContext("PageTagLAlpha");
			break;
		case 3:
			createICRContext("PageTagNum");
			break;
		}
	}

	/**
	 * @param focusItem
	 * @param focusIndex
	 */
	private boolean handleMenuRightAtOptionsFieldOrder(
			MenuBrowseListItem focusItem, int focusIndex) {
		/* push all the fields down, and rotate the one from the end 
		 * to the current positions:
		 * 1) loop over the showorder field array
		 * 2) if the position of the field is >= current postion, move it down
		 * 3) if the field position exceeds the bottom, then roll it up
		 * 4) at focusIndex=0, all fields should be rotated
		 * 5) at focusIndex1=1, only 5 fields should be shown etc.
		 * 
		 * Basically, you fix the location of the fields as you go down the list
		 */
		for (int i=0;i<pagetagDatabase.showfieldorder.length;i++){
			if (pagetagDatabase.showfieldorder[i]>= focusIndex){
				pagetagDatabase.showfieldorder[i]++;
				if (pagetagDatabase.showfieldorder[i]==pagetagDatabase.showfieldorder.length){
					pagetagDatabase.showfieldorder[i]=focusIndex;
				}
			}
		}
		updateOrderFieldsSuffix((MenuBrowseListItem)focusItem.myMenuDriver.parentOptionMenuDriver.menuBrowseList.getFocusItem());
		updateAfterShowHideAndOrderOptions(focusItem);
		propertiesdirty=true;
		return true;
	}

	/**
	 * @param focusItem
	 * @param focusIndex
	 */
	private boolean handleMenuRightAtOptionShowHide(MenuBrowseListItem focusItem,
			int focusIndex) {
		pagetagDatabase.showfieldsettings[focusIndex]= !pagetagDatabase.showfieldsettings[focusIndex];
		//now update the suffix
		if (pagetagDatabase.showfieldsettings[focusIndex]){
			focusItem.setSuffix(":[shown]");
		} else {
			focusItem.setSuffix(":[hidden]");				
		}
		updateAfterShowHideAndOrderOptions(focusItem);
		propertiesdirty=true;
		return true;
	}

	/**
	 * 
	 */
	private boolean handleMenuRightAtSearchTags() {

		//if the tagoperation list is current, then take action
		// else show the options
		if (tagsearchoptMenuDriver.menuBrowseList.isCurrent()){
			switch (tagsearchoptMenuDriver.menuBrowseList.getFocusIndex()){
				case 0:
					//delete the tag
					int itemtodelete=tagsearchMenuDriver.menuBrowseList.getFocusIndex();
					int databaseid=pagetagDatabase.getDatabaseEntryForSearchBrowseList(itemtodelete);
					int areaid=pagetagDatabase.Database[databaseid].regionid;
					long paaddress=pagetagDatabase.Database[databaseid].pageaddress;
					pagetagDatabase.deleteItemFromSearchBrowseList(itemtodelete);
					deleteTagRegion(areaid,paaddress);
					displayMessage("** Deleted **","SL_CA",2);
					int currentitemcount=pagetagDatabase.getItemCount();
					int searchlistsize=pagetagDatabase.tagsearchvectorItems.size();
					if (currentitemcount>0 && searchlistsize>0 ){
						if (itemtodelete == searchlistsize){
							tagsearchMenuDriver.menuBrowseList.setFocusItem(searchlistsize-1);
						} 					
						this.display.setCurrent(tagsearchMenuDriver.menuBrowseList);
						//need to call activate to update the browse menu suffix
						activateBrowseAndSearchMenu();
					} else {
						/*there are no items to show
						 * deactivate the browse and search menu
						 * reset the appmode to menu selection
						 */ 
						if (currentitemcount==0){
							deactivateBrowseAndSearchMenu();
						} else {
							//need to call activate to update the browse menu suffix
							activateBrowseAndSearchMenu();
						}
						this.display.setCurrent(mypenletMenuDriver.menuBrowseList);
						appMode=appmodeMenuSelection;
					}
					/* the browse list focus item might have been deleted. 
					 * if this is out of range, then adjust it to the bottom
					 */
					if (currentitemcount>0){
						int focusindex=tagbrowseMenuDriver.menuBrowseList.getFocusIndex();
						if (focusindex==tagbrowseMenuDriver.menuBrowseList.getItemCount()){
							tagbrowseMenuDriver.menuBrowseList.setFocusItem(focusindex-1);
						}
					}
					
			}
		} else {
			this.display.setCurrent(tagsearchoptMenuDriver.menuBrowseList);
		}
		return true;
	}

	/**
	 * 
	 */
	private boolean handleMenuRightAtBrowseTags() {
		//if the tagoperation list is current, then take action
		// else show the options
		if (tagbrowseoptMenuDriver.menuBrowseList.isCurrent()){
			switch (tagbrowseoptMenuDriver.menuBrowseList.getFocusIndex()){
				case 0:
					//delete the tag
					int itemtodelete=tagbrowseMenuDriver.menuBrowseList.getFocusIndex();
					int databaseid=pagetagDatabase.getDatabaseEntryForTagBrowseList(itemtodelete);
					int areaid=pagetagDatabase.Database[databaseid].regionid;
					long paaddress=pagetagDatabase.Database[databaseid].pageaddress;
					pagetagDatabase.deleteItemFromTagBrowseList(itemtodelete);
					deleteTagRegion(areaid,paaddress);
					displayMessage("** Deleted **","SL_CA",2);
					int currentitemcount=pagetagDatabase.getItemCount();
					if (currentitemcount>0){
						if (itemtodelete == currentitemcount){
							tagbrowseMenuDriver.menuBrowseList.setFocusItem(currentitemcount-1);
						} 					
						//need to call activate to update the browse menu suffix
						activateBrowseAndSearchMenu();
						this.display.setCurrent(tagbrowseMenuDriver.menuBrowseList);
					} else {
						/*there are no items to show
						 * deactivate the browse and search menu
						 * reset the appmode to menu selection
						 */ 
						deactivateBrowseAndSearchMenu();
						this.display.setCurrent(mypenletMenuDriver.menuBrowseList);
					}
			}
		} else {
			this.display.setCurrent(tagbrowseoptMenuDriver.menuBrowseList);
		}
		return true;
	}

	/**
	 * @param focusIndex
	 */
	private boolean handleMenuRightAtMainMenu(int focusIndex) {
		penStrokeFilter=false;
		switch (focusIndex){
		case 0:
			//new tag
			newTag();
			return true;
		case 2:
			//search tag
			if (pagetagDatabase.getItemCount()>0) {
				searchTag();
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 */
	private void searchTag() {
		mypenletMenuDriver.menuBrowseList.setFocusItem(2);
		
		if (pagetagDatabase.currentItemCount>0){
			appMode=appmodeSearchTag;
			this.label.draw(srchPrompt, true);
			this.display.setCurrent(this.label);
		} else {
			this.display.setCurrent(mypenletMenuDriver.menuBrowseList);
		}
	}

	/**
	 * 
	 */
	private void newTag() {
		mypenletMenuDriver.menuBrowseList.setFocusItem(0);
		if (pagetagDatabase.hasSpace()){
			appMode=appmodeNewTag;
			tagRectangle=null;
			this.label.draw(tagPrompt, true);
			this.display.setCurrent(this.label);
		} else {
			displayMessage("! Full-Del+Exit !","SL_Error", 2);
		}
	}

	private void UCASETutor(){
		appMode=appmodeHelpUCASE;
		activateTutor(UCASEPageTagTutor);
	}
	
	private void lcaseTutor(){
		appMode=appmodeHelplcase;
		activateTutor(lcasePageTagTutor);
	}

	private void numberTutor(){
		appMode=appmodeHelpnumber;
		activateTutor(numberPageTagTutor);
	}
	private void activateTutor(PageTagTutor tutor){
		currentTutor=tutor;
		TutorTest=currentTutor.getPrevString();
		if (TutorTest.length()==0){
			currentTutor.ilastTutorPosition=0;
			TutorTest=currentTutor.getNextString();
		}
		TutorPrompt="Write:"+TutorTest;
		TutorResult="";
		updateDisplay(TutorPrompt);
		createICRContext(currentTutor.icrContextName);
	}

	private void displayMessage(String msg, String sound,int wait) {
		
		Displayable currentDisplay=display.getCurrent();
		ScrollLabel msglabel=new ScrollLabel();

		if (sound.length()>0){
			interfaceHelper.playSound(sound);
		}
		msglabel.draw(msg,true);
		display.setCurrent(msglabel);
		try {
			Thread.currentThread();
			Thread.sleep(1000*wait);
		} catch (InterruptedException e) {
			logger.debug("sleep="+e.getMessage());
		}
		this.display.setCurrent(currentDisplay);
	}

	/**
	 * @param focusItem
	 */
	private void updateAfterShowHideAndOrderOptions(MenuBrowseListItem focusItem) {
		//refresh the display
		this.display.setCurrent(focusItem.myMenuDriver.menuBrowseList);

		//update the fields to be displayed
		pagetagDatabase.createShowFieldOrder();

		//update all of the data in the browser list
		pagetagDatabase.updateTagBrowseList();
		//create the submenu for the browse items
	}

	public void penDown(long time, Region areaID, PageInstance pageInstance) {

		int id=areaID.getAreaId();
		String docTitle=pageInstance.getDocument().getTitle().toString();
		if (docTitle.compareTo("PageTagPaper")==0){
			switch (id){
			//handle the menu control regions
			case 31:
				interfaceHelper.playSound("SL_Ack");
				newTag();
				break;
			case 32:
				interfaceHelper.playSound("SL_Ack");
				gotoQuickTagMenu();
				break;
			case 33:
				interfaceHelper.playSound("SL_Ack");
				searchTag();
				break;
			case 34:
				interfaceHelper.playSound("SL_Ack");
				gotoBrowseTagMenu();
				break;
			case 35:
				interfaceHelper.playSound("SL_Ack");
				this.context.notifyStateChange(false);
				break;	
			//handle the data view toggle regions
			case 41:
			case 42:
			case 43:
			case 44:
			case 45:
			case 46:
				int index=id-41;

				interfaceHelper.playSound("SL_Ack");
				
		        //sub menu for option
		        MenuBrowseListItem item1=(MenuBrowseListItem)mypenletMenuDriver.vectorMenuItems.elementAt(4);
		        //sub menu for show tag data sub option
		        MenuBrowseListItem item2=(MenuBrowseListItem)item1.subMenuDriver.vectorMenuItems.elementAt(0);
		        // field that is to be toggled
		        MenuBrowseListItem item3=(MenuBrowseListItem)item2.subMenuDriver.vectorMenuItems.elementAt(index);
		        handleMenuRightAtOptionShowHide(item3,index);
		        //set focus to this item & show the menu
		        item2.subMenuDriver.menuBrowseList.setFocusItem(index);
		        //set focus to select tag data in the parent menu
		        item1.subMenuDriver.menuBrowseList.setFocusItem(0);
		        this.display.setCurrent(item2.subMenuDriver.menuBrowseList);
		        //set the main menu focus item so that menu_left is positioned correctly
		        mypenletMenuDriver.menuBrowseList.setFocusItem(4);
		        appMode=item2.myMenuDriver.appMode;
		        this.icrContext.clearStrokes();
		        tagRectangle=null;
		        tagText="";
		        searchText="";
		        break;
			default:
				interfaceHelper.playSound("SL_Ack");
				updateDisplay(pageInstance.getDocument().getTitle().toString()+"="+Integer.toString(id));
			}
			return;
		}
		if (id!=0){
			String tag=pagetagDatabase.getTagForRegionID(id);
			if (tag==null){
				RegionCollection rc=this.context.getCurrentRegionCollection();
				rc.removeRegion(areaID);
			} else {
				switch (appMode) {
				case appmodeSearchTag:
					searchText=tag;
		    		updateDisplay(srchPrompt+searchText);
					penStrokeFilter=true;
					return;
				default:

					BrowseList menuList=(BrowseList) display.getCurrent();
					MenuBrowseListItem focusItem=(MenuBrowseListItem) menuList.getFocusItem();
					int focusIndex=menuList.getFocusIndex();
					appMode=focusItem.myMenuDriver.appMode;
					if (appMode==appmodeQuickTag){
						//update the tag
						if (focusIndex==0){
							focusItem.setText(tag);
							menuList.setFocusItem(focusIndex);
							/* user has tapped on an existing tag to use it
							 * in the Tap N Pick mode. Clear stale strokes
							 * and set rectangle to null.
							 */
							tagRectangle=null;
							penStrokeFilter=true;
						}
					}
					return;

				
				}
			}
		}
	}

	public void penUp(long time, Region region, PageInstance pageInstance) {

		// No implementation required
		
	}
	
	public void createSearchTagList(){
		
		if (pagetagDatabase.updateTagSearchList(searchText)){
			
			//display message and count found
			int count=tagsearchMenuDriver.menuBrowseList.getItemCount();
			
			if (count>1){
				displayMessage(Integer.toString(count)+" entries found.","SL_CA",2);
			} else {
				displayMessage(Integer.toString(count)+" entry found.","SL_CA",2);
			}
				

			//create the submenu for the search items
	        tagsearchMenuDriver.menuBrowseList.setFocusItem(0);
			this.display.setCurrent(tagsearchMenuDriver.menuBrowseList);
			appMode=appmodeBrowseSearchTag;
			searchText="";
		} else {
			//display that no matches were found
			displayMessage("!! No Matches !!","SL_Error",2);
			
			//clear search text. this will filter out the phantom character
			searchText="";
		}
	}

    public boolean readPageOffsetsFromFile(String filename){
    	PenletStorage penstore=this.context.getInternalPenletStorage();
    	try {
            if (penstore.exists(filename)){

        			InputStream inStream=penstore.openInputStream(filename);
        	        if (inStream.available()>1){
        	        	DataInputStream dinStream=new DataInputStream(inStream);

        	        	if (adjustReadPosition(dinStream)){
        	        		readOffsets(dinStream);
        	        	}

        	        	dinStream.close();
        	        } else {
        	        	inStream.close();
        	        }
        	        return true;
            	} else {
            		this.logger.debug(filename+" not found!");
            	}
            }  catch (IOException e) {
    			this.logger.debug("open offset datainstream: "+e.getMessage());
    		}
    	return false;
    }

    private boolean adjustReadPosition(DataInputStream dinStream){
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

    public int readOffsets(DataInputStream dinStream){
		
		//now read the items till there are none
		
		pageOffsetSlot=0;
		int offsetcount;
		try{
			offsetcount=dinStream.readInt();
		} catch (IOException e) {
			
			this.logger.debug("No Offset:"+e.getMessage());
			return 0;
		}

		if (pageOffset.length<offsetcount){
			pageOffset=new int[offsetcount+5];
			pageOffsetDocumentName=new String[offsetcount+5];
		}
		
		while (readOffset(pageOffsetSlot,dinStream)){
			pageOffsetSlot++;
		}
		this.logger.debug("offset read read="+Integer.toString(pageOffsetSlot));
		return pageOffsetSlot;
	}
	
	private boolean readOffset(int itempos,DataInputStream dstream){
		try {
			pageOffset[itempos]=dstream.readInt();
			pageOffsetDocumentName[itempos]=dstream.readUTF();
			return true;
		} catch (IOException e) {
			this.logger.debug("read offset: "+e.getMessage());
			return false;
		}
	}

    private boolean writePageOffsetsToFile(String filename){

    	PenletStorage penstore=this.context.getInternalPenletStorage();
    	OutputStream outStream;
    	DataOutputStream doutStream;
    	//open dataoutput stream
        try {
        	if (penstore.exists(filename)){
        		penstore.delete(filename);
        	}
			outStream=penstore.openOutputStream(filename,true);
	        doutStream=new DataOutputStream(outStream);
		} catch (IOException e) {
			this.logger.debug("open offset dataoutstream: "+e.getMessage());
			return false;
		}
		
		try{
			//write the code
			doutStream.writeByte(8);
			doutStream.writeByte(57);
			
			//write the pageoffset count
			doutStream.writeInt(pageOffsetSlot);
			
			//write the pageoffsets
			for (int i=0;i<pageOffsetSlot;i++){
				doutStream.writeInt(pageOffset[i]);
				doutStream.writeUTF(pageOffsetDocumentName[i]);
			}
			doutStream.close();
			return true;
			
		} catch (IOException e){
			
		}
    	return false;
    	
    }

	// *** GENERATED METHOD -- DO NOT MODIFY ***
	/**
	 * Responsible for delegating generic PEN_DOWN events to area specific pen down methods based upon the area in which
	 * they occurred. This method is generated and managed by the penlet project nature. Users should modify the
	 * individual area event handlers (e.g. on<AreaName>PenDown) or the generic event handler (e.g. penDown)).
	
	 * @param time The time at which the event occurred
	 * @param region The region in which the event occurred
	 * @param page The page on which the event occurred
	 * @return true if the event was successfully handled, false otherwise
	 */
	protected boolean penDownEventDelegator(long time, Region region, PageInstance page) {
		boolean eventHandled = true;
		switch (region.getAreaId()) {
	case 2:
				eventHandled = onArea7PenDown(time, region, page);
				break;
			default:
			eventHandled = false;
		}
		return eventHandled;
	}
	// *** END OF GENERATED CODE ***

	protected boolean onArea7PenDown(long time, Region region, PageInstance page) {
		return true;
	}
}
