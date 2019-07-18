package com.xudadong.module.lakers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xudadong.common.AbsFragment;
import com.xudadong.spi.core.Provide;
import com.xudadong.spi.core.ServiceProvider;

import java.util.List;

/**
 * <title>
 * <p>
 * Created by didi on 2019-07-15.
 */
@Provide(AbsFragment.class)
public class LakersFragment extends AbsFragment {
    @Override
    public String getName() {
        return "湖人";
    }

    @Override
    public String getSharedData() {
        return "伦纳德加盟后，我们将会组成一支拥有三巨头的超级球队";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lakers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        StringBuilder sb = new StringBuilder("来自各方的消息：\n");
        List<AbsFragment> fragments = ServiceProvider.getProviders(AbsFragment.class);
        for (AbsFragment fragment : fragments) {
            if (!fragment.getClass().getName().equals(getClass().getName())) {
                sb.append("\n").append(fragment.getName()).append(": ").append(fragment.getSharedData()).append("\n");
            }
        }

        TextView vInfo = getView().findViewById(R.id.vInfo);
        vInfo.setText(sb);
    }
}
