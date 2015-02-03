package pl.edu.uj.synchrotron.jive;

/**
 * Created by lukasz on 25.01.15.
 */

import android.view.View;

/**
 * Class for creating view of multi-level expandable list view.
 */
public interface NLevelView {

    public View getView(NLevelItem item);
}