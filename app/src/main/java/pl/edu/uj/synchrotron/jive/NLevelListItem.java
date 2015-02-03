package pl.edu.uj.synchrotron.jive;

/**
 * Created by lukasz on 25.01.15.
 */

import android.view.View;

/**
 * Class for creating list element of multi-level expandable list view.
 */
public interface NLevelListItem {

    public boolean isExpanded();

    public void toggle();

    public NLevelListItem getParent();

    public View getView();
}