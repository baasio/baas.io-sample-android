
package com.kth.baasio.sample.ui.main;

import static com.kth.common.utils.LogUtils.LOGV;
import static com.kth.common.utils.LogUtils.makeLogTag;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.Baas;
import com.kth.baasio.callback.BaasioCallback;
import com.kth.baasio.callback.BaasioQueryCallback;
import com.kth.baasio.entity.BaasioBaseEntity;
import com.kth.baasio.entity.entity.BaasioEntity;
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

public class PostFragment extends SherlockFragment implements OnRefreshListener, Callback {

    private static final String TAG = makeLogTag(PostFragment.class);

    public static final String ENTITY_TYPE = "post";

    public static final String ENTITY_PROPERTY_NAME_WRITER_USERNAME = "writer_username";

    public static final String ENTITY_PROPERTY_NAME_WRITER_PICTURE = "writer_picture";

    public static final String ENTITY_PROPERTY_NAME_WRITER_UUID = "writer_uuid";

    public static final String ENTITY_PROPERTY_NAME_TITLE = "title";

    public static final String ENTITY_PROPERTY_NAME_BODY = "body";

    private ViewGroup mRootView;

    private ImageFetcher mImageFetcher;

    private PullToRefreshListView mPullToRefreshList;

    private ListView mList;

    private TextView mEmptyList;

    private EntityListAdapter mListAdapter;

    private ArrayList<BaasioEntity> mEntityList;

    private BaasioQuery mQuery;

    private ActionMode mActionMode;

    private View mLongClickedView;

    private Integer mLongClickedPosition;

    public PostFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageFetcher = EtcUtils.getImageFetcher(getActivity());

        mEntityList = new ArrayList<BaasioEntity>();
        mListAdapter = new EntityListAdapter(getActivity(), mImageFetcher);

