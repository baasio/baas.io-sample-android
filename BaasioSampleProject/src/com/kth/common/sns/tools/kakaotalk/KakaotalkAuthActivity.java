/**
 * Copyright 2014 Kakao Corp.
 *
 * Redistribution and modification in source or binary forms are not permitted without specific prior written permission. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kth.common.sns.tools.kakaotalk;

import com.kakao.Session;
import com.kakao.SessionCallback;
import com.kakao.exception.KakaoException;
import com.kakao.widget.LoginButton;
import com.kth.baasio.callback.BaasioSignInCallback;
import com.kth.baasio.entity.user.BaasioUser;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.sample.ui.dialog.DialogUtils;
import com.kth.baasio.utils.ObjectUtils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * 샘플에서 사용하게 될 로그인 페이지 세션을 오픈한 후 action을 override해서 사용한다.
 * 
 * @author MJ
 */
public class KakaotalkAuthActivity extends FragmentActivity {
    public static final String _TAG = "KakaotalkAuthActivity";

    private Context mContext;

    private LoginButton loginButton;

    private final SessionCallback mySessionCallback = new MySessionStatusCallback();

    /**
     * super.onCreate를 호출하여 Session처리를 맡긴다. 로그인 버튼을 클릭 했을시 access token을 요청하도록
     * 설정한다.
     * 
     * @param savedInstanceState 기존 session 정보가 저장된 객체
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
    }

    protected void onResume() {
        super.onResume();
        // 세션을 초기화 한다
        if (Session.initializeSession(this, mySessionCallback)) {
            // 1. 세션을 갱신 중이면, 프로그레스바를 보이거나 버튼을 숨기는 등의 액션을 취한다
            DialogUtils.showProgressDialog(KakaotalkAuthActivity.this, "kakaotalk_login",
                    "로그인 중입니다.");
        } else if (Session.getCurrentSession().isOpened()) {
            DialogUtils.showProgressDialog(KakaotalkAuthActivity.this, "kakaotalk_login",
                    "로그인 중입니다.");

            onSessionOpened();
            return;
        }

        if (!Session.getCurrentSession().isOpened()) {
            DialogUtils.showProgressDialog(KakaotalkAuthActivity.this, "kakaotalk_login",
                    "로그인 중입니다.");

            Session.getCurrentSession().open(mySessionCallback);
        }
    }

    private class MySessionStatusCallback implements SessionCallback {
        /**
         * 세션이 오픈되었으면 가입페이지로 이동 한다.
         */
        @Override
        public void onSessionOpened() {
            // 프로그레스바를 보이고 있었다면 중지하고 세션 오픈후 보일 페이지로 이동
            KakaotalkAuthActivity.this.onSessionOpened();
        }

        /**
         * 세션이 삭제되었으니 로그인 화면이 보여야 한다.
         * 
         * @param exception 에러가 발생하여 close가 된 경우 해당 exception
         */
        @Override
        public void onSessionClosed(final KakaoException exception) {
            // 프로그레스바를 보이고 있었다면 중지하고 세션 오픈을 못했으니 다시 로그인 버튼 노출.
            DialogUtils.dissmissProgressDialog(KakaotalkAuthActivity.this, "kakaotalk_login");
        }

    }

    protected void onSessionOpened() {
        String accessToken = "";
        try {
            accessToken = Session.getCurrentSession().getAccessToken();
        } catch (IllegalStateException e) {
            return;
        }

        if (!ObjectUtils.isEmpty(accessToken)) {
            BaasioUser.signInViaKakaotalkInBackground(mContext, accessToken,
                    new BaasioSignInCallback() {

                        @Override
                        public void onException(BaasioException e) {
                            DialogUtils.dissmissProgressDialog(KakaotalkAuthActivity.this,
                                    "kakaotalk_login");

                            setResult(RESULT_CANCELED);
                            finish();
                        }

                        @Override
                        public void onResponse(BaasioUser response) {
                            DialogUtils.dissmissProgressDialog(KakaotalkAuthActivity.this,
                                    "kakaotalk_login");

                            setResult(RESULT_OK);
                            finish();
                        }
                    });
        }
    }
}
