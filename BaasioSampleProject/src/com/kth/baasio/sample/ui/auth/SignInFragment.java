
package com.kth.baasio.sample.ui.auth;

import com.actionbarsherlock.app.SherlockFragment;
import com.kth.baasio.callback.BaasioSignInCallback;
import com.kth.baasio.entity.user.BaasioUser;
import com.kth.baasio.exception.BaasioException;
import com.kth.baasio.sample.R;
import com.kth.baasio.utils.ObjectUtils;
import com.kth.common.sns.tools.facebook.FacebookAuthActivity;
import com.kth.common.sns.tools.kakaotalk.KakaotalkAuthActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;

public class SignInFragment extends SherlockFragment {
    public static final int REQUEST_SIGNUP = 1;

    public static final int REQUEST_SIGNIN_VIA_FACEBOOK = 2;

    public static final int REQUEST_SIGNIN_VIA_KAKAOTALK = 3;

    // private Fragment mFragment;

    private Context mContext;

    private ViewGroup mRootView;

    private EditText mEmail;

    private EditText mPassword;

    private Button mConfirm;

    private TextView mViaFacebook;

    private TextView mViaKakaotalk;

    private TextView mNeedSignUp;

    private TextView mForgotPassword;

    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public static final String INTENT_EMAIL_INFO = "email";

    public static final String INTENT_KAKAOTALK_INFO = "kakaotalk";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        // mFragment = this;

        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_signin, null);
        mEmail = (EditText)mRootView.findViewById(R.id.textEmail);

        mPassword = (EditText)mRootView.findViewById(R.id.textPassword);

        mConfirm = (Button)mRootView.findViewById(R.id.buttonConfirm);
        mConfirm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                Pattern pattern = Pattern.compile(EMAIL_PATTERN);
                if (!pattern.matcher(email).matches()) {
                    Toast.makeText(getActivity(),
                            getActivity().getResources().getString(R.string.error_invalid_email),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (password == null || password.length() < 4) {
                    Toast.makeText(getActivity(), getString(R.string.error_invalid_password),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                BaasioUser.signInInBackground(mContext, email, password,
                        new BaasioSignInCallback() {

                            @Override
                            public void onException(BaasioException e) {
                                if (e.getStatusCode() != null) {
                                    if (e.getErrorCode() == 201) {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.error_invalid_grant),
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                }

                                Toast.makeText(getActivity(),
                                        "signInInBackground =>" + e.toString(), Toast.LENGTH_LONG)
                                        .show();
                            }

                            @Override
                            public void onResponse(BaasioUser response) {
                                if (!ObjectUtils.isEmpty(response)) {
                                    getActivity().setResult(Activity.RESULT_OK);
                                    getActivity().finish();
                                }
                            }
                        });
            }
        });

        mViaFacebook = (TextView)mRootView.findViewById(R.id.textViaFacebook);
        mViaFacebook.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, FacebookAuthActivity.class);
                startActivityForResult(intent, REQUEST_SIGNIN_VIA_FACEBOOK);
            }
        });

        mViaKakaotalk = (TextView)mRootView.findViewById(R.id.textViaKakaotalk);
        mViaKakaotalk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Intent intent = new Intent(mContext,
                // KakaotalkAuthActivity.class);
                // startActivityForResult(intent, REQUEST_SIGNIN_VIA_KAKAOTALK);

                Intent intent = new Intent(mContext, KakaotalkAuthActivity.class);
                startActivityForResult(intent, REQUEST_SIGNIN_VIA_KAKAOTALK);
            }
        });

        mNeedSignUp = (TextView)mRootView.findViewById(R.id.textNeedSignUp);
        mNeedSignUp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SignUpActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });

        mForgotPassword = (TextView)mRootView.findViewById(R.id.textForgotPassword);
        mForgotPassword.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();

                Pattern pattern = Pattern.compile(EMAIL_PATTERN);
                if (!pattern.matcher(email).matches()) {
                    Toast.makeText(getActivity(),
                            getActivity().getResources().getString(R.string.error_invalid_email),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (!ObjectUtils.isEmpty(email)) {
                    Uri uri = BaasioUser.getResetPasswordUrl(email);

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
            }
        });

        Intent intent = getActivity().getIntent();
        if (!ObjectUtils.isEmpty(intent)) {
            String email = intent.getStringExtra(INTENT_EMAIL_INFO);

            if (!ObjectUtils.isEmpty(email)) {
                mEmail.setText(email);
            }
        }

        return mRootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SIGNIN_VIA_FACEBOOK
                    || requestCode == REQUEST_SIGNIN_VIA_KAKAOTALK) {

                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            } else if (requestCode == REQUEST_SIGNUP) {
                if (!ObjectUtils.isEmpty(data)) {
                    String email = data.getStringExtra(INTENT_EMAIL_INFO);

                    if (!ObjectUtils.isEmpty(email)) {
                        mEmail.setText(email);
                    }
                } else {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == REQUEST_SIGNIN_VIA_FACEBOOK) {
                Toast.makeText(getActivity(), getString(R.string.error_signin_facebook),
                        Toast.LENGTH_LONG).show();
            } else if (requestCode == REQUEST_SIGNIN_VIA_KAKAOTALK) {
                Toast.makeText(getActivity(), getString(R.string.error_signin_kakaotalk),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
