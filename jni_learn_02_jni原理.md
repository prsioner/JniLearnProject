####1.extern "C"的作用

extern “C”是用来在C++程序中声明或定义一个C的符号，说明当前修饰的内容需要使用C编译器来编译

因为C不支持重载，C++ 支持重载，如果不加extern c ，那么jni 定义的方法在cpp文件中会当成
C++ 来重载，会自动带上env，jobject 等参数，这样，跟我们在java 层定义的jni方法（不带参数）就
不一样了，编译器会报错，提示找不到方法，所以需要加extern C 让C编译器来编译，目的是让 方法名
和java 层定义的方法名一致,而且不带参数
eg：
java:
public native void sendCallBackCmd();

jni：
extern "C"
JNIEXPORT void JNICALL
Java_com_prsioner_jnilearnproject_MainActivity_sendCallBackCmd(JNIEnv *env, jobject thiz)；

C编译之后就是：sendCallBackCmd(); 


####2.JNIEXPORT
是一个宏定义
#define JNIEXPORT  __attribute__ ((visibility ("default")))
用于定义jni函数的可见性，"default"和"hidden"对应与java 中的public和private


####3.动态注册与静态注册
eg:

动态注册和静态注册都是为了让java定义的jni方法和c++ 内实现的jni方法做一个映射关系，
动态注册native方法形式不同，需要手动从JavaVM 虚拟机引擎中获取jniEnv环境
动态注册代码示例 
java 中定义了一个
public native String stringFromJNI();
那么native-lib.cpp中就得这样注册
```
jstring stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
static const JNINativeMethod gMethods[] ={
{
    "stringFromJNI",
    "()Ljava/lang/String",
    "(jstring*)stringFromJNI"
}

};

JNIEXPORT jint JNI_Onload(JavaVM * vm,void* reserved){
  JniEnv *jniEnv = NULL;
  if(vm->getEnv(&jniEnv,JNI_VERSION_1_4)!=JNI_OK){
  return -1
  }
  jclass clazz = env-> FindClass("com/prsioner/jnilearnproject/MainActivity")
    env->RegisterNatives(clazz,gMethods,1);

  return JNI_VERSION_1_4;
}
```


下面的是静态注册
java层代码:
```

callBackBtn.setOnClickListener(v -> {
            sendCallBackCmd();
        });
    /**
     * send command and call back
     */
    public native void sendCallBackCmd();

/**
     * 给JNI 回调的方法
     */
    public void jniCallBack(int code){
        Toast.makeText(MainActivity.this,"call back by jni code:"+code,Toast.LENGTH_SHORT).show();
    }
```
native-lib.cpp代码：

```
extern "C"
JNIEXPORT void JNICALL
Java_com_prsioner_jnilearnproject_MainActivity_sendCallBackCmd(JNIEnv *env, jobject thiz) {

    jclass activityClass = env->GetObjectClass(thiz);
    // C/C++调用java的方法时，需要使用反射
    //通过反射的方式获取到方法Id
    jmethodID idText = env->GetMethodID(activityClass,"jniCallBack","(I)V");
    int code = 99;
    //std::int32_t code1  = 99;

    //根据java 层定义的回调方法的返回值类型选择对应env方法
    env->CallVoidMethod(thiz,idText,code);
    
}
```

####4.纯java 工程如何通过jni 调用 c/c++ ？
步骤1. 定义一个java 类  Test.java
```
package com; 
public class Test{ 
    static{ 
            System.loadLibrary("bridge");
         }
    public native int nativeAdd(int x,int y);
 
    public static void main(String[] args){
        Test obj = new Test(); 
        System.out.printf("%d\n",obj.nativeAdd(2012,3));
     }
     }
```
2.进行代码编译:
cmd 命令行中:
javac com/Test.java
javah -jni com.Test
 
会生成一个com_Test.h文件
```
#include <jni.h>
#ifndef _Included_com_Test 
#define _Included_com_Test 
#ifdef __cplusplus 
extern "C" { 
#endif
 /** Class: com_Test 
   * Method: nativeAdd 
   * Signature: (II)I 
   */ 
JNIEXPORT jint JNICALL Java_com_Test_nativeAdd (JNIEnv *, jobject, jint, jint); 
#ifdef 
__cplusplus 
}
#endif
```
意思是如果是c++ 环境就用c编译器来编译执行 extern c 内的代码
如果是c环境，就不编译extern c 内的代码

3.写一个c文件 比如brige.c，来实现nativeAdd这个方法
代码如下:
```
#include "com_test.h"
JNIEXPORT jint JNICALL Java_com_Test_nativeAdd(JNIEnv * env, jobject obj, jint x, jint y){
    
    return x+y;
}
```
4.c文件在linux 系统内的可执行文件是.so,所以我们需要把.c文件编译成.so文件
```
gcc -shared -I C:\Program Files\Java\jdk1.8.0_181\include -I C:\Program
Files\Java\jdk1.8.0_181\include\win32 bridge.c -o libbridge.so
```
注意:
1.注意这里几个gcc的选项， -shared 是说明要生成动态库，而两个 -I 的选项，是因为我们用到 <jni.h>
相关的头文件是放在 <jdk>/include 和 <jdk>/include/linux 两个目录下。
最后需要注意一点的是 -o 选项，我们在java代码中调用的是 System.loadLibrary("xxx") ,那么生成
的动态链接库的名称就必须是 libxxx.so 的形式（这里指Linux环境），否则在执行java代码的时候，
就会报 java.lang.UnsatisfiedLinkError: no XXX in java.library.path 的错误

2.c/C++中，引用本地的.h时，使用" "  eg: #include "jni.h"
        引用系统库时，使用 <>   eg: #include <jni.h>

其实so 库在load时，会在内存生成一个函数映射表，key 是java_包名_类名_方法名, value 是真正这个函数的地址
在程序运行时，需要调用方法，虚拟机会根据方法是否有申明native , 如果有声明，就 去so 的函数映射表里
找这个方法，然后拿到地址去执行
如果时普通函数，就去class 里面找，找到后执行

#####.Java对象和C++的对象
java 的string 对象存放在方法区（存放class信息、常量池等）
C++ 的string 对象存放在栈区
