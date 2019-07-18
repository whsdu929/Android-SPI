package com.xudadong.app;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.xudadong.app.adapter.FragmentPagerItemAdapter;
import com.xudadong.common.AbsFragment;
import com.xudadong.common.DLog;
import com.xudadong.spi.core.ServiceProvider;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Android-SPI");
        loadFragments();
    }

    private void loadFragments() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager viewPager = findViewById(R.id.viewPager);
        final List<AbsFragment> fragments = ServiceProvider.getProviders(AbsFragment.class);
        DLog.d("fragment count:" + fragments.size());

        FragmentPagerItemAdapter.Builder builder = new FragmentPagerItemAdapter.Builder(this, getSupportFragmentManager());
        for (AbsFragment fragment : fragments) {
            builder.add(fragment.getName(), fragment);
        }
        viewPager.setAdapter(builder.build());
        tabLayout.setupWithViewPager(viewPager);
    }
}
