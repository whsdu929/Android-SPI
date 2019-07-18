package com.xudadong.module.clippers;

import android.app.Application;

import com.xudadong.common.AbsApplication;
import com.xudadong.common.DLog;
import com.xudadong.spi.core.Provide;

/**
 * <title>
 * <p>
 * Created by didi on 2019-07-15.
 */
@Provide(AbsApplication.class)
public class ClippersApplication extends AbsApplication {
    @Override
    public void onCreate(Application application) {
        DLog.d("ClippersApplication -- onCreate");
    }
}
