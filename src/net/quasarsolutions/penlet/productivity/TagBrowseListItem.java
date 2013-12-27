package net.quasarsolutions.penlet.productivity;

import java.io.InputStream;
import net.quasarsolutions.penlet.utils.MenuBrowseListItem;
import net.quasarsolutions.penlet.utils.PenletMenuDriver;
import com.livescribe.display.Image;
import com.livescribe.penlet.Logger;

public class TagBrowseListItem extends MenuBrowseListItem{
	
	int DatabaseEntry;
	
	public TagBrowseListItem(Logger log, boolean selectable,
			InputStream stream, Image icon, String title, PenletMenuDriver menu,int dentry) {
		super(log, selectable, stream, icon, title, menu);
		DatabaseEntry=dentry;
	}
}

