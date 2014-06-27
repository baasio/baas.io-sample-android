
package com.kth.common.sns.tools.facebook;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.facebook.Session;
import com.facebook.SessionState;
import com.kth.baasio.callback.BaasioSignInCallback;
import com.kth.baasio.entity.user.BaasioUser;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.sample.ui.dialog.DialogUtils;
import com.kth.baasio.utils.ObjectUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class FacebookAuthActivity extends SherlockFragmentActivity {
    public static final String _TAG = "FacebookAuthActivity";

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        // start Facebook Login
        Session.openActiveSession(this, true, new Session.StatusCallback() {

            // callback when session changes state
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (session.isOpened()) {
                    String token = session.getAccessToken();

                    if (!ObjectUtils.isEmpty(token)) {
                        DialogUtils.showProgressDialog(FacebookAuthActivity.this, "facebook_login",
                                "로그인 중입니다.");

                        BaasioUser.signInViaFacebookInBackground(mContext, token,
                                new BaasioSignInCallback() {

                                    @Override
                                    public void onException(BaasioException e) {
                                        DialogUtils.dissmissProgressDialog(
                                                FacebookAuthActivity.this, "facebook_login");

                                        setResult(RESULT_CANCELED);
                                        finish();
                                    }

                                    @Override
                                    public void onResponse(BaasioUser response) {
                                        DialogUtils.dissmissProgressDialog(
                                                FacebookAuthActivity.this, "facebook_login");

                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                });
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}
