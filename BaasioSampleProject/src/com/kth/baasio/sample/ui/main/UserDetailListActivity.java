
package com.kth.baasio.sample.ui.main;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.sample.R;
import com.kth.baasio.sample.ui.PagingFragment;
import com.kth.baasio.sample.ui.PagingSimpleSinglePaneActivity;

import android.os.Bundle;

public class UserDetailListActivity extends PagingSimpleSinglePaneActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /*
     * (non-Javadoc)
     * @see com.kth.baasio.baassample.ui.SimpleSinglePaneActivity#onCreatePane()
     */
    @Override
    protected PagingFragment onCreatePane() {
        return new UserDetailListFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getSupportMenuInflater().inflate(R.menu.fragment_userdetail, menu);

        MenuItem prev = menu.findItem(R.id.menu_user_prev);
        MenuItem next = menu.findItem(R.id.menu_user_next);

        if (getFragment() != null) {
            if (getFragment().hasNext()) {
                next.setVisible(true);
            } else {
                next.setVisible(false);
            }

            if (getFragment().hasPrev()) {
                prev.setVisible(true);
            } else {
                prev.setVisible(false);
            }
        }

        return true;
    }

}
