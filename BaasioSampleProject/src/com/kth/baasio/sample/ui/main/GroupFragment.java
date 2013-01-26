
package com.kth.baasio.sample.ui.main;

import static com.kth.common.utils.LogUtils.LOGV;
import static com.kth.common.utils.LogUtils.makeLogTag;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.callback.BaasioCallback;
import com.kth.baasio.callback.BaasioQueryCallback;
import com.kth.baasio.entity.BaasioBaseEntity;
import com.kth.baasio.entity.group.BaasioGroup;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.query.BaasioQuery;
import com.kth.baasio.query.BaasioQuery.ORDER_BY;
import com.kth.baasio.sample.R;
import com.kth.baasio.sample.ui.dialog.DialogUtils;
import com.kth.baasio.sample.ui.dialog.EntityDialogFragment;
import com.kth.baasio.sample.ui.dialog.EntityDialogFragment.EntityDialogResultListener;
import com.kth.baasio.sample.ui.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.kth.baasio.sample.ui.pulltorefresh.PullToRefreshListView;
import com.kth.baasio.sample.utils.EtcUtils;
import com.kth.baasio.sample.utils.actionmodecompat.ActionMode;
import com.kth.baasio.sample.utils.actionmodecompat.ActionMode.Callback;
import com.kth.baasio.utils.ObjectUtils;

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

public class GroupFragment extends SherlockFragment implements OnRefreshListener, Callback {

    private static final String TAG = makeLogTag(GroupFragment.class);

    public static final String ENTITY_TYPE = "group";

    private ViewGroup mRootView;

    private PullToRefreshListView mPullToRefreshList;

    private ListView mList;

    private TextView mEmptyList;

    private EntityListAdapter mListAdapter;

    private ArrayList<BaasioGroup> mEntityList;

    private BaasioQuery mQuery;

    private ActionMode mActionMode;

    private View mLongClickedView;

    private Integer mLongClickedPosition;

    public GroupFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEntityList = new ArrayList<BaasioGroup>();
        mListAdapter = new EntityListAdapter(getActivity());

        getEntities(false);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_pulltorefresh_list, null);

        mEmptyList = (TextView)mRootView.findViewById(R.id.textEmptyList);
        mEmptyList.setText(getString(R.string.empty_group_list));

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
        inflater.inflate(R.menu.fragment_group, menu);
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
            case R.id.menu_group_create: {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "create_group", EntityDialogFragment.CREATE_GROUP);
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String body = data.getString("text1");

                                        BaasioGroup group = new BaasioGroup();
                                        group.setPath(body);
                                        group.saveInBackground(new BaasioCallback<BaasioGroup>() {

                                            @Override
                                            public void onException(BaasioException e) {
                                                Toast.makeText(getActivity(),
                                                        "saveInBackground =>" + e.toString(),
                                                        Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onResponse(BaasioGroup response) {
                                                if (response != null) {
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

            List<BaasioGroup> groups = BaasioBaseEntity.toType(entities, BaasioGroup.class);
            mEntityList.addAll(groups);

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

            List<BaasioGroup> groups = BaasioBaseEntity.toType(entities, BaasioGroup.class);
            mEntityList.addAll(groups);

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
            mQuery.setType(ENTITY_TYPE);
            mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
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

        public EntityListAdapter(Context context) {
            super();

            mContext = context;
            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mEntityList.size();
        }

        @Override
        public BaasioGroup getItem(int position) {
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
                convertView = mInflater.inflate(R.layout.listview_item_grouplist, parent, false);

                view = new EntityViewHolder();

                view.mRoot = (ViewGroup)convertView.findViewById(R.id.layoutRoot);
                view.mName = (TextView)convertView.findViewById(R.id.textName);
                view.mCreatedTime = (TextView)convertView.findViewById(R.id.textCreatedTime);
                view.mModifiedTime = (TextView)convertView.findViewById(R.id.textModifiedTime);

                if (view != null) {
                    convertView.setTag(view);
                }
            } else {
                view = (EntityViewHolder)convertView.getTag();
            }

            BaasioGroup entity = mEntityList.get(position);

            if (entity != null) {
                String display = "";
                if (!ObjectUtils.isEmpty(entity.getPath())) {
                    display = entity.getPath();
                }

                if (!ObjectUtils.isEmpty(entity.getTitle())) {
                    display = display + "(" + entity.getTitle() + ")";
                }

                if (!ObjectUtils.isEmpty(display)) {
                    view.mName.setText(display);
                }

                if (entity.getCreated() != null) {
                    String createdTime = EtcUtils.getDateString(entity.getCreated());
                    if (!TextUtils.isEmpty(createdTime)) {
                        view.mCreatedTime.setText(getString(R.string.created_time_group,
                                createdTime));
                    }
                }

                if (entity.getModified() != null) {
                    String modifiedTime = EtcUtils.getDateString(entity.getModified());
                    if (!TextUtils.isEmpty(modifiedTime)) {
                        view.mModifiedTime.setText(getString(R.string.modified_time_group,
                                modifiedTime));
                    }
                }

                if (view.mRoot != null) {
                    view.mRoot.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            BaasioGroup group = mEntityList.get(position);

                            Intent intent = new Intent(getActivity(), UserForGroupActivity.class);
                            intent.putExtra(
                                    Intent.EXTRA_TITLE,
                                    getString(R.string.title_activity_user_for_group,
                                            group.getPath()));
                            intent.putExtra("group", group.toString());

                            startActivity(intent);
                        }
                    });

                    view.mRoot.setOnLongClickListener(new View.OnLongClickListener() {

                        @Override
                        public boolean onLongClick(View view) {
                            if (mActionMode != null) {
                                // CAB already displayed, ignore
                                return true;
                            }

                            mLongClickedView = view;
                            mLongClickedPosition = position;

                            mActionMode = ActionMode.start(getActivity(), GroupFragment.this);
                            EtcUtils.setActivatedCompat(mLongClickedView, true);
                            return true;
                        }
                    });
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

        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.contextmenu_fragment_group, menu);

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
            case R.id.menu_group_modify: {
                final int position = mLongClickedPosition;
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        BaasioGroup group = mEntityList.get(position);

                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "modify_group", EntityDialogFragment.MODIFY_GROUP);
                        entityDialog.setText1(group.getPath());
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String body = data.getString("text1");

                                        BaasioGroup group = mEntityList.get(position);
                                        group.setPath(body);
                                        group.updateInBackground(new BaasioCallback<BaasioGroup>() {

                                            @Override
                                            public void onException(BaasioException e) {
                                                Toast.makeText(getActivity(),
                                                        "updateInBackground =>" + e.toString(),
                                                        Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onResponse(BaasioGroup response) {
                                                if (response != null) {
                                                    mEntityList.remove(position);
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
                                        return false;
                                    }
                                });
                    }
                }, 100);

                handled = true;
                break;
            }
            case R.id.menu_group_delete: {
                final int position = mLongClickedPosition;
                BaasioGroup group = mEntityList.get(position);
                group.deleteInBackground(new BaasioCallback<BaasioGroup>() {

                    @Override
                    public void onException(BaasioException e) {
                        Toast.makeText(getActivity(), "deleteInBackground =>" + e.toString(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onResponse(BaasioGroup response) {
                        if (!ObjectUtils.isEmpty(response)) {
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
