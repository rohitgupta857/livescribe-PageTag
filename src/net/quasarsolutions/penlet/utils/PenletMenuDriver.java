package net.quasarsolutions.penlet.utils;

import java.util.Vector;

import com.livescribe.display.BrowseList;
import com.livescribe.display.Display;
import com.livescribe.event.MenuEvent;
import com.livescribe.penlet.Penlet;
import com.livescribe.configuration.Config;

public class PenletMenuDriver {

    //menu variables
    public Vector vectorMenuItems;
    public BrowseList menuBrowseList;
    public PenletMenuDriver parentOptionMenuDriver;
    private int menuOptions=0;
    public int appMode;
    Penlet myPenlet;
    Display myDisplay;
    String [] menuSounds;
    
    public PenletMenuDriver(Penlet pen, Display disp,PenletMenuDriver pMenu,int appmode){
    	myPenlet=pen;
    	myDisplay=disp;
    	parentOptionMenuDriver=pMenu;
    	appMode=appmode;
    }
    
	public void makeMenu(Config ConfigData,String menuPrefix) {
		String menuStringName,menuTitle,menuSoundName,menuSelectableName,menuSelectable;
		boolean menuIsSelectable;
		// add items to the menu
		
		// first get the number of items
		menuOptions=(int) ConfigData.getLongValue(menuPrefix+"Count");
		
		if (menuOptions > 0) {

			vectorMenuItems=new Vector();
			menuSounds=new String[menuOptions];
			for (int i=0;i<menuOptions;i++) {
				
				menuStringName=menuPrefix+Integer.toString(i);
				menuSoundName=menuStringName+"Sound";
				menuSelectableName=menuStringName+"Selectable";

				//read the information from the config file
				
				menuTitle=ConfigData.getStringValue(menuStringName);
				if (menuTitle==null){
					menuTitle="!"+menuStringName+"!";
				}
				
				menuSounds[i]=ConfigData.getStringValue(menuSoundName);
				if (menuSounds[i]==null){
					menuSounds[i]="";
				}
				
				menuSelectable=ConfigData.getStringValue(menuSelectableName);

				if (menuSelectable==null){
					menuSelectable="False";
					menuTitle+="!NOSEL!";
				}
				
				menuIsSelectable=true;
				if (menuSelectable.compareTo("True")!=0){
					menuIsSelectable=false;
				}

				MenuBrowseListItem item=new MenuBrowseListItem(myPenlet.logger,menuIsSelectable,null,null,menuTitle,this);
				vectorMenuItems.addElement(item);
			}			
			menuBrowseList = new BrowseList(vectorMenuItems,null);
		}
	}
	
	public void makeMenu(Vector menuVector){

		vectorMenuItems=menuVector;
		menuBrowseList = new BrowseList(vectorMenuItems,null);
	}

	public boolean processMenuEvent(MenuEvent menuEvent){

		/* check if this is the active menu. if not, 
		 * then pass on the value to the sub menu
		 */
		MenuBrowseListItem item;
		item=(MenuBrowseListItem) menuBrowseList.getFocusItem();
		
		
		if (menuBrowseList.isCurrent()){
		
			switch (menuEvent.eventId) {
			case MenuEvent.MENU_DOWN:
				menuBrowseList.focusToNext();
				return true;
			case MenuEvent.MENU_UP:
				menuBrowseList.focusToPrevious();
				return true;
			case MenuEvent.MENU_RIGHT:
				/* get current item and check if it is selectable
				 * if selectable, check if it is has a sub menu
				 * if there is a sub menu then switch the active display
				 * to the submenu
				 */
				if (item.isSelectable()){
					if (item.subMenuDriver !=null){
						myDisplay.setCurrent(item.subMenuDriver.menuBrowseList);
						return true;
					}
					return false;
				}
				return false;
			case MenuEvent.MENU_LEFT:
				if (item.myMenuDriver.parentOptionMenuDriver!=null){
					myDisplay.setCurrent(item.myMenuDriver.parentOptionMenuDriver.menuBrowseList);
					return true;
				}
				return false;
			case MenuEvent.MENU_SELECT:
				return false;
			default: 
			}
			return true;
		} else { 
			/* there should be a sub menu to the item in focus
			 * locate that item, and pass this event to the
			 * process menu handler for that sub menu
			 */
			if (item.isSelectable()){
				if (item.subMenuDriver !=null){
					return item.subMenuDriver.processMenuEvent(menuEvent);
				}
			}
		}
		
		return false;
	}

	public boolean isMenuSelectable(){

		return ((MenuBrowseListItem)menuBrowseList.getFocusItem()).isSelectable();

	}
}
