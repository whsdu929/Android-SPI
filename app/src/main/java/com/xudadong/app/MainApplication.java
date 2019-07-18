package com.xudadong.app;

import android.app.Application;

import com.xudadong.common.AbsApplication;
import com.xudadong.spi.core.Provide;
import com.xudadong.spi.core.ServiceProvider;

import java.util.List;

/**
 * <title>
 * <p>
 * Created by didi on 2019-07-15.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ServiceProvider.init();
        List<AbsApplication> applications = ServiceProvider.getProviders(AbsApplication.class);
        for (AbsApplication delegate : applications) {
            delegate.onCreate(this);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        ServiceProvider.destroy();
    }
}
