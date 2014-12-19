
package com.kth.baasio.sample.ui.dialog;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.kth.baasio.sample.R;
import com.kth.baasio.utils.ObjectUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

public class EntityDialogFragment extends SherlockDialogFragment {
    public static final int CREATE_ENTITY = 0;

    public static final int MODIFY_ENTITY = 1;

    public static final int SEND_PUSH = 2;

    public static final int SEND_PUSH_BY_TARGET = 3;

    public static final int CREATE_GROUP = 4;

    public static final int MODIFY_GROUP = 5;

    public static final int CREATE_COMMENT = 6;

    public static final int MODIFY_COMMENT = 7;

    public static final int CHANGE_PASSWORD = 8;

    private int mMode = -1;

    private String mText1;

    private String mText2;

    private String mTitle;

    private ViewGroup mRoot;

    private EditText mTextBody1;

    private EditText mTextBody2;

    private CheckBox mCheckBox1;

    private CheckBox mCheckBox2;

    public static EntityDialogFragment newInstance() {
        EntityDialogFragment frag = new EntityDialogFragment();
        return frag;
    }

    private void setTitle(String title) {
        this.mTitle = title;
    }

    public void setText1(String text) {
        this.mText1 = text;
    }

    public void setText2(String text) {
        this.mText2 = text;
    }

    public void setShareMode(int mode) {
        this.mMode = mode;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setStyle(SherlockDialogFragment.STYLE_NORMAL, android.R.style.Theme_Dialog);

        switch (mMode) {
            case CREATE_ENTITY: {
                setTitle(getString(R.string.create_post_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_entity_post, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);
                mTextBody2 = (EditText)mRoot.findViewById(R.id.textInput2);
                break;
            }
            case MODIFY_ENTITY: {
                setTitle(getString(R.string.modify_post_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_entity_post, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);
                mTextBody2 = (EditText)mRoot.findViewById(R.id.textInput2);
                break;
            }
            case SEND_PUSH: {
                setTitle(getString(R.string.sendpush_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_sendpush, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);
                mTextBody2 = (EditText)mRoot.findViewById(R.id.textInput2);

                mCheckBox1 = (CheckBox)mRoot.findViewById(R.id.checkIOS);
                mCheckBox2 = (CheckBox)mRoot.findViewById(R.id.checkAndroid);
                break;
            }
            case SEND_PUSH_BY_TARGET: {
                setTitle(getString(R.string.sendpush_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_sendpush_by_target, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);
                break;
            }
            case CREATE_GROUP: {
                setTitle(getString(R.string.create_group_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_entity_group, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);
                break;
            }
            case MODIFY_GROUP: {
                setTitle(getString(R.string.modify_group_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_entity_group, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);
                break;
            }
            case CREATE_COMMENT: {
                setTitle(getString(R.string.create_comment_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_entity_comment, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);

                break;
            }
            case MODIFY_COMMENT: {
                setTitle(getString(R.string.modify_comment_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_entity_comment, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);

                break;
            }
            case CHANGE_PASSWORD: {
                setTitle(getString(R.string.change_password_dialog_title));
                mRoot = (ViewGroup)getActivity().getLayoutInflater().inflate(
                        R.layout.dialog_change_password, null);
                mTextBody1 = (EditText)mRoot.findViewById(R.id.textInput);
                mTextBody2 = (EditText)mRoot.findViewById(R.id.textInput2);
            }
            default:
                break;
        }

        if (!ObjectUtils.isEmpty(mText1) && !ObjectUtils.isEmpty(mTextBody1)) {
            mTextBody1.setText(mText1);
        }

        if (!ObjectUtils.isEmpty(mText2) && !ObjectUtils.isEmpty(mTextBody2)) {
            mTextBody2.setText(mText2);
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(mTitle)
                .setView(mRoot)
                .setPositiveButton(R.string.common_dialog_confirm,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String body = mTextBody1.getText().toString().trim();

                                if (mListener != null) {
                                    Bundle data = new Bundle();
                                    data.putString("text1", body);
                                    if (mMode == SEND_PUSH) {
                                        if (mCheckBox1.isChecked()) {
                                            data.putBoolean("ios", true);
                                        }
                                        if (mCheckBox2.isChecked()) {
                                            data.putBoolean("android", true);
                                        }
                                    }

                                    if (mTextBody2 != null) {
                                        String tag = mTextBody2.getText().toString().trim();
                                        if (!TextUtils.isEmpty(tag)) {
                                            data.putString("text2", tag);
                                        }
                                    }
                                    mListener.onPositiveButtonSelected(mMode, data);
                                }
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.common_dialog_cancel,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
    }

    private EntityDialogResultListener mListener;

    public interface EntityDialogResultListener {
        public boolean onPositiveButtonSelected(int mode, Bundle data);
    }

    public void setEntityDialogResultListener(EntityDialogResultListener listener) {
        mListener = listener;
    }
}
