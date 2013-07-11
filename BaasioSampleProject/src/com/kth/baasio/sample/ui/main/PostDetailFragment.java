
package com.kth.baasio.sample.ui.main;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.callback.BaasioCallback;
import com.kth.baasio.callback.BaasioQueryCallback;
import com.kth.baasio.entity.BaasioBaseEntity;
import com.kth.baasio.entity.entity.BaasioEntity;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.query.BaasioQuery;
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
import com.kth.common.utils.LogUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
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

public class PostDetailFragment extends SherlockFragment implements OnRefreshListener, Callback {

    private static final String TAG = LogUtils.makeLogTag(PostDetailFragment.class);

    public static final String ENTITY_TYPE = "comment";

    public static final String ENTITY_PROPERTY_NAME_BODY = "body";

    private ViewGroup mRootView;

    private HeaderLayout mLayoutDetail;

    public ImageView mImageProfile;

    public TextView mTextTitle;

    public TextView mTextName;

    public TextView mTextCreated;

    public TextView mTextModified;

    public TextView mTextBody;

    public TextView mTextMore;

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

    private BaasioEntity mPost;

    private boolean isOpened;

    public PostDetailFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageFetcher = EtcUtils.getImageFetcher(getActivity());

        mEntityList = new ArrayList<BaasioEntity>();
        mListAdapter = new EntityListAdapter(getActivity());

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            String postString = intent.getStringExtra("post");

