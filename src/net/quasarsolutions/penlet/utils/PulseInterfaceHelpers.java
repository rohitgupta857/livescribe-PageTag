package net.quasarsolutions.penlet.utils;

import java.io.IOException;
import java.io.InputStream;

import com.livescribe.configuration.SystemConfig;
import com.livescribe.display.Display;
import com.livescribe.display.Image;
import com.livescribe.ui.MediaPlayer;
import com.livescribe.ui.ScrollLabel;

public class PulseInterfaceHelpers {
	boolean soundon;
	MediaPlayer player;
	Class penletClass;
	SystemConfig sysConfig;

	//image streams
    InputStream inputstreamImage;
	
	public PulseInterfaceHelpers(MediaPlayer play,boolean soundstatus,Class penclass,SystemConfig sysconfig){
		soundon=soundstatus;
		player=play;
		penletClass=penclass;
		sysConfig=sysconfig;
	}
	
	public void setSound(boolean soundstatus){
		soundon=soundstatus;
	}
	
	public void setMediaPlayer(MediaPlayer play){
		player=play;
	}
	
	public void displayLabel(Display display,ScrollLabel label,Image img, String txt){
		label.draw(img,txt,true);
		display.setCurrent(label);
	}

	public void displayLabel(Display display,ScrollLabel label,String txt,Image img){
		label.draw(txt,img,true);
		display.setCurrent(label);
	}

	public void displayImageAtXY(Display display,ScrollLabel label,Image img,int x,int y){
		label.draw("",img,x,y, img.getWidth(),img.getHeight(),true);
		display.setCurrent(label);
	}


	public void playSound(String soundname){
		if (soundon && !sysConfig.isMuted()){
			soundname="/audio/"+soundname+".wav";
			player.play(soundname,false);
		}
	}

	public void playSound(String[] soundnamearray){
		if (soundon && !sysConfig.isMuted()){
			for (int j=0;j<soundnamearray.length;j++){
				soundnamearray[j]="/audio/"+soundnamearray[j]+".wav";				
			}
			player.play(soundnamearray,false);
		}
	}
	
	public Image loadImage(String imagename){
		
		Image retimage;
		inputstreamImage=penletClass.getResourceAsStream(imagename);
  
    
		try {
		retimage=Image.createImage(inputstreamImage);
		} catch (IOException e) {
			retimage=null;
		}
		return retimage;
	}
}
