package com.xudadong.spi.plugin;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * <title>
 * <p>
 * Created by didi on 2019-07-16.
 */
class CodeGeneratorUtil {

    static final String GENERATE_TO_CLASS_NAME = "com/xudadong/spi/core/ServiceProvider.class";
    static final String GENERATE_TO_METHOD_NAME = "register";

    static File mServiceProviderFile = null;
    static Map<String, Set<String>> mProvidersMap = new HashMap<>();

    static void generateRegisterCode() {
        if (mServiceProviderFile != null && mServiceProviderFile.getName().endsWith(".jar") && mProvidersMap != null && mProvidersMap.size() > 0) {
            insertRegisterCodeIntoJarFile(mServiceProviderFile);
            mServiceProviderFile = null;
            mProvidersMap.clear();
        }
    }

    private static void insertRegisterCodeIntoJarFile(File jarFile) {
        if (jarFile != null) {
            //创建一个临时jar文件，要修改注入的字节码会先写入该文件里
            File optJar = new File(jarFile.getParent(), jarFile.getName() + ".opt");
            if (optJar.exists()) {
                optJar.delete();
            }
            try {
                JarFile file = new JarFile(jarFile);
                Enumeration enumeration = file.entries();
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                    String entryName = jarEntry.getName();
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    InputStream inputStream = file.getInputStream(jarEntry);
                    jarOutputStream.putNextEntry(zipEntry);
                    if (GENERATE_TO_CLASS_NAME.equals(entryName)) {
                        //找到需要插入代码的class，通过ASM动态注入字节码
                        ClassReader classReader = new ClassReader(inputStream);
                        // 构建一个ClassWriter对象，并设置让系统自动计算栈和本地变量大小
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                        ClassVisitor classVisitor = new ServiceProviderClassVisitor(Opcodes.ASM6, classWriter);
                        //开始扫描class文件
                        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

                        byte[] bytes = classWriter.toByteArray();
                        //将注入过字节码的class，写入临时jar文件里
                        jarOutputStream.write(bytes);
                    } else {
                        jarOutputStream.write(IOUtils.toByteArray(inputStream));
                    }
                    inputStream.close();
                    jarOutputStream.closeEntry();
                }
                jarOutputStream.close();
                file.close();
                //删除原来的jar文件
                if (jarFile.exists()) {
                    jarFile.delete();
                }
                //重新命名临时jar文件，新的jar包里已经包含了我们注入的字节码了
                optJar.renameTo(jarFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ServiceProviderClassVisitor extends ClassVisitor {
        public ServiceProviderClassVisitor(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
            MethodVisitor methodVisitor =  super.visitMethod(i, s, s1, s2, strings);
            if (s.equals(GENERATE_TO_METHOD_NAME)) {
                methodVisitor = new RegisterMethodVisitor(Opcodes.ASM6, methodVisitor);
            }
            return methodVisitor;
        }
    }

    private static class RegisterMethodVisitor extends MethodVisitor {
        public RegisterMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
                Iterator<Map.Entry<String, Set<String>>> iterator = mProvidersMap.entrySet().iterator();
                while(iterator.hasNext()) {
                    Map.Entry<String, Set<String>> entry = iterator.next();
                    String key = entry.getKey();
                    Set<String> providersSet = entry.getValue();
                    if (providersSet != null && providersSet.size() > 0) {
                        Iterator<String> stringIterator = providersSet.iterator();
                        while(stringIterator.hasNext()){
                            String provider = stringIterator.next();
                            mv.visitFieldInsn(Opcodes.GETSTATIC, "com/xudadong/spi/core/ProvidersPool", "registry", "Lcom/xudadong/spi/core/ProvidersRegistry;");
                            mv.visitLdcInsn(key);
                            mv.visitLdcInsn(provider);
                            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "com/xudadong/spi/core/ProvidersRegistry", "register", "(Ljava/lang/String;Ljava/lang/String;)V", true);
                        }
                    }
                }
            }
            super.visitInsn(opcode);
        }
    }
}