        getEntities(false);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_pulltorefresh_list, null);

        mEmptyList = (TextView)mRootView.findViewById(R.id.textEmptyList);
        mEmptyList.setText(getString(R.string.empty_post_list));

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
        inflater.inflate(R.menu.fragment_post, menu);
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
            case R.id.menu_post_create: {
                if (ObjectUtils.isEmpty(Baas.io().getSignedInUser())) {
                    Toast.makeText(getActivity(), getString(R.string.msg_need_login),
                            Toast.LENGTH_LONG).show();
                    return true;
                }
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "create_entity", EntityDialogFragment.CREATE_ENTITY);
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String title = data.getString("text1");
                                        String body = data.getString("text2");
                                        return processEntity(mode, title, body, -1);
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

            List<BaasioEntity> posts = BaasioBaseEntity.toType(entities, BaasioEntity.class);
            mEntityList.addAll(posts);

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

            List<BaasioEntity> posts = BaasioBaseEntity.toType(entities, BaasioEntity.class);
            mEntityList.addAll(posts);

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

    public boolean processEntity(int mode, String title, String body, final int position) {
        if (TextUtils.isEmpty(title)) {
            return false;
        }

        if (mode == EntityDialogFragment.CREATE_ENTITY) {
            BaasioEntity entity = new BaasioEntity(ENTITY_TYPE);
            entity.setProperty(ENTITY_PROPERTY_NAME_WRITER_USERNAME, Baas.io().getSignedInUser()
                    .getUsername());
            entity.setProperty(ENTITY_PROPERTY_NAME_WRITER_PICTURE, Baas.io().getSignedInUser()
                    .getPicture());
            entity.setProperty(ENTITY_PROPERTY_NAME_WRITER_UUID, Baas.io().getSignedInUser()
                    .getUuid().toString());
            entity.setProperty(ENTITY_PROPERTY_NAME_TITLE, title);
            if (!ObjectUtils.isEmpty(body)) {
                entity.setProperty(ENTITY_PROPERTY_NAME_BODY, body);
            }

            entity.saveInBackground(new BaasioCallback<BaasioEntity>() {

                @Override
                public void onException(BaasioException e) {
                    Toast.makeText(getActivity(), "saveInBackground =>" + e.toString(),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onResponse(BaasioEntity response) {
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
        } else if (mode == EntityDialogFragment.MODIFY_ENTITY) {
            BaasioEntity entity = new BaasioEntity(mEntityList.get(position));
            entity.setProperty(ENTITY_PROPERTY_NAME_TITLE, title);
            entity.setProperty(ENTITY_PROPERTY_NAME_BODY, body);

            entity.updateInBackground(new BaasioCallback<BaasioEntity>() {

                @Override
                public void onException(BaasioException e) {
                    Toast.makeText(getActivity(), "updateInBackground =>" + e.toString(),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onResponse(BaasioEntity response) {
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
        }

        return false;
    }

    public class EntityViewHolder {
        public ViewGroup mRoot;

        public ImageView mProfile;

        public TextView mTitle;

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
        public BaasioEntity getItem(int position) {
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
                convertView = mInflater.inflate(R.layout.listview_item_postlist, parent, false);

                view = new EntityViewHolder();

                view.mRoot = (ViewGroup)convertView.findViewById(R.id.layoutRoot);
                view.mProfile = (ImageView)convertView.findViewById(R.id.imageProfile);
                view.mTitle = (TextView)convertView.findViewById(R.id.textTitle);
                view.mBody = (TextView)convertView.findViewById(R.id.textBody);
                view.mCreatedTime = (TextView)convertView.findViewById(R.id.textCreatedTime);
                view.mModifiedTime = (TextView)convertView.findViewById(R.id.textModifiedTime);

                if (view != null) {
                    convertView.setTag(view);
                }
            } else {
                view = (EntityViewHolder)convertView.getTag();
            }

            BaasioEntity entity = mEntityList.get(position);

            if (entity != null) {
                setStringToView(entity, view.mTitle, ENTITY_PROPERTY_NAME_TITLE);
                setStringToView(entity, view.mBody, ENTITY_PROPERTY_NAME_BODY);

                String imageUrl = EtcUtils.getStringFromEntity(entity,
                        ENTITY_PROPERTY_NAME_WRITER_PICTURE);
                if (imageUrl != null) {
                    mImageFetcher.loadImage(imageUrl, view.mProfile, R.drawable.person_image_empty);
                } else {
                    view.mProfile.setImageResource(R.drawable.person_image_empty);
                }

                if (entity.getCreated() != null) {
                    String createdTime = EtcUtils.getDateString(entity.getCreated());
                    if (!TextUtils.isEmpty(createdTime)) {
                        view.mCreatedTime
                                .setText(getString(R.string.created_time_post, createdTime));
                    }
                }

                if (entity.getModified() != null) {
                    String modifiedTime = EtcUtils.getDateString(entity.getModified());
                    if (!TextUtils.isEmpty(modifiedTime)) {
                        view.mModifiedTime.setText(getString(R.string.modified_time_post,
                                modifiedTime));
                    }
                }

                if (view.mRoot != null) {
                    view.mRoot.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            BaasioEntity entity = mEntityList.get(position);

                            Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                            intent.putExtra(Intent.EXTRA_TITLE,
                                    getString(R.string.title_activity_postdetail));
                            intent.putExtra("post", entity.toString());

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

                            mActionMode = ActionMode.start(getActivity(), PostFragment.this);
                            EtcUtils.setActivatedCompat(mLongClickedView, true);
                            return true;
                        }
                    });
                }
            }
            return convertView;
        }

        private void setStringToView(BaasioEntity entity, TextView view, String value) {
            view.setText(EtcUtils.getStringFromEntity(entity, value));
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
        inflater.inflate(R.menu.contextmenu_fragment_post, menu);

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
            case R.id.menu_post_modify: {
                if (ObjectUtils.isEmpty(Baas.io().getSignedInUser())) {
                    Toast.makeText(getActivity(), getString(R.string.msg_need_login),
                            Toast.LENGTH_LONG).show();
                    handled = true;
                    break;
                }

                final int position = mLongClickedPosition;

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        BaasioEntity entity = mEntityList.get(position);
                        String title = EtcUtils.getStringFromEntity(entity,
                                ENTITY_PROPERTY_NAME_TITLE);
                        String body = EtcUtils.getStringFromEntity(entity,
                                ENTITY_PROPERTY_NAME_BODY);

                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "modify_entity", EntityDialogFragment.MODIFY_ENTITY);
                        entityDialog.setText1(title);
                        entityDialog.setText2(body);
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String title = data.getString("text1");
                                        String body = data.getString("text2");
                                        return processEntity(mode, title, body, position);
                                    }
                                });
                    }
                }, 100);

                handled = true;
                break;
            }
            case R.id.menu_post_delete: {
                if (ObjectUtils.isEmpty(Baas.io().getSignedInUser())) {
                    Toast.makeText(getActivity(), getString(R.string.msg_need_login),
                            Toast.LENGTH_LONG).show();
                    handled = true;
                    break;
                }

                final int position = mLongClickedPosition;

                BaasioEntity entity = mEntityList.get(mLongClickedPosition);
                entity.deleteInBackground(new BaasioCallback<BaasioEntity>() {

                    @Override
                    public void onException(BaasioException e) {
                        Toast.makeText(getActivity(), "deleteInBackground =>" + e.toString(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onResponse(BaasioEntity response) {
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
