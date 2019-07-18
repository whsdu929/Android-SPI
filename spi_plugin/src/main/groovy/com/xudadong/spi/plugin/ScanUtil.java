package com.xudadong.spi.plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <title>
 * <p>
 * Created by didi on 2019-07-16.
 */
class ScanUtil {
    static final String ANNOTATION_PROVIDE_DESC = "Lcom/xudadong/spi/core/Provide;";

    static boolean shouldProcessJarOrDir(String name) {
        return name != null && !name.startsWith("com.android.support") && !name.startsWith("android.arch");
    }

    static boolean shouldProcessFile(String name) {
        return name != null && name.endsWith(".class") &&
                !name.endsWith("BuildConfig.class") &&
                !name.startsWith("R$") &&
                !name.equals("R.class") &&
                !name.equals("com/xudadong/spi/core/Provide.class") &&
                !name.equals("com/xudadong/spi/core/ProvidersPool$1.class") &&
                !name.equals("com/xudadong/spi/core/ProvidersPool.class") &&
                !name.equals("com/xudadong/spi/core/ProvidersRegistry.class");
    }

    static void scanFile(File file) {
        try {
            scanInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描jar包里的所有class文件：
     * 1.通过包名识别所有需要注入的类名
     * 2.找到目标类所在的jar包，后面我们会在该jar包里进行代码注入
     */
    static void scanJar(File srcFile, File destFile) {
        if (srcFile != null) {
            try {
                JarFile jarFile = new JarFile(srcFile);
                Enumeration<JarEntry> enumeration = jarFile.entries();
                while(enumeration.hasMoreElements()){
                    JarEntry jarEntry = enumeration.nextElement();
                    //class文件的名称，这里是全路径类名，包名之间以"/"分隔
                    String jarEntryName = jarEntry.getName();
                    if(jarEntryName.startsWith(CodeGeneratorUtil.GENERATE_TO_CLASS_NAME)){
                        //扫描结束后，我们会生成注册代码到这个文件里
                        CodeGeneratorUtil.mServiceProviderFile = destFile;
                    } else if (shouldProcessFile(jarEntryName)) {
                        InputStream inputStream = jarFile.getInputStream(jarEntry);
                        scanInputStream(inputStream);
                        inputStream.close();
                    }
                }
                jarFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void scanInputStream(InputStream inputStream) {
        try {
            ClassReader classReader = new ClassReader(inputStream);
            // 构建一个ClassWriter对象，并设置让系统自动计算栈和本地变量大小
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            ProvideClassVisitor classVisitor = new ProvideClassVisitor(org.objectweb.asm.Opcodes.ASM6, classWriter);
            //开始扫描class文件
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ProvideClassVisitor extends ClassVisitor {
        String mClassName;

        public ProvideClassVisitor(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public void visit(int i, int i1, String s, String s1, String s2, String[] strings) {
            super.visit(i, i1, s, s1, s2, strings);
            mClassName = s;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (desc.endsWith(ANNOTATION_PROVIDE_DESC)) {
                av = new ProvideAnnotationVisitor(org.objectweb.asm.Opcodes.ASM6, av, mClassName);
            }
            return av;
        }
    }

    private static class ProvideAnnotationVisitor extends AnnotationVisitor {
        String mClassName;

        ProvideAnnotationVisitor(int api, AnnotationVisitor av, String className) {
            super(api, av);
            mClassName = className;
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            if (mClassName != null && mClassName.length() > 0 && value != null) {
                String key = (value.toString()).replace('/', '.');
                if (key.startsWith("L")) {
                    key = key.substring(1, key.length() - 1);
                }
                String provider = mClassName.replace('/', '.');

                Set<String> providersSet = CodeGeneratorUtil.mProvidersMap.get(key);
                if (providersSet == null) {
                    providersSet = new HashSet<>();
                    CodeGeneratorUtil.mProvidersMap.put(key, providersSet);
                }
                providersSet.add(provider);
            }
        }
    }
}
