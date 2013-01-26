/**
 * 0. Project  : XXXX 프로젝트
 *
 * 1. FileName : UserForGroupActivity.java
 * 2. Package : com.kth.baasio.baassample.ui.main
 * 3. Comment : 
 * 4. 작성자  : Brad
 * 5. 작성일  : 2012. 12. 10. 오후 8:14:18
 * 6. 변경이력 : 
 *                    이름     : 일자          : 근거자료   : 변경내용
 *                   ------------------------------------------------------
 *                    Brad : 2012. 12. 10. :            : 신규 개발.
 */

package com.kth.baasio.sample.ui.main;

import com.kth.baasio.sample.ui.SimpleSinglePaneActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * <PRE>
 * 1. ClassName : 
 * 2. FileName  : UserForGroupActivity.java
 * 3. Package  : com.kth.baasio.baassample.ui.main
 * 4. Comment  : 
 * 5. 작성자   : Brad
 * 6. 작성일   : 2012. 12. 10. 오후 8:14:18
 * </PRE>
 */
public class UserForGroupActivity extends SimpleSinglePaneActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /*
     * (non-Javadoc)
     * @see com.kth.baasio.baassample.ui.SimpleSinglePaneActivity#onCreatePane()
     */
    @Override
    protected Fragment onCreatePane() {
        UserFragment fragment = new UserFragment();
        return fragment;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onNewIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }
}
