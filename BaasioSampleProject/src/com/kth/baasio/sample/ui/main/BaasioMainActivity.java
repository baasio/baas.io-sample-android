
package com.kth.baasio.sample.ui.main;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.Baas;
import com.kth.baasio.callback.BaasioCallback;
import com.kth.baasio.entity.user.BaasioUser;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.helpcenter.ui.HelpCenterActivity;
import com.kth.baasio.sample.R;
import com.kth.baasio.sample.ui.BaseActivity;
import com.kth.baasio.sample.ui.auth.SignInActivity;
import com.kth.baasio.sample.ui.dialog.DialogUtils;
import com.kth.baasio.sample.ui.dialog.EntityDialogFragment;
import com.kth.baasio.sample.ui.dialog.EntityDialogFragment.EntityDialogResultListener;
import com.kth.baasio.utils.ObjectUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.widget.Toast;

public class BaasioMainActivity extends BaseActivity implements OnPageChangeListener, TabListener {
    public static final int REQUEST_SIGNIN = 1;

    private Context mContext;

    private ViewPager mViewPager;

    private UserFragment mUserFragment;

    private GroupFragment mGroupFragment;

    private PostFragment mPostFragment;

    private PushFragment mPushFragment;

    private FileFragment mFileFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baasio_sample);

        mContext = this;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(false);
        }

        mViewPager = (ViewPager)findViewById(R.id.pager);
        if (mViewPager != null) {
            // Phone setup
            mViewPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));
            mViewPager.setOnPageChangeListener(this);
            mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
            mViewPager.setPageMargin(getResources()
                    .getDimensionPixelSize(R.dimen.page_margin_width));

            final ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            // actionBar.addTab(actionBar.newTab().setText(R.string.auth_title).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(R.string.title_user).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(R.string.title_group).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(R.string.title_post).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(R.string.title_push).setTabListener(this));
            actionBar.addTab(actionBar.newTab().setText(R.string.title_file).setTabListener(this));
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Baas.ACTION_UNAUTHORIZED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SIGNIN) {
                Toast.makeText(mContext, getString(R.string.msg_signin_success), Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getSupportMenuInflater().inflate(R.menu.activity_baas_sample, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem signout = menu.findItem(R.id.menu_signout);
        MenuItem signin = menu.findItem(R.id.menu_signin);

        MenuItem changePassword = menu.findItem(R.id.menu_change_password);

        if (ObjectUtils.isEmpty(Baas.io().getSignedInUser())) {
            if (!ObjectUtils.isEmpty(signout)) {
                signout.setVisible(false);
                signin.setVisible(true);

                changePassword.setVisible(false);
            }
        } else {
            if (!ObjectUtils.isEmpty(signout)) {
                signout.setVisible(true);
                signin.setVisible(false);

                changePassword.setVisible(true);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_settings: {
                break;
            }
            case R.id.menu_helpdesk: {
                Intent intent = new Intent(mContext, HelpCenterActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.menu_signin: {
                Baas.io().fireUnauthorized();
                break;
            }
            case R.id.menu_signout: {
                BaasioUser.signOut(mContext);
                break;
            }
            case R.id.menu_change_password: {
                EntityDialogFragment fragment = DialogUtils.showEntityDialog(this,
                        "change_password", EntityDialogFragment.CHANGE_PASSWORD);

                unregisterReceiver(mReceiver);

                fragment.setEntityDialogResultListener(new EntityDialogResultListener() {

                    @Override
                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                        String oldPassword = data.getString("text1");
                        String newPassword = data.getString("text2");

                        BaasioUser.changePasswordInBackground(oldPassword, newPassword,
                                new BaasioCallback<Boolean>() {

                                    @Override
                                    public void onResponse(Boolean response) {
                                        Toast.makeText(BaasioMainActivity.this,
                                                "Password changed successfully!", Toast.LENGTH_LONG)
                                                .show();

                                        IntentFilter filter = new IntentFilter();
                                        filter.addAction(Baas.ACTION_UNAUTHORIZED);
                                        registerReceiver(mReceiver, filter);
                                    }

                                    @Override
                                    public void onException(BaasioException e) {
                                        Toast.makeText(BaasioMainActivity.this,
                                                "changePasswordInBackground =>" + e.toString(),
                                                Toast.LENGTH_LONG).show();

                                        IntentFilter filter = new IntentFilter();
                                        filter.addAction(Baas.ACTION_UNAUTHORIZED);
                                        registerReceiver(mReceiver, filter);
                                    }
                                });
                        return false;
                    }
                });
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private class MainPagerAdapter extends FragmentPagerAdapter {
        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    mUserFragment = new UserFragment();
                    return mUserFragment;

                case 1:
                    mGroupFragment = new GroupFragment();
                    return mGroupFragment;

                case 2:
                    mPostFragment = new PostFragment();
                    return mPostFragment;

                case 3:
                    mPushFragment = new PushFragment();
                    return mPushFragment;

                case 4:
                    mFileFragment = new FileFragment();
                    return mFileFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 5;
        }
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#
     * onPageScrollStateChanged(int)
     */
    @Override
    public void onPageScrollStateChanged(int arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see
     * android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled
     * (int, float, int)
     */
    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see
     * android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected
     * (int)
     */
    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.ActionBar.TabListener#onTabSelected(com.
     * actionbarsherlock.app.ActionBar.Tab,
     * android.support.v4.app.FragmentTransaction)
     */
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.ActionBar.TabListener#onTabUnselected(com.
     * actionbarsherlock.app.ActionBar.Tab,
     * android.support.v4.app.FragmentTransaction)
     */
    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.ActionBar.TabListener#onTabReselected(com.
     * actionbarsherlock.app.ActionBar.Tab,
     * android.support.v4.app.FragmentTransaction)
     */
    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub

    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent intent2 = new Intent(mContext, SignInActivity.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            startActivityForResult(intent2, REQUEST_SIGNIN);
        }

    };
}
