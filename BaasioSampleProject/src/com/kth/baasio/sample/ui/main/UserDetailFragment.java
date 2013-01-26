
package com.kth.baasio.sample.ui.main;

import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.callback.BaasioQueryCallback;
import com.kth.baasio.entity.BaasioBaseEntity;
import com.kth.baasio.entity.user.BaasioUser;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.query.BaasioQuery;
import com.kth.baasio.query.BaasioQuery.ORDER_BY;
import com.kth.baasio.sample.R;
import com.kth.baasio.sample.cache.ImageFetcher;
import com.kth.baasio.sample.ui.PagingFragment;
import com.kth.baasio.sample.ui.dialog.DialogUtils;
import com.kth.baasio.sample.utils.EtcUtils;
import com.kth.baasio.utils.ObjectUtils;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class UserDetailFragment extends PagingFragment {
    public static final int REQUEST_ADD_USER = 1;

    public static final String ENTITY_TYPE = "user";

    private ViewGroup mRootView;

    private ImageView mImageProfile;

    private TextView mTextUsername;

    private TextView mTextName;

    private TextView mTextEmail;

    private TextView mTextCreated;

    private TextView mTextModified;

    private ScrollView mScrollFacebook;

    private TextView mtextFacebookUsername;

    private TextView mtextFacebookUpdatedTime;

    private TextView mtextFacebookEmail;

    private TextView mtextFacebookLink;

    private TextView mtextFacebookFirstname;

    private TextView mtextFacebookLastname;

    private TextView mtextFacebookTimezone;

    private ImageFetcher mImageFetcher;

    private BaasioQuery mQuery;

    private BaasioUser mUser;

    public UserDetailFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageFetcher = EtcUtils.getImageFetcher(getActivity());

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_userdetail, null);

        mImageProfile = (ImageView)mRootView.findViewById(R.id.imageProfile);
        mTextUsername = (TextView)mRootView.findViewById(R.id.textUsername);
        mTextName = (TextView)mRootView.findViewById(R.id.textName);
        mTextEmail = (TextView)mRootView.findViewById(R.id.textEmail);
        mTextCreated = (TextView)mRootView.findViewById(R.id.textCreated);
        mTextModified = (TextView)mRootView.findViewById(R.id.textModified);

        mScrollFacebook = (ScrollView)mRootView.findViewById(R.id.scrollFacebook);
        mtextFacebookUsername = (TextView)mRootView.findViewById(R.id.textFacebookUsername);
        mtextFacebookUpdatedTime = (TextView)mRootView.findViewById(R.id.textFacebookUpdatedTime);
        mtextFacebookEmail = (TextView)mRootView.findViewById(R.id.textFacebookEmail);
        mtextFacebookLink = (TextView)mRootView.findViewById(R.id.textFacebookLink);
        mtextFacebookFirstname = (TextView)mRootView.findViewById(R.id.textFacebookFirstname);
        mtextFacebookLastname = (TextView)mRootView.findViewById(R.id.textFacebookLastname);
        mtextFacebookTimezone = (TextView)mRootView.findViewById(R.id.textFacebookTimezone);

        if (ObjectUtils.isEmpty(mQuery)) {
            getEntityes();
        } else {
            refreshUserData();
        }

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
            com.actionbarsherlock.view.MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_userdetail, menu);

        MenuItem prev = menu.findItem(R.id.menu_user_prev);
        MenuItem next = menu.findItem(R.id.menu_user_next);

        if (hasNext()) {
            next.setVisible(true);
        } else {
            next.setVisible(false);
        }

        if (hasPrev()) {
            prev.setVisible(true);
        } else {
            prev.setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_user_next: {
                next();
                return true;
            }
            case R.id.menu_user_prev: {
                prev();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshUserData() {
        String imageUrl = mUser.getPicture();
        if (imageUrl != null) {
            mImageFetcher.loadImage(imageUrl, mImageProfile, R.drawable.person_image_empty);
        } else {
            mImageProfile.setImageResource(R.drawable.person_image_empty);
        }

        if (!ObjectUtils.isEmpty(mUser.getUsername())) {
            mTextUsername.setText(mUser.getUsername());
        }

        String displayName = mUser.getName();
        if (!ObjectUtils.isEmpty(mUser.getFirstname()) && !ObjectUtils.isEmpty(mUser.getLastname())) {
            displayName = displayName + "(" + mUser.getFirstname() + " " + mUser.getLastname()
                    + ")";
        } else {
            if (!ObjectUtils.isEmpty(mUser.getFirstname())) {
                displayName = displayName + "(" + mUser.getFirstname() + ")";
            }

            if (!ObjectUtils.isEmpty(mUser.getLastname())) {
                displayName = displayName + "(" + mUser.getLastname() + ")";
            }
        }
        mTextName.setText(displayName);

        if (!ObjectUtils.isEmpty(mUser.getEmail())) {
            mTextEmail.setText(mUser.getEmail());
        } else {
            mTextEmail.setText("");
        }
        String created = EtcUtils.getSimpleDateString(mUser.getCreated());
        mTextCreated.setText("Signed-up at " + created);

        String modified = EtcUtils.getSimpleDateString(mUser.getModified());
        mTextModified.setText("Profile modified at " + modified);

        if (!ObjectUtils.isEmpty(mUser.getFacebook())) {
            mScrollFacebook.setVisibility(View.VISIBLE);
            if (!ObjectUtils.isEmpty(mUser.getFacebook().getUsername())) {
                if (!ObjectUtils.isEmpty(mUser.getFacebook().getGender())) {
                    mtextFacebookUsername.setText("Username: " + mUser.getFacebook().getUsername()
                            + " (" + mUser.getFacebook().getGender() + ")");
                } else {
                    mtextFacebookUsername.setText("Username: " + mUser.getFacebook().getUsername());
                }
            } else {
                if (!ObjectUtils.isEmpty(mUser.getFacebook().getGender())) {
                    mtextFacebookUsername.setText("Username: *Anonymous* ("
                            + mUser.getFacebook().getGender() + ")");
                } else {
                    mtextFacebookUsername.setText("Username: *Anonymous*");
                }
            }
            if (!ObjectUtils.isEmpty(mUser.getFacebook().getUpdatedTime())) {
                mtextFacebookUpdatedTime.setText("Updated at "
                        + mUser.getFacebook().getUpdatedTime());
            }
            if (!ObjectUtils.isEmpty(mUser.getFacebook().getEmail())) {
                mtextFacebookEmail.setText("Email: " + mUser.getFacebook().getEmail());
            }

            if (!ObjectUtils.isEmpty(mUser.getFacebook().getLink())) {
                mtextFacebookLink.setText("Link: " + mUser.getFacebook().getLink());
            }
            if (!ObjectUtils.isEmpty(mUser.getFacebook().getFirstname())) {
                mtextFacebookFirstname.setText("Firstname :" + mUser.getFacebook().getFirstname());
            }
            if (!ObjectUtils.isEmpty(mUser.getFacebook().getLastname())) {
                mtextFacebookLastname.setText("Lastname: " + mUser.getFacebook().getLastname());
            }
            if (!ObjectUtils.isEmpty(mUser.getFacebook().getTimezone())) {
                if (mUser.getFacebook().getTimezone() > 0) {
                    mtextFacebookTimezone.setText("Timezone: +"
                            + mUser.getFacebook().getTimezone().toString());
                } else {
                    mtextFacebookTimezone.setText("Timezone: "
                            + mUser.getFacebook().getTimezone().toString());
                }
            }
        } else {
            mScrollFacebook.setVisibility(View.GONE);
        }

        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public boolean hasNext() {
        if (mQuery != null) {
            return mQuery.hasNextEntities();
        }
        return false;
    }

    @Override
    public boolean hasPrev() {
        if (mQuery != null) {
            return mQuery.hasPrevEntities();
        }
        return false;
    }

    private BaasioQueryCallback mQueryCallback = new BaasioQueryCallback() {

        @Override
        public void onResponse(List<BaasioBaseEntity> entities, List<Object> list,
                BaasioQuery query, long timestamp) {
            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    DialogUtils.dissmissProgressDialog(getActivity(), "query_progress");
                }
            });

            mQuery = query;

            List<BaasioUser> users = BaasioBaseEntity.toType(entities, BaasioUser.class);

            if (users.size() == 1) {
                mUser = users.get(0).toType(BaasioUser.class);
                refreshUserData();
            }
        }

        @Override
        public void onException(BaasioException e) {
            Toast.makeText(getActivity(), "queryOrNextOrPrevInBackground =>" + e.toString(),
                    Toast.LENGTH_LONG).show();
            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    DialogUtils.dissmissProgressDialog(getActivity(), "query_progress");
                }
            });
        }
    };

    @Override
    public void next() {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                DialogUtils.showProgressDialog(getActivity(), "query_progress",
                        getString(R.string.progress_dialog_loading));
            }
        });

        if (ObjectUtils.isEmpty(mQuery)) {
            mQuery = new BaasioQuery();
            mQuery.setType(ENTITY_TYPE);
            mQuery.setLimit(1);
            mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
        }

        mQuery.nextInBackground(mQueryCallback);
    }

    private void getEntityes() {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                DialogUtils.showProgressDialog(getActivity(), "query_progress",
                        getString(R.string.progress_dialog_loading));
            }
        });

        if (ObjectUtils.isEmpty(mQuery)) {
            mQuery = new BaasioQuery();
            mQuery.setType(ENTITY_TYPE);
            mQuery.setLimit(1);
            mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
        }

        mQuery.queryInBackground(mQueryCallback);
    }

    @Override
    public void prev() {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                DialogUtils.showProgressDialog(getActivity(), "query_progress",
                        getString(R.string.progress_dialog_loading));
            }
        });

        if (ObjectUtils.isEmpty(mQuery)) {
            mQuery = new BaasioQuery();
            mQuery.setType(ENTITY_TYPE);
            mQuery.setLimit(1);
            mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
        }

        mQuery.prevInBackground(mQueryCallback);
    }

}
