
package com.kth.baasio.sample.ui.pulltorefresh;

import com.kth.baasio.sample.R;

import android.content.Context;
import android.view.LayoutInflater;

public class LoadingLayoutFooter extends LoadingLayout {

    public LoadingLayoutFooter(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_footer, this);
    }
}
