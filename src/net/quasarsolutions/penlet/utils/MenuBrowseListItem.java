package net.quasarsolutions.penlet.utils;

import java.io.InputStream;
import java.util.Vector;

import com.livescribe.configuration.Config;
import com.livescribe.display.BrowseList;
import com.livescribe.display.Display;
import com.livescribe.display.Image;
import com.livescribe.penlet.Logger;
import com.livescribe.penlet.Penlet;
import com.livescribe.i18n.ResourceBundle;

public class MenuBrowseListItem implements BrowseList.Item{
	
	boolean isMenuSelectable;
	InputStream MenuSound;
	Image MenuIcon;
	String MenuTitle,MenuPrefix,MenuSuffix;
	Logger Log;

	public PenletMenuDriver subMenuDriver,myMenuDriver;

	public MenuBrowseListItem(Logger log,boolean selectable, InputStream stream, Image icon, String title,PenletMenuDriver pMenu){

		isMenuSelectable=selectable;
		MenuIcon=icon;
		MenuSound=stream;
		MenuTitle=title;
		Log=log;
		subMenuDriver=null;
		myMenuDriver=pMenu;
		MenuPrefix=MenuSuffix="";
	}

	public void createSubOptions (Penlet pen,Config configdata,String subMenuPrefix,PenletMenuDriver pMenu, Display disp,int appmode){
		subMenuDriver=new PenletMenuDriver(pen,disp,pMenu,appmode);
		subMenuDriver.makeMenu(configdata,subMenuPrefix);
		
	}

	public void createSubOptions (PenletMenuDriver mDriver, Vector mVector){
		subMenuDriver=mDriver;
		subMenuDriver.makeMenu(mVector);
		
	}

	public void deleteSubOptions(){
		subMenuDriver=null;
	}
	
	public void updateTitle(String title){
		MenuTitle=title;
	}
	
	public boolean isSelectable(){
		return isMenuSelectable;
	}
	
	public void setSelectable(boolean select){
		isMenuSelectable=select;
	}

	public Image getIcon() {
		return MenuIcon;
	}
	
	public void setIcon(Image icon){
		MenuIcon=icon;
	}
	
	public Object getText(){
		return MenuPrefix+MenuTitle+MenuSuffix;
	}

	public void setText(String text){
		MenuTitle=text;
	}

	public void setPrefix(String text){
		MenuPrefix=text;
	}

	public void setSuffix(String text){
		MenuSuffix=text;
	}
	public InputStream getAudioStream(){
		return MenuSound;
	}

	// abstract interface implementation for BrowseList.Item
	public String getAudioMimeType(){
		
		return ResourceBundle.MIME_AUDIO_WAV ;
	}

}

