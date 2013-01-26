
package com.kth.common.sns.tools;

import com.kth.baasio.sample.BuildConfig;

public class SnsConfig {
    public static final String FACEBOOK_APPID = BuildConfig.DEBUG ? "PUT_YOUR_FACEBOOK_APPID_FOR_DEBUG"
            : "PUT_YOUR_FACEBOOK_APPID_FOR_RELEASE";

    public static final String[] FACEBOOK_PERMISSIONS = new String[] {
            "publish_stream", "read_stream", "offline_access", "email"
    };
}
