package net.quasarsolutions.penlet.utils;

import com.livescribe.penlet.Penlet;
import com.livescribe.util.InactivityTimer;

public class QuitAppInactivityTimer extends InactivityTimer {

	Penlet myPen;
	public QuitAppInactivityTimer(Penlet pen) {
		super();
		myPen=pen;
	}

	protected void notifyNoActivity() {
		myPen.logger.debug("change requested");
		if (myPen.getContext().notifyStateChange(false)){
			myPen.logger.debug("changed");
		} else {
			myPen.logger.debug("change denied");
		}

	}
	
	public void recordActivity(){
		myPen.logger.debug("activity");
	}


}