            if (!ObjectUtils.isEmpty(postString)) {
                mPost = JsonUtils.parse(postString, BaasioEntity.class);
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
        mEmptyList.setText(getString(R.string.empty_comment_list));

        mLayoutDetail = (HeaderLayout)inflater.inflate(R.layout.listview_header_postdetail, null);
        mLayoutDetail.setOnSizeChangedListener(new HeaderLayout.SizeChangedListener() {

            @Override
            public void onSizeChanged(int w, int h, int oldw, int oldh) {
                int height = mRootView.getHeight();

                ViewGroup.LayoutParams params = mEmptyList.getLayoutParams();
                params.height = height - h;
                mEmptyList.setLayoutParams(params);
            }
        });

        mImageProfile = (ImageView)mLayoutDetail.findViewById(R.id.imageProfile);
        mTextName = (TextView)mLayoutDetail.findViewById(R.id.textName);
        mTextTitle = (TextView)mLayoutDetail.findViewById(R.id.textTitle);
        mTextCreated = (TextView)mLayoutDetail.findViewById(R.id.textCreated);
        mTextModified = (TextView)mLayoutDetail.findViewById(R.id.textModified);

        mTextBody = (TextView)mLayoutDetail.findViewById(R.id.textBody);
        mTextMore = (TextView)mLayoutDetail.findViewById(R.id.textMore);

        mTextMore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isOpened) {
                    mTextBody.setMaxLines(Integer.MAX_VALUE);
                    mTextBody.setEllipsize(null);

                    mTextMore.setText(R.string.label_fold);

                    isOpened = true;
                } else {
                    mTextBody.setMaxLines(2);
                    mTextBody.setEllipsize(TruncateAt.END);

                    mTextMore.setText(R.string.label_expand);

                    isOpened = false;
                }
            }
        });

        mPullToRefreshList = (PullToRefreshListView)mRootView.findViewById(R.id.list);
        mPullToRefreshList.setOnRefreshListener(this);

        mList = mPullToRefreshList.getRefreshableView();
        mList.addHeaderView(mLayoutDetail);
        mList.setDivider(null);

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

        refreshView();

        return mRootView;
    }

    private void refreshView() {
        if (mPost != null) {
            String imageUrl = EtcUtils.getStringFromEntity(mPost,
                    PostFragment.ENTITY_PROPERTY_NAME_WRITER_PICTURE);
            if (imageUrl != null) {
                mImageFetcher.loadImage(imageUrl, mImageProfile, R.drawable.person_image_empty);
            } else {
                mImageProfile.setImageResource(R.drawable.person_image_empty);
            }

            setStringToView(mPost, mTextTitle, PostFragment.ENTITY_PROPERTY_NAME_TITLE);
            setStringToView(mPost, mTextName, PostFragment.ENTITY_PROPERTY_NAME_WRITER_USERNAME);
            setStringToView(mPost, mTextBody, PostFragment.ENTITY_PROPERTY_NAME_BODY);

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    Layout l = mTextBody.getLayout();
                    if (l != null) {
                        int lines = l.getLineCount();
                        int maxLines = getResources().getInteger(R.integer.line_number_2);

                        if (lines > maxLines) {
                            mTextMore.setVisibility(View.VISIBLE);
                        } else if (lines > 0) {
                            if (l.getEllipsisCount(lines - 1) > 0) {
                                mTextMore.setVisibility(View.VISIBLE);
                            } else {
                                mTextMore.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            mTextMore.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }, 100);

            if (mPost.getCreated() != null) {
                String createdTime = EtcUtils.getDateString(mPost.getCreated());
                if (!TextUtils.isEmpty(createdTime)) {
                    mTextCreated.setText(getString(R.string.created_time_post, createdTime));
                }
            }

            if (mPost.getModified() != null) {
                String modifiedTime = EtcUtils.getDateString(mPost.getModified());
                if (!TextUtils.isEmpty(modifiedTime)) {
                    mTextModified.setText(getString(R.string.modified_time_post, modifiedTime));
                }
            }
        }
    }

    private void setStringToView(BaasioEntity entity, TextView view, String value) {
        view.setText(EtcUtils.getStringFromEntity(entity, value));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
            com.actionbarsherlock.view.MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_postdetail, menu);
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
            case R.id.menu_comment_create: {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "create_comment",
                                EntityDialogFragment.CREATE_COMMENT);
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String body = data.getString("text1");
                                        return processEntity(mode, body, -1);
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

            List<BaasioEntity> comments = BaasioBaseEntity.toType(entities, BaasioEntity.class);
            mEntityList.addAll(comments);

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

            List<BaasioEntity> comments = BaasioBaseEntity.toType(entities, BaasioEntity.class);
            mEntityList.addAll(comments);

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
            mQuery.setRelation(mPost, "write_comment");
        }

        if (!next)
            mQuery.queryInBackground(mQueryCallback);
        else
            mQuery.nextInBackground(mQueryNextCallback);
    }

    public boolean processEntity(int mode, String body, final int position) {
        if (TextUtils.isEmpty(body)) {
            return false;
        }

        if (mode == EntityDialogFragment.CREATE_COMMENT) {
            BaasioEntity entity = new BaasioEntity(ENTITY_TYPE);
            entity.setProperty(ENTITY_PROPERTY_NAME_BODY, body);

            entity.saveInBackground(new BaasioCallback<BaasioEntity>() {

                @Override
                public void onException(BaasioException e) {
                    Toast.makeText(getActivity(), "saveInBackground =>" + e.toString(),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onResponse(BaasioEntity response) {
                    // TODO: 본글이랑 연결해야함.
                    mPost.connectInBackground("write_comment", response, BaasioEntity.class,
                            new BaasioCallback<BaasioEntity>() {

                                @Override
                                public void onException(BaasioException e) {
                                    Toast.makeText(getActivity(),
                                            "connectInBackground =>" + e.toString(),
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onResponse(BaasioEntity response) {
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
            });
        } else if (mode == EntityDialogFragment.MODIFY_COMMENT) {
            BaasioEntity entity = mEntityList.get(position);
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
                convertView = mInflater.inflate(R.layout.listview_item_commentlist, parent, false);

                view = new EntityViewHolder();

                view.mRoot = (ViewGroup)convertView.findViewById(R.id.layoutRoot);
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
                setStringToView(entity, view.mBody, ENTITY_PROPERTY_NAME_BODY);

                if (entity.getCreated() != null) {
                    String createdTime = EtcUtils.getDateString(entity.getCreated());
                    if (!TextUtils.isEmpty(createdTime)) {
                        view.mCreatedTime.setText(getString(R.string.created_time_comment,
                                createdTime));
                    }
                }

                if (entity.getModified() != null) {
                    String modifiedTime = EtcUtils.getDateString(entity.getModified());
                    if (!TextUtils.isEmpty(modifiedTime)) {
                        view.mModifiedTime.setText(getString(R.string.modified_time_comment,
                                modifiedTime));
                    }
                }

                if (view.mRoot != null) {
                    view.mRoot.setOnLongClickListener(new View.OnLongClickListener() {

                        @Override
                        public boolean onLongClick(View view) {
                            if (mActionMode != null) {
                                // CAB already displayed, ignore
                                return true;
                            }

                            mLongClickedView = view;
                            mLongClickedPosition = position;

                            mActionMode = ActionMode.start(getActivity(), PostDetailFragment.this);
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
        inflater.inflate(R.menu.contextmenu_fragment_postdetail, menu);

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
            case R.id.menu_comment_modify: {
                final int position = mLongClickedPosition;

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        BaasioEntity entity = mEntityList.get(position);
                        String body = EtcUtils.getStringFromEntity(entity,
                                ENTITY_PROPERTY_NAME_BODY);

                        EntityDialogFragment entityDialog = DialogUtils.showEntityDialog(
                                getActivity(), "modify_comment",
                                EntityDialogFragment.MODIFY_COMMENT);
                        entityDialog.setText1(body);
                        entityDialog
                                .setEntityDialogResultListener(new EntityDialogResultListener() {

                                    @Override
                                    public boolean onPositiveButtonSelected(int mode, Bundle data) {
                                        String body = data.getString("text1");
                                        return processEntity(mode, body, position);
                                    }
                                });
                    }
                }, 100);

                handled = true;
                break;
            }
            case R.id.menu_comment_delete: {
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

        LogUtils.LOGV(TAG, "onActionItemClicked: position=" + mLongClickedPosition + " title="
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
