
package com.kth.baasio.sample.ui.main;

import static com.kth.common.utils.LogUtils.LOGV;
import static com.kth.common.utils.LogUtils.makeLogTag;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.callback.BaasioCallback;
import com.kth.baasio.callback.BaasioQueryCallback;
import com.kth.baasio.entity.BaasioBaseEntity;
import com.kth.baasio.entity.group.BaasioGroup;
import com.kth.baasio.entity.push.BaasioMessage;
import com.kth.baasio.entity.push.BaasioPush;
import com.kth.baasio.entity.user.BaasioUser;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.query.BaasioQuery;
import com.kth.baasio.query.BaasioQuery.ORDER_BY;
import com.kth.baasio.sample.R;
import com.kth.baasio.sample.cache.ImageFetcher;
import com.kth.baasio.sample.ui.dialog.DialogUtils;
import com.kth.baasio.sample.ui.dialog.EntityDialogFragment;
import com.kth.baasio.sample.ui.dialog.EntityDialogFragment.EntityDialogResultListener;
import com.kth.baasio.sample.ui.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.kth.baasio.sample.ui.pulltorefresh.PullToRefreshListView;
import com.kth.baasio.sample.utils.EtcUtils;
import com.kth.baasio.sample.utils.actionmodecompat.ActionMode;
import com.kth.baasio.sample.utils.actionmodecompat.ActionMode.Callback;
import com.kth.baasio.utils.JsonUtils;
import com.kth.baasio.utils.ObjectUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class UserFragment extends SherlockFragment implements OnRefreshListener, Callback {

    private static final String TAG = makeLogTag(UserFragment.class);

    public static final int REQUEST_ADD_USER = 1;

    private ViewGroup mRootView;

    private ImageFetcher mImageFetcher;

    private PullToRefreshListView mPullToRefreshList;

    private ListView mList;

    private TextView mEmptyList;

    private EntityListAdapter mListAdapter;

    private ArrayList<BaasioUser> mEntityList;

    private BaasioQuery mQuery;

    private ActionMode mActionMode;

    private View mLongClickedView;

    private Integer mLongClickedPosition;

    private BaasioGroup mGroup;

    private boolean mIsAddUser;

    public UserFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageFetcher = EtcUtils.getImageFetcher(getActivity());

        mEntityList = new ArrayList<BaasioUser>();
        mListAdapter = new EntityListAdapter(getActivity(), mImageFetcher);

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            String groupString = intent.getStringExtra("group");

            if (!ObjectUtils.isEmpty(groupString)) {
                mGroup = JsonUtils.parse(groupString, BaasioGroup.class);
            }

            String title = intent.getStringExtra(Intent.EXTRA_TITLE);

            if (getString(R.string.title_activity_add_user).equals(title)) {
                mIsAddUser = true;
            }
        }

        getEntities(false);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_pulltorefresh_list, null);

        mEmptyList = (TextView)mRootView.findViewById(R.id.textEmptyList);
        if (ObjectUtils.isEmpty(mGroup)) {
            mEmptyList.setText(getString(R.string.empty_user_list));
        } else {
            mEmptyList.setText(getString(R.string.empty_group_user_list));
        }

        mPullToRefreshList = (PullToRefreshListView)mRootView.findViewById(R.id.list);
        mPullToRefreshList.setOnRefreshListener(this);

        mList = mPullToRefreshList.getRefreshableView();

        mList.setAdapter(mListAdapter);

        if (!ObjectUtils.isEmpty(mQuery)) {
            if (mQuery.hasNextEntities()) {
                if (mPullToRefreshList != null) {
                    mPullToRefreshList.setHasMoreData(true);
                    mPullToRefreshList.setFooterVisible();
                }

            } else {
                if (mPullToRefreshList != null) {
                    mPullToRefreshList.setHasMoreData(false);
                    mPullToRefreshList.setFooterGone();
                }

            }
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
        if (ObjectUtils.isEmpty(mGroup)) {
            if (!mIsAddUser) {
                inflater.inflate(R.menu.fragment_user, menu);
            }
        } else {
            inflater.inflate(R.menu.fragment_user_for_group, menu);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        // TODO Auto-generated method stub
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_userdetail: {
                Intent intent = new Intent(getActivity(), UserDetailActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.menu_userdetail_list: {
                Intent intent = new Intent(getActivity(), UserDetailListActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.menu_add_user: {
                Intent intent = new Intent(getActivity(), AddUserToGroupActivity.class);
                intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_activity_add_user));

                startActivityForResult(intent, REQUEST_ADD_USER);
                return true;
            }
            case R.id.menu_push_send: {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "entity_dialog", EntityDialogFragment.SEND_PUSH);
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String body = data.getString("text1");
                                        boolean android = data.getBoolean("android");
                                        boolean ios = data.getBoolean("ios");
                                        String tag = data.getString("text2");

                                        BaasioMessage message = new BaasioMessage();
                                        if (tag != null && !ObjectUtils.isEmpty(tag.trim())) {
                                            message.setTarget(BaasioMessage.TARGET_TYPE_TAG);
                                            message.setTo(tag.trim());
                                        }

                                        message.setMessage(body, null, null);

                                        int flag = 0;
                                        if (android) {
                                            flag |= BaasioMessage.PLATFORM_FLAG_TYPE_GCM;
                                        }
                                        if (ios) {
                                            flag |= BaasioMessage.PLATFORM_FLAG_TYPE_IOS;
                                        }
                                        message.setPlatform(flag);

                                        BaasioPush.sendPushInBackground(message,
                                                new BaasioCallback<BaasioMessage>() {

                                                    @Override
                                                    public void onException(BaasioException e) {
                                                        Toast.makeText(
                                                                getActivity(),
                                                                "sendPushInBackground =>"
                                                                        + e.toString(),
                                                                Toast.LENGTH_LONG).show();
                                                    }

                                                    @Override
                                                    public void onResponse(BaasioMessage response) {
                                                        if (response != null) {
                                                            Toast.makeText(
                                                                    getActivity(),
                                                                    "sendPushInBackground: Success: "
                                                                            + response.toString(),
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });

                                        return false;
                                    }
                                });
                    }
                }, 100);

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_ADD_USER) {
                String userString = data.getStringExtra("user");

                BaasioUser user = JsonUtils.parse(userString, BaasioUser.class);
                mGroup.addInBackground(user, new BaasioCallback<BaasioUser>() {

                    @Override
                    public void onException(BaasioException e) {
                        Toast.makeText(getActivity(), "addInBackground =>" + e.toString(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onResponse(BaasioUser response) {
                        if (!ObjectUtils.isEmpty(response)) {
                            mEntityList.add(0, response);

                            mListAdapter.notifyDataSetChanged();

                            if (mEntityList.isEmpty()) {
                                mEmptyList.setVisibility(View.VISIBLE);
                            } else {
                                mEmptyList.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        }
    }

    private BaasioQueryCallback mQueryCallback = new BaasioQueryCallback() {

        @Override
        public void onResponse(List<BaasioBaseEntity> entities, List<Object> list,
                BaasioQuery query, long timestamp) {
            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    if (mPullToRefreshList != null) {
                        if (mPullToRefreshList.isRefreshing())
                            mPullToRefreshList.onRefreshComplete();
                    }

                }
            });

            mEntityList.clear();

            mQuery = query;

            List<BaasioUser> users = BaasioBaseEntity.toType(entities, BaasioUser.class);
            mEntityList.addAll(users);

            mListAdapter.notifyDataSetChanged();

            if (mEntityList.isEmpty()) {
                mEmptyList.setVisibility(View.VISIBLE);
            } else {
                mEmptyList.setVisibility(View.GONE);
            }

            if (mQuery.hasNextEntities()) {
                if (mPullToRefreshList != null) {
                    mPullToRefreshList.setHasMoreData(true);
                    mPullToRefreshList.setFooterVisible();
                }

            } else {
                if (mPullToRefreshList != null) {
                    mPullToRefreshList.setHasMoreData(false);
                    mPullToRefreshList.setFooterGone();
                }

            }
        }

        @Override
        public void onException(BaasioException e) {
            Toast.makeText(getActivity(), "queryInBackground =>" + e.toString(), Toast.LENGTH_LONG)
                    .show();

            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    if (mPullToRefreshList != null) {
                        if (mPullToRefreshList.isRefreshing())
                            mPullToRefreshList.onRefreshComplete();
                    }
                }
            });

        }
    };

    private BaasioQueryCallback mQueryNextCallback = new BaasioQueryCallback() {

        @Override
        public void onResponse(List<BaasioBaseEntity> entities, List<Object> list,
                BaasioQuery query, long timestamp) {

            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    if (mPullToRefreshList != null) {
                        mPullToRefreshList.setIsLoading(false);
                    }
                }
            });

            mQuery = query;

            List<BaasioUser> users = BaasioBaseEntity.toType(entities, BaasioUser.class);
            mEntityList.addAll(users);

            mListAdapter.notifyDataSetChanged();

            if (mEntityList.isEmpty()) {
                mEmptyList.setVisibility(View.VISIBLE);
            } else {
                mEmptyList.setVisibility(View.GONE);
            }

            if (mQuery.hasNextEntities()) {
                if (mPullToRefreshList != null) {
                    mPullToRefreshList.setHasMoreData(true);
                    mPullToRefreshList.setFooterVisible();
                }

            } else {
                if (mPullToRefreshList != null) {
                    mPullToRefreshList.setHasMoreData(false);
                    mPullToRefreshList.setFooterGone();
                }

            }
        }

        @Override
        public void onException(BaasioException e) {
            Toast.makeText(getActivity(), "nextInBackground =>" + e.toString(), Toast.LENGTH_LONG)
                    .show();

            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    if (mPullToRefreshList != null) {
                        mPullToRefreshList.setIsLoading(false);
                    }
                }
            });

        }
    };

    private void getEntities(final boolean next) {
        if (ObjectUtils.isEmpty(mQuery)) {
            mQuery = new BaasioQuery();
            if (!ObjectUtils.isEmpty(mGroup)) {
                mQuery.setGroup(mGroup);
                mQuery.setOrderBy(BaasioUser.PROPERTY_USERNAME, ORDER_BY.ASCENDING);
            } else {
                mQuery.setType(BaasioUser.ENTITY_TYPE);
                mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
            }
        }

        if (!next)
            mQuery.queryInBackground(mQueryCallback);
        else
            mQuery.nextInBackground(mQueryNextCallback);
    }

    public class EntityViewHolder {
        public ViewGroup mRoot;

        public ImageView mProfile;

        public TextView mName;

        public TextView mBody;

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
                convertView = mInflater.inflate(R.layout.listview_item_userlist, parent, false);

                view = new EntityViewHolder();

                view.mRoot = (ViewGroup)convertView.findViewById(R.id.layoutRoot);
                view.mProfile = (ImageView)convertView.findViewById(R.id.imageProfile);
                view.mName = (TextView)convertView.findViewById(R.id.textName);
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

                if (!ObjectUtils.isEmpty(entity.getUsername())) {
                    view.mName.setText(entity.getUsername());
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

                if (view.mRoot != null) {
                    if (mIsAddUser) {
                        view.mRoot.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.putExtra("user", mEntityList.get(position).toString());

                                getActivity().setResult(Activity.RESULT_OK, intent);
                                getActivity().finish();
                            }
                        });
                    } else {
                        view.mRoot.setOnLongClickListener(new View.OnLongClickListener() {

                            @Override
                            public boolean onLongClick(View view) {
                                if (mActionMode != null) {
                                    // CAB already displayed, ignore
                                    return true;
                                }

                                mLongClickedView = view;
                                mLongClickedPosition = position;

                                mActionMode = ActionMode.start(getActivity(), UserFragment.this);
                                EtcUtils.setActivatedCompat(mLongClickedView, true);
                                return true;
                            }
                        });
                    }

                }
            }
            return convertView;
        }
    }

    /*
     * (non-Javadoc)
     * @see com.kth.kanu.baassample.view.pulltorefresh.PullToRefreshBase.
     * OnRefreshListener#onRefresh()
     */
    @Override
    public void onRefresh() {
        getEntities(false);
    }

    /*
     * (non-Javadoc)
     * @see com.kth.kanu.baassample.view.pulltorefresh.PullToRefreshBase.
     * OnRefreshListener#onUpdate()
     */
    @Override
    public void onUpdate() {
        getEntities(true);
    }

    /*
     * (non-Javadoc)
     * @see com.kth.kanu.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onCreateActionMode
     * (com.kth.kanu.baassample.utils.actionmodecompat.ActionMode,
     * android.view.Menu)
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (mLongClickedView == null) {
            return true;
        }

        if (!mIsAddUser) {
            if (ObjectUtils.isEmpty(mGroup)) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.contextmenu_fragment_user, menu);
            } else {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.contextmenu_fragment_user_for_group, menu);
            }
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.kth.kanu.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onPrepareActionMode
     * (com.kth.kanu.baassample.utils.actionmodecompat.ActionMode,
     * android.view.Menu)
     */
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.kth.kanu.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onActionItemClicked
     * (com.kth.kanu.baassample.utils.actionmodecompat.ActionMode,
     * android.view.MenuItem)
     */
    @Override
    public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) {
        boolean handled = false;
        switch (item.getItemId()) {
            case R.id.menu_remove_user: {
                if (!ObjectUtils.isEmpty(mGroup)) {
                    BaasioUser entity = mEntityList.get(mLongClickedPosition);
                    final int position = mLongClickedPosition;
                    mGroup.removeInBackground(entity, new BaasioCallback<BaasioUser>() {

                        @Override
                        public void onException(BaasioException e) {
                            Toast.makeText(getActivity(), "removeInBackground =>" + e.toString(),
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onResponse(BaasioUser response) {
                            if (response != null) {
                                mEntityList.remove(position);
                                mListAdapter.notifyDataSetChanged();

                                if (mEntityList.isEmpty()) {
                                    mEmptyList.setVisibility(View.VISIBLE);
                                } else {
                                    mEmptyList.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
                }
                handled = true;
                break;
            }
            case R.id.menu_push_send: {
                BaasioUser entity = mEntityList.get(mLongClickedPosition);
                final String userUuid = entity.getUuid().toString();

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "entity_dialog",
                                EntityDialogFragment.SEND_PUSH_BY_TARGET);
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String body = data.getString("text1");

                                        BaasioMessage message = new BaasioMessage();
                                        message.setMessage(body, null, null);
                                        message.setTo(userUuid);

                                        BaasioPush.sendPushInBackground(message,
                                                new BaasioCallback<BaasioMessage>() {

                                                    @Override
                                                    public void onException(BaasioException e) {
                                                        Toast.makeText(
                                                                getActivity(),
                                                                "sendPushInBackground =>"
                                                                        + e.toString(),
                                                                Toast.LENGTH_LONG).show();
                                                    }

                                                    @Override
                                                    public void onResponse(BaasioMessage response) {
                                                        if (response != null) {
                                                            Toast.makeText(
                                                                    getActivity(),
                                                                    "sendPushInBackground: Success: "
                                                                            + response.toString(),
                                                                    Toast.LENGTH_LONG).show();
                                                        }

                                                    }
                                                });
                                        return false;
                                    }
                                });
                    }
                }, 100);

                handled = true;
                break;
            }
        }

        LOGV(TAG,
                "onActionItemClicked: position=" + mLongClickedPosition + " title="
                        + item.getTitle());
        mActionMode.finish();
        return handled;
    }

    /*
     * (non-Javadoc)
     * @see com.kth.kanu.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onDestroyActionMode
     * (com.kth.kanu.baassample.utils.actionmodecompat.ActionMode)
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        if (mLongClickedView != null) {
            EtcUtils.setActivatedCompat(mLongClickedView, false);
            mLongClickedPosition = null;
            mLongClickedView = null;
        }
    }

}
