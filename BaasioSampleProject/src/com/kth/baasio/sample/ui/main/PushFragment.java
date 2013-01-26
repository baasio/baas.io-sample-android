
package com.kth.baasio.sample.ui.main;

import static com.kth.common.utils.LogUtils.makeLogTag;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gcm.GCMRegistrar;
import com.kth.baasio.Baas;
import com.kth.baasio.callback.BaasioDeviceCallback;
import com.kth.baasio.callback.BaasioQueryCallback;
import com.kth.baasio.callback.BaasioResponseCallback;
import com.kth.baasio.entity.BaasioBaseEntity;
import com.kth.baasio.entity.push.BaasioDevice;
import com.kth.baasio.entity.push.BaasioPush;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.preferences.BaasioPreferences;
import com.kth.baasio.query.BaasioQuery;
import com.kth.baasio.response.BaasioResponse;
import com.kth.baasio.sample.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class PushFragment extends SherlockFragment {

    private static final String TAG = makeLogTag(PushFragment.class);

    private ViewGroup mRootView;

    private TextView mLoginStatus;

    private TextView mRegId;

    private TextView mDeviceUuid;

    private EditText mTag;

    private Button mButtonUpdate;

    private TextView mResult;

    private AsyncTask mGCMRegisterTask;

    public PushFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_push, null);

        mRegId = (TextView)mRootView.findViewById(R.id.regId);
        mDeviceUuid = (TextView)mRootView.findViewById(R.id.deviceUuid);
        mLoginStatus = (TextView)mRootView.findViewById(R.id.loginStatus);
        mTag = (EditText)mRootView.findViewById(R.id.tag);

        mButtonUpdate = (Button)mRootView.findViewById(R.id.buttonUpdate);
        mButtonUpdate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                registerDevice(true);
            }
        });
        mResult = (TextView)mRootView.findViewById(R.id.result);

        refreshView();

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshView();
    }

    private void refreshView() {
        String regId = GCMRegistrar.getRegistrationId(getActivity());
        if (regId != null) {
            mRegId.setText(regId);
        }

        if (TextUtils.isEmpty(Baas.io().getAccessToken())) {
            mLoginStatus.setText(getString(R.string.status_signout));
        } else {
            mLoginStatus.setText(getString(R.string.status_signin));
        }

        String deviceUuid = BaasioPreferences.getDeviceUuidForPush(getActivity());
        if (deviceUuid != null) {
            mDeviceUuid.setText(deviceUuid);
        }

        String tags = BaasioPreferences.getRegisteredTags(getActivity());
        if (tags != null) {
            mTag.setText(tags);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
            com.actionbarsherlock.view.MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_push, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        // TODO Auto-generated method stub
        super.onPrepareOptionsMenu(menu);
    }

    private void registerDevice(final boolean isUpdate) {
        String tagString = mTag.getText().toString().trim();
        mGCMRegisterTask = BaasioPush.registerWithTagsInBackground(getActivity(), tagString,
                new BaasioDeviceCallback() {

                    @Override
                    public void onException(BaasioException e) {
                        if (isUpdate) {
                            Toast.makeText(getActivity(), getString(R.string.msg_update_info_fail),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.msg_register_fail),
                                    Toast.LENGTH_LONG).show();
                        }

                        mResult.setText(e.toString());
                    }

                    @Override
                    public void onResponse(BaasioDevice response) {
                        if (response != null) {
                            if (isUpdate) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.msg_update_info_success),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(),
                                        getString(R.string.msg_register_success), Toast.LENGTH_LONG)
                                        .show();
                            }

                            mResult.setText(response.toString());

                            String deviceUuid = BaasioPreferences
                                    .getDeviceUuidForPush(getActivity());
                            if (deviceUuid != null) {
                                mDeviceUuid.setText(deviceUuid);
                            }
                        }

                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_signout: {
                refreshView();
                break;
            }

            case R.id.menu_push_register: {
                registerDevice(false);
                break;
            }

            case R.id.menu_push_unregister: {
                BaasioPush.unregisterInBackground(getActivity(), new BaasioResponseCallback() {

                    @Override
                    public void onException(BaasioException e) {
                        Toast.makeText(getActivity(), getString(R.string.msg_unregister_fail),
                                Toast.LENGTH_LONG).show();

                        mResult.setText(e.toString());
                    }

                    @Override
                    public void onResponse(BaasioResponse response) {
                        if (response != null) {
                            Toast.makeText(getActivity(),
                                    getString(R.string.msg_unregister_success), Toast.LENGTH_LONG)
                                    .show();

                            mResult.setText(response.toString());

                            String deviceUuid = BaasioPreferences
                                    .getDeviceUuidForPush(getActivity());
                            if (deviceUuid != null) {
                                mDeviceUuid.setText(deviceUuid);
                            }
                        }
                    }
                });
                break;
            }

            case R.id.menu_push_getinfo: {
                if (!GCMRegistrar.isRegisteredOnServer(getActivity())) {
                    Toast.makeText(getActivity(), "Already unregistered on the GCM server.",
                            Toast.LENGTH_LONG).show();
                } else {
                    String deviceUuid = BaasioPreferences.getDeviceUuidForPush(getActivity());
                    if (TextUtils.isEmpty(deviceUuid)) {
                        Toast.makeText(getActivity(), "Device Uuid is empty.", Toast.LENGTH_LONG)
                                .show();
                        break;
                    } else {
                        BaasioQuery query = new BaasioQuery();
                        query.setType("pushes/devices/" + deviceUuid);
                        query.queryInBackground(new BaasioQueryCallback() {

                            @Override
                            public void onResponse(List<BaasioBaseEntity> entities,
                                    List<Object> list, BaasioQuery query, long timestamp) {

                                if (entities != null) {

                                    if (entities.size() > 0) {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.msg_getinfo_success),
                                                Toast.LENGTH_LONG).show();

                                        mResult.setText(entities.get(0).toString());
                                    }
                                }
                            }

                            @Override
                            public void onException(BaasioException e) {
                                Toast.makeText(getActivity(), getString(R.string.msg_getinfo_fail),
                                        Toast.LENGTH_LONG).show();

                                mResult.setText(e.toString());
                            }
                        });
                    }
                }
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

}
