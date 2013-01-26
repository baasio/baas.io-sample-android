
package com.kth.baasio.sample.ui.main;

import static com.kth.common.utils.LogUtils.LOGV;
import static com.kth.common.utils.LogUtils.makeLogTag;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.kth.baasio.callback.BaasioCallback;
import com.kth.baasio.callback.BaasioDownloadAsyncTask;
import com.kth.baasio.callback.BaasioDownloadCallback;
import com.kth.baasio.callback.BaasioQueryCallback;
import com.kth.baasio.callback.BaasioUploadAsyncTask;
import com.kth.baasio.callback.BaasioUploadCallback;
import com.kth.baasio.entity.BaasioBaseEntity;
import com.kth.baasio.entity.file.BaasioFile;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.query.BaasioQuery;
import com.kth.baasio.query.BaasioQuery.ORDER_BY;
import com.kth.baasio.sample.R;
import com.kth.baasio.sample.ui.dialog.DialogUtils;
import com.kth.baasio.sample.ui.dialog.ProgressDialogFragment;
import com.kth.baasio.sample.ui.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.kth.baasio.sample.ui.pulltorefresh.PullToRefreshListView;
import com.kth.baasio.sample.utils.EtcUtils;
import com.kth.baasio.sample.utils.FileUtils;
import com.kth.baasio.sample.utils.actionmodecompat.ActionMode;
import com.kth.baasio.sample.utils.actionmodecompat.ActionMode.Callback;
import com.kth.baasio.utils.ObjectUtils;
import com.kth.common.utils.LogUtils;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class FileFragment extends SherlockFragment implements OnRefreshListener, Callback {

    private static final String TAG = makeLogTag(FileFragment.class);

    private final static int REQUEST_FILE_FOR_CREATE = 1;

    private final static int REQUEST_FILE_FOR_UPDATE = 2;

    private ViewGroup mRootView;

    private PullToRefreshListView mPullToRefreshList;

    private ListView mList;

    private TextView mEmptyList;

    private FileListAdapter mListAdapter;

    private ArrayList<BaasioFile> mEntityList;

    private BaasioQuery mQuery;

    private BaasioUploadAsyncTask mUploadFileAsyncTask = null;

    private BaasioDownloadAsyncTask mDownloadFileAsyncTask = null;

    private ActionMode mActionMode;

    private View mLongClickedView;

    private Integer mLongClickedPosition;

    private Integer mPositionForUpdate;

    public FileFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEntityList = new ArrayList<BaasioFile>();

        getEntities(false);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_pulltorefresh_list, null);

        mEmptyList = (TextView)mRootView.findViewById(R.id.textEmptyList);
        mEmptyList.setText(getString(R.string.empty_file_list));

        mPullToRefreshList = (PullToRefreshListView)mRootView.findViewById(R.id.list);
        mPullToRefreshList.setOnRefreshListener(this);
        mList = mPullToRefreshList.getRefreshableView();
        mListAdapter = new FileListAdapter(getActivity());

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
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
            com.actionbarsherlock.view.MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_file, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_file_create: {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_FILE_FOR_CREATE);
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
                    if (mPullToRefreshList.isRefreshing())
                        mPullToRefreshList.onRefreshComplete();
                }
            });

            mEntityList.clear();

            mQuery = query;

            List<BaasioFile> files = BaasioBaseEntity.toType(entities, BaasioFile.class);
            mEntityList.addAll(files);

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
                    if (mPullToRefreshList.isRefreshing())
                        mPullToRefreshList.onRefreshComplete();
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
                    mPullToRefreshList.setIsLoading(false);
                }
            });

            mQuery = query;

            List<BaasioFile> files = BaasioBaseEntity.toType(entities, BaasioFile.class);
            mEntityList.addAll(files);

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
            mQuery.setType(BaasioFile.ENTITY_TYPE + "s");
            mQuery.setOrderBy(BaasioBaseEntity.PROPERTY_MODIFIED, ORDER_BY.DESCENDING);
        }

        if (!next)
            mQuery.queryInBackground(mQueryCallback);
        else
            mQuery.nextInBackground(mQueryNextCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final int position;
        if (mPositionForUpdate != null) {
            position = mPositionForUpdate;
        } else {
            position = -1;
        }

        mPositionForUpdate = null;

        if (resultCode == Activity.RESULT_OK) {
            Uri contentUri = data.getData();
            if (contentUri == null) {
                return;
            }

            final String srcFilePath;
            try {
                srcFilePath = FileUtils.getPath(getActivity(), contentUri);
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }

            if (requestCode == REQUEST_FILE_FOR_CREATE) {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        ProgressDialogFragment dialog = DialogUtils.showProgressDialog(
                                getActivity(), "upload_progress", "업로드 중입니다.",
                                ProgressDialog.STYLE_HORIZONTAL);

                        dialog.setDialogResultListener(new ProgressDialogFragment.DialogResultListener() {

                            @Override
                            public boolean onNegativeButtonSelected(String tag) {
                                if (mUploadFileAsyncTask != null) {
                                    mUploadFileAsyncTask.cancel(true);

                                    Toast.makeText(getActivity(), "Cancel success",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), "Cancel failed",
                                            Toast.LENGTH_LONG).show();
                                }
                                return false;
                            }

                        });

                        DialogUtils.setProgress(getActivity(), "upload_progress", 0);

                        BaasioFile uploadFile = new BaasioFile();

                        mUploadFileAsyncTask = uploadFile.fileUploadInBackground(srcFilePath, null,
                                new BaasioUploadCallback() {

                                    @Override
                                    public void onResponse(BaasioFile response) {
                                        DialogUtils.dissmissProgressDialog(getActivity(),
                                                "upload_progress");

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

                                    @Override
                                    public void onProgress(long total, long current) {
                                        float progress = (float)((double)current / (double)total);

                                        DialogUtils.setProgress(getActivity(), "upload_progress",
                                                (int)(progress * 100.f));
                                    }

                                    @Override
                                    public void onException(BaasioException e) {
                                        DialogUtils.dissmissProgressDialog(getActivity(),
                                                "upload_progress");

                                        LogUtils.LOGE(TAG,
                                                "fileUploadInBackground =>" + e.toString());

                                        Toast.makeText(getActivity(),
                                                "fileUploadInBackground =>" + e.toString(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                }, 300);

            } else if (requestCode == REQUEST_FILE_FOR_UPDATE) {
                if (position == -1) {
                    return;
                }

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        ProgressDialogFragment dialog = DialogUtils.showProgressDialog(
                                getActivity(), "upload_progress", "업로드 중입니다.",
                                ProgressDialog.STYLE_HORIZONTAL);

                        dialog.setDialogResultListener(new ProgressDialogFragment.DialogResultListener() {

                            @Override
                            public boolean onNegativeButtonSelected(String tag) {
                                if (mUploadFileAsyncTask != null) {
                                    mUploadFileAsyncTask.cancel(true);

                                    Toast.makeText(getActivity(), "Cancel success",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), "Cancel failed",
                                            Toast.LENGTH_LONG).show();
                                }
                                return true;
                            }

                        });

                        DialogUtils.setProgress(getActivity(), "upload_progress", 0);

                        BaasioFile updateFile = mEntityList.get(position);

                        if (updateFile != null) {
                            mUploadFileAsyncTask = updateFile.fileUpdateInBackground(srcFilePath,
                                    null, new BaasioUploadCallback() {

                                        @Override
                                        public void onResponse(BaasioFile response) {
                                            DialogUtils.dissmissProgressDialog(getActivity(),
                                                    "upload_progress");

                                            mEntityList.remove(position);

                                            mEntityList.add(0, response);

                                            mListAdapter.notifyDataSetChanged();

                                            if (mEntityList.isEmpty()) {
                                                mEmptyList.setVisibility(View.VISIBLE);
                                            } else {
                                                mEmptyList.setVisibility(View.GONE);
                                            }

                                        }

                                        @Override
                                        public void onProgress(long total, long current) {
                                            float progress = (float)((double)current / (double)total);

                                            DialogUtils.setProgress(getActivity(),
                                                    "upload_progress", (int)(progress * 100.f));
                                        }

                                        @Override
                                        public void onException(BaasioException e) {
                                            DialogUtils.dissmissProgressDialog(getActivity(),
                                                    "upload_progress");

                                            LogUtils.LOGE(TAG,
                                                    "fileUpdateInBackground =>" + e.toString());

                                            Toast.makeText(getActivity(),
                                                    "fileUpdateInBackground =>" + e.toString(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    }
                }, 300);
            }
        }
    }

    public class EntityViewHolder {
        public LinearLayout mRoot;

        public ImageView mFileType;

        public ImageView mFolderType;

        public TextView mName;

        public TextView mSize;

        public LinearLayout mLowerLayout;

        public TextView mCreatedTime;

        public TextView mModifiedTime;
    }

    private class FileListAdapter extends BaseAdapter {
        private final String TAG = makeLogTag(FileListAdapter.class);

        private Context mContext;

        private LayoutInflater mInflater;

        public FileListAdapter(Context context) {
            super();

            mContext = context;

            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mEntityList.size();
        }

        @Override
        public BaasioFile getItem(int position) {
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
                convertView = mInflater.inflate(R.layout.listview_item_filelist, parent, false);

                view = new EntityViewHolder();
                view.mRoot = (LinearLayout)convertView.findViewById(R.id.layoutRoot);
                view.mFileType = (ImageView)convertView.findViewById(R.id.imageFileType);
                view.mFolderType = (ImageView)convertView.findViewById(R.id.imageFolderType);

                view.mName = (TextView)convertView.findViewById(R.id.textName);
                view.mSize = (TextView)convertView.findViewById(R.id.textSize);

                view.mLowerLayout = (LinearLayout)convertView.findViewById(R.id.layoutLower);
                view.mCreatedTime = (TextView)convertView.findViewById(R.id.textCreatedTime);
                view.mModifiedTime = (TextView)convertView.findViewById(R.id.textModifiedTime);

                if (view != null) {
                    convertView.setTag(view);
                }
            } else {
                view = (EntityViewHolder)convertView.getTag();
            }

            final BaasioFile entity = mEntityList.get(position).toType(BaasioFile.class);

            if (entity != null) {
                view.mFileType.setVisibility(View.VISIBLE);
                view.mFolderType.setVisibility(View.GONE);

                view.mSize.setVisibility(View.VISIBLE);

                JsonNode sizeNode = entity.getProperties().get("content-length");
                view.mSize.setText(sizeNode.getLongValue() + " bytes");

                view.mLowerLayout.setVisibility(View.VISIBLE);

                // String filename = entity.getFilename();
                // String result = "";
                // try {
                // result = URLDecoder.decode(filename, "UTF-8");
                // } catch (UnsupportedEncodingException e) {
                // // TODO Auto-generated catch block
                // e.printStackTrace();
                // }

                view.mName.setText(entity.getFilename());

                if (entity.getCreated() != null) {
                    String createdTime = EtcUtils.getDateString(entity.getCreated());
                    if (!TextUtils.isEmpty(createdTime)) {
                        view.mCreatedTime
                                .setText(getString(R.string.created_time_file, createdTime));
                    }
                }

                if (entity.getModified() != null) {
                    String modifiedTime = EtcUtils.getDateString(entity.getModified());
                    if (!TextUtils.isEmpty(modifiedTime)) {
                        view.mModifiedTime.setText(getString(R.string.modified_time_file,
                                modifiedTime));
                    }
                }

                view.mRoot.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onItemClicked(entity);
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

                        mActionMode = ActionMode.start(getActivity(), FileFragment.this);
                        EtcUtils.setActivatedCompat(mLongClickedView, true);
                        return true;
                    }
                });
            }
            return convertView;
        }
    }

    private void onItemClicked(BaasioFile entity) {
        downloadFile(entity);
    }

    public void downloadFile(BaasioFile entity) {
        String localPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Download/";

        new Handler().post(new Runnable() {

            @Override
            public void run() {
                ProgressDialogFragment dialog = DialogUtils.showProgressDialog(getActivity(),
                        "download_progress", "다운로드 중입니다.", ProgressDialog.STYLE_HORIZONTAL);

                dialog.setDialogResultListener(new ProgressDialogFragment.DialogResultListener() {

                    @Override
                    public boolean onNegativeButtonSelected(String tag) {
                        if (mDownloadFileAsyncTask != null) {
                            mDownloadFileAsyncTask.cancel(true);

                            Toast.makeText(getActivity(), "Cancel success", Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            Toast.makeText(getActivity(), "Cancel failed", Toast.LENGTH_LONG)
                                    .show();
                        }
                        return false;
                    }

                });
                DialogUtils.setProgress(getActivity(), "download_progress", 0);
            }
        });

        mDownloadFileAsyncTask = entity.fileDownloadInBackground(localPath,
                new BaasioDownloadCallback() {

                    @Override
                    public void onResponse(String localFilePath) {
                        DialogUtils.dissmissProgressDialog(getActivity(), "download_progress");
                        if (!ObjectUtils.isEmpty(localFilePath)) {
                            Toast.makeText(getActivity(),
                                    "fileDownloadInBackground =>" + localFilePath,
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onProgress(long total, long current) {
                        float progress = (float)((double)current / (double)total);
                        LogUtils.LOGE(TAG, "fileDownloadInBackground onProgress:" + progress);

                        DialogUtils.setProgress(getActivity(), "download_progress",
                                (int)(progress * 100.f));
                    }

                    @Override
                    public void onException(BaasioException e) {
                        DialogUtils.dissmissProgressDialog(getActivity(), "download_progress");

                        Toast.makeText(getActivity(), "fileDownloadInBackground =>" + e.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onRefresh() {
        getEntities(false);
    }

    @Override
    public void onUpdate() {
        getEntities(true);
    }

    /*
     * (non-Javadoc)
     * @see
     * com.kth.baasio.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onCreateActionMode
     * (com.kth.baasio.baassample.utils.actionmodecompat.ActionMode,
     * android.view.Menu)
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (mLongClickedView == null) {
            return true;
        }

        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.contextmenu_fragment_file, menu);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.kth.baasio.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onPrepareActionMode
     * (com.kth.baasio.baassample.utils.actionmodecompat.ActionMode,
     * android.view.Menu)
     */
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.kth.baasio.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onActionItemClicked
     * (com.kth.baasio.baassample.utils.actionmodecompat.ActionMode,
     * android.view.MenuItem)
     */
    @Override
    public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) {
        boolean handled = false;
        switch (item.getItemId()) {
            case R.id.menu_file_update: {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_FILE_FOR_UPDATE);

                mPositionForUpdate = mLongClickedPosition;

                handled = true;
                break;
            }
            case R.id.menu_file_delete: {
                final int position = mLongClickedPosition;

                BaasioFile entity = mEntityList.get(mLongClickedPosition);

                entity.deleteInBackground(new BaasioCallback<BaasioFile>() {

                    @Override
                    public void onResponse(BaasioFile response) {
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

                    @Override
                    public void onException(BaasioException e) {
                        Toast.makeText(getActivity(), "deleteInBackground =>" + e.toString(),
                                Toast.LENGTH_LONG).show();
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
     * @see
     * com.kth.baasio.baassample.utils.actionmodecompat.ActionMode.Callback#
     * onDestroyActionMode
     * (com.kth.baasio.baassample.utils.actionmodecompat.ActionMode)
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
