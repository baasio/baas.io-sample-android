
package com.kth.baasio.sample.ui;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class PagingFragment extends SherlockFragment implements PagingInterface {

    public abstract boolean hasNext();

    public abstract boolean hasPrev();

    public abstract void next();

    public abstract void prev();

}
