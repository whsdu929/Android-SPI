package com.xudadong.spi.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <title>
 * <p>
 * Created by didi on 2019-07-15.
 */
class RegisterPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(AppPlugin.class)) {
            project.extensions.findByType(AppExtension.class).registerTransform(new RegisterTransform())
        }
    }
}
