
package com.kth.common.sns.tools.facebook;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

public class ProgessDialogFragment extends DialogFragment {

    private String mBody;

    public static ProgessDialogFragment newInstance() {
        ProgessDialogFragment frag = new ProgessDialogFragment();
        return frag;
    }

    public void setBody(String body) {
        mBody = body;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final ProgressDialog dialog = new ProgressDialog(getActivity());
        if (mBody != null && mBody.length() > 0) {
            dialog.setMessage(mBody);
        }

        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        // Disable the back button
        OnKeyListener keyListener = new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }
        };
        dialog.setOnKeyListener(keyListener);
        return dialog;
    }
}
