package com.xudadong.spi.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <title>
 * <p>
 * Created by didi on 2019-07-15.
 */
class RegisterTransform extends Transform {
    @Override
    public String getName() {
        return "RegisterTransform";
    }

    //该Transform支持扫描的文件类型，分为class文件和资源文件，我们这里只处理class文件的扫描
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    //Transfrom的扫描范围，我这里扫描整个工程，包括当前module以及其他jar包、aar文件等所有的class
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        Iterator<TransformInput> iterator = transformInvocation.getInputs().iterator();
        while (iterator.hasNext()) {
            //inputs就是所有扫描到的class文件或者是jar包，一共2种类型
            TransformInput transformInput = iterator.next();

            //1.遍历所有的class文件目录
            Iterator<DirectoryInput> directoryInputIterator = transformInput.getDirectoryInputs().iterator();
            while(directoryInputIterator.hasNext()) {
                DirectoryInput directoryInput = directoryInputIterator.next();
                File srcFile = directoryInput.getFile();
                if (ScanUtil.shouldProcessJarOrDir(srcFile.getName())) {
                    List<File> allFiles = getAllFile(srcFile.getAbsolutePath(), false);
                    for(File file : allFiles){
                        if (ScanUtil.shouldProcessFile(file.getName())) {
                            ScanUtil.scanFile(file);
                        }
                    }
                }
                //Transform扫描的class文件是输入文件(input)，有输入必然会有输出(output)，处理完成后需要将输入文件拷贝到一个输出目录下去，
                //后面打包将class文件转换成dex文件时，直接采用的就是输出目录下的class文件了。
                //必须这样获取输出路径的目录名称
                File destDir = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                FileUtils.copyDirectory(srcFile, destDir);
            }

            //2.遍历查找所有的jar包
            Iterator<JarInput> jarInputIterator = transformInput.getJarInputs().iterator();
            while(jarInputIterator.hasNext()) {
                JarInput jarInput = jarInputIterator.next();
                File srcFile = jarInput.getFile();
                File destFile = outputProvider.getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                if (ScanUtil.shouldProcessJarOrDir(jarInput.getName())) {
                    //扫描jar包的核心代码在这里，主要做2件事情：
                    //1.扫描该jar包里有没有使用注解的类;
                    //2.扫描ServiceProvider这个类在哪个jar包里，并记录下来，后面需要在该类里动态注入字节码；
                    ScanUtil.scanJar(srcFile, destFile);
                }
                FileUtils.copyFile(srcFile, destFile);
            }
        }

        //1.通过前面的步骤，我们已经扫描到所有使用了xx注解的类；
        //2.后面需要在app初始化方法里，动态注入字节码；
        //3.将所有扫描到的类，通过类名进行反射调用实例化
        CodeGeneratorUtil.generateRegisterCode();
    }

    private static List<File> getAllFile(String directoryPath, boolean isAddDirectory) {
        List<File> list = new ArrayList<>();
        File baseFile = new File(directoryPath);
        if (baseFile.isFile() || !baseFile.exists()) {
            return list;
        }
        File[] files = baseFile.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if(isAddDirectory){
                    list.add(file);
                }
                list.addAll(getAllFile(file.getAbsolutePath(), isAddDirectory));
            } else {
                list.add(file);
            }
        }
        return list;
    }
}
