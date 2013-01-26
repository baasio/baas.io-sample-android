
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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class UserDetailListFragment extends PagingFragment {
    private ViewGroup mRootView;

    private ListView mListUserDetail;

    private EntityListAdapter mListAdapter;

    private ArrayList<BaasioUser> mEntityList;

    private ImageFetcher mImageFetcher;

    private BaasioQuery mQuery;

    public UserDetailListFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageFetcher = EtcUtils.getImageFetcher(getActivity());

        mEntityList = new ArrayList<BaasioUser>();
        mListAdapter = new EntityListAdapter(getActivity(), mImageFetcher);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_list, null);

        mListUserDetail = (ListView)mRootView.findViewById(R.id.list);
        mListUserDetail.setAdapter(mListAdapter);

        if (ObjectUtils.isEmpty(mQuery)) {
            getEntities();
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

            mEntityList.clear();

            mEntityList.addAll(users);

            mListAdapter.notifyDataSetChanged();

            getActivity().supportInvalidateOptionsMenu();
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
            mQuery.setType(BaasioUser.ENTITY_TYPE);
            mQuery.setLimit(3);
            mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
        }

        mQuery.nextInBackground(mQueryCallback);
    }

    private void getEntities() {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                DialogUtils.showProgressDialog(getActivity(), "query_progress",
                        getString(R.string.progress_dialog_loading));
            }
        });

        if (ObjectUtils.isEmpty(mQuery)) {
            mQuery = new BaasioQuery();
            mQuery.setType(BaasioUser.ENTITY_TYPE);
            mQuery.setLimit(3);
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
            mQuery.setType(BaasioUser.ENTITY_TYPE);
            mQuery.setLimit(3);
            mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
        }

        mQuery.prevInBackground(mQueryCallback);
    }

    public class EntityViewHolder {
        public ViewGroup mRoot;

        public ImageView mProfile;

        public TextView mUsername;

        public TextView mName;

        public TextView mEmail;

        public TextView mCreatedTime;

        public TextView mModifiedTime;
    }

    private class EntityListAdapter extends BaseAdapter {
        private Context mContext;

        private LayoutInflater mInflater;

        private ImageFetcher mImageFetcher;

        public EntityListAdapter(Context context, ImageFetcher imageFetcher) {
            super();

            mContext = context;

            mImageFetcher = imageFetcher;

            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mEntityList.size();
        }

        @Override
        public BaasioUser getItem(int position) {
            return mEntityList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /*
         * (non-Javadoc)
         * @see android.widget.Adapter#getView(int, android.view.View,
         * android.view.ViewGroup)
         */
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            EntityViewHolder view = null;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.listview_item_userdetail_list, parent,
                        false);

                view = new EntityViewHolder();

                view.mRoot = (ViewGroup)convertView.findViewById(R.id.layoutRoot);
                view.mProfile = (ImageView)convertView.findViewById(R.id.imageProfile);
                view.mUsername = (TextView)convertView.findViewById(R.id.textUsername);
                view.mName = (TextView)convertView.findViewById(R.id.textName);
                view.mEmail = (TextView)convertView.findViewById(R.id.textEmail);
                view.mCreatedTime = (TextView)convertView.findViewById(R.id.textCreatedTime);
                view.mModifiedTime = (TextView)convertView.findViewById(R.id.textModifiedTime);

                if (view != null) {
                    convertView.setTag(view);
                }
            } else {
                view = (EntityViewHolder)convertView.getTag();
            }

            BaasioUser entity = mEntityList.get(position);

            if (entity != null) {
                String imageUrl = entity.getPicture();
                if (imageUrl != null) {
                    mImageFetcher.loadImage(imageUrl, view.mProfile, R.drawable.person_image_empty);
                } else {
                    view.mProfile.setImageResource(R.drawable.person_image_empty);
                }

                if (entity.getUsername() != null) {
                    view.mUsername.setText(entity.getUsername());
                }

                String displayName = entity.getName();
                if (!ObjectUtils.isEmpty(entity.getFirstname())
                        && !ObjectUtils.isEmpty(entity.getLastname())) {
                    displayName = displayName + "(" + entity.getFirstname() + " "
                            + entity.getLastname() + ")";
                } else {
                    if (!ObjectUtils.isEmpty(entity.getFirstname())) {
                        displayName = displayName + "(" + entity.getFirstname() + ")";
                    }

                    if (!ObjectUtils.isEmpty(entity.getLastname())) {
                        displayName = displayName + "(" + entity.getLastname() + ")";
                    }
                }
                view.mName.setText(displayName);

                if (entity.getEmail() != null) {
                    view.mEmail.setText(entity.getEmail());
                }

                if (entity.getCreated() != null) {
                    String createdTime = EtcUtils.getDateString(entity.getCreated());
                    if (!TextUtils.isEmpty(createdTime)) {
                        view.mCreatedTime
                                .setText(getString(R.string.created_time_user, createdTime));
                    }
                }

                if (entity.getModified() != null) {
                    String modifiedTime = EtcUtils.getDateString(entity.getModified());
                    if (!TextUtils.isEmpty(modifiedTime)) {
                        view.mModifiedTime.setText(getString(R.string.modified_time_user,
                                modifiedTime));
                    }
                }
            }
            return convertView;
        }

    }
}
