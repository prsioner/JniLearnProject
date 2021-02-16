###1.JNI
####1.JNI 和NDK的区别
JNI:
提供一种Java调用C/C++的解决方案，相当于java 和C/C++之间连接的桥梁,JNI描述的是一种技术

NDK:
Android NDK 是一组允许您将 C 或 C++（“原生代码”）嵌入到 Android 应用中的工具，NDK描述
的是工具集,NDK主要作用是:
> + 实现功能库平台之间的移植
> + 重复现有库(google 提供了很多extral库,对应处理不同的功能)，或者提供自己的C++库来重复使用
> + 有助于提高某些情况下的程序性能，比如游戏等计算密集型应用


####2.JNI 方法的注册
#####2.1 静态注册
1.定义：
当Java层调用native函数时，会在JNI库中根据函数名查找对应的JNI函数。如果没找到，会报错。
如果找到了，则会在native函数与JNI函数之间建立关联关系，其实就是保存JNI函数的函数指针。
下次再调用native函数，就可以直接使用这个函数指针

2.jni函数格式
java_包名_类名_函数名
eg：Java_com_prsioner_jnilearnproject_MainActivity_stringFromJNI(...)

3.静态注册的缺点
> + 要求JNI函数的名字必须遵循JNI规范的命名格式；
> + 名字冗长，容易出错；
> + 初次调用会根据函数名去搜索JNI中对应的函数，会影响执行效率；
> + 需要编译所有声明了native函数的Java类，每个所生成的class文件都要用javah工具生成一个头文
件

**android studio 在创建C++ 工程时会帮我们自动切换到混合编程环境，声明native方法时也提供了自动创建对应jni函数的便捷操作**
**当然jni 也并不是android 特有的技术，纯java环境也是有一系列的jni工具来协助我们完成开发(jdk中提供相应的jni代码编译工具)，只是使用起来有些麻烦**

#####2.2 动态注册
通过提供一个函数映射表，注册给JVM虚拟机，这样JVM就可以用函数映射表来调用相应的函数，就不必通过函数名来查找需要调用的函数
Java与JNI通过JNINativeMethod的结构来建立函数映射表，它在jni.h头文件中定义，其结构内容如
下：
```
typedef struct { 
 const char* name;
 const char* signature; 
 void* fnPtr; 
} JNINativeMethod;
```
> 1. 创建映射表后，调用RegisterNatives函数将映射表注册给JVM;
> 2. 当Java层通过System.loadLibrary加载JNI库时，会在库中查JNI_OnLoad函数。可将JNI_OnLoad视
     为JNI库的入口函数，需要在这里完成所有函数映射和动态注册工作，及其他一些初始化工作

####3.数据类型的转换
我们知道，java的数据类型是不能直接给C/C++ 使用的，所以需要在JNI层做一个转换

对应关系如下:        

|  Java 类型   | JNI类型   |  描述      |
|  ----       | ----     | ----      |
| boolean     | jboolean | 无符号8位   |
| byte        | jbyte    | 有符号8位   |
| char        | jchar    | 有符号16位  |
| short       | jshort   | 有符号16位  |
| int         |  jint    |  有符号32位 |
| long        | jlong    | 有符号64位  |
| float       | jfloat   | 32位       |
| double      | jdouble  | 64位       |

引用数据类型转换
**除了Class、String、Throwable和基本数据类型的数组外，其余所有Java对象的数据类型在JNI中
  都用jobject表示。Java中的String也是引用类型，但是由于使用频率较高，所以在JNI中单独创建
  了一个jstring类型**

| java 引用类型 |              JNI 类型 | java引用类型 | JNI类型      |
|    ----             |   ----        |  ----      | --------    |
|  All Object         |   jobject     | short[]    | jshortArray |
| java.lang.Class     | jclass        | int[]      | jintArray   |
| java.lang.String    | jstring       | long[]     | jlongArray  |
| java.lang.Throwable | jthrowable    |float[]     | jfloatArray |
| Object[]            | jobjectArray  | double[]   | jdoubleArray |
| boolean[]           | jbooleanArray | char[]     | jcharArray  |
| byte[]              | jbyteArray    |

多维数组（含二维数组）都是引用类型，需要使用 jobjectArray 类型存取其值；
例如，二维整型数组就是指向一位数组的数组，其声明使用方式如下:
```
//获得一维数组的类引用，即jintArray类型 
jclass intArrayClass = env->FindClass("[I"); 
//构造一个指向jintArray类一维数组的对象数组，该对象数组初始大小为length，类型为 jsize 
jobjectArray obejctIntArray = env->NewObjectArray(length ,intArrayClass , NULL);
```

####4.JNI 函数签名信息
**由于Java支持函数重载，因此仅仅根据函数名是没法找到对应的JNI函数。为了解决这个问题，JNI
将参数类型和返回值类型作为函数的签名信息**

eg:函数签名例子:

| Java 函数                | 函数签名                    |
|  ----                   |  ----                      |
|    string fun()         |  "()Ljava/lang/String;"    |
|  long fun(int a,Class c)| "(ILjava/lang/Class;)J"    |
|  void fun(byte[] bytes) | "([B)V"                    |

java常用数据类型及对应字符表

|   java 类型 |    字符    |
|   ----     |    ----    |
|   void     |   V        |
|  boolean   |   Z        |
|  int       |   I        |
| long       |   J        |
| double     |   D        |
| float      |   F        |
| byte       |   B        |
| char       |   C        |
| short      |   S        |
|  int[]     |  [I        |
|  String    | Ljava/lang/String; |
| Object[]   | [Ljava/lang/object; |


####5. JNIEnv介绍
#####5.1 JNIEnv 概念
JNIEnv是一个线程相关的结构体, 该结构体代表了 Java 在本线程的运行环境。通过JNIEnv可以调用
到一系列JNI系统函数。
#####5.2 JNIEnv线程相关性
每个线程中都有一个 JNIEnv 指针。JNIEnv只在其所在线程有效, 它不能在线程间传递
**注意：在C++创建的子线程中获取JNIEnv，要通过调用JavaVM的AttachCurrentThread函数获
  得。在子线程退出时，要调用JavaVM的DetachCurrentThread函数来释放对应的资源，否则会出
  错**
JNIEnv 作用：
> + 访问Java成员变量和成员方法；
> + 调用Java构造方法创建Java对象等

####6.JNI 编译

native 工程的入口在 app的build.gradle 中申明的:
```
externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.10.2"
        }
    }
```
然后找到 CMakeLists.txt  进行Cmake编译



#####1. ndkBuild 
[使用ndk-build编译生成so文件](https://blog.csdn.net/cheng2290/article/details/77717164)

#####2.Cmake编译
[Cmake的使用](https://www.jianshu.com/p/c71ec5d63f0d)
CMake 则是一个跨平台的编译工具，它并不会直接编译出对象，而是根据自定义的语言规则
（CMakeLists.txt）生成 对应 makefile 或 project 文件，然后再调用底层的编译， 在Android
Studio 2.2 之后支持Cmake编译

######2.1 add_library 指令
1.add_library 指令
**语法：add_library(libname [SHARED | STATIC | MODULE] [EXCLUDE_FROM_ALL] [source])**
将一组源文件 source 编译出一个库文件，并保存为 libname.so (lib 前缀是生成文件时 CMake自
动添加上去的)。其中有三种库文件类型，不写的话，默认为 STATIC;
> + SHARED: 表示动态库，可以在(Java)代码中使用 System.loadLibrary(name) 动态调用；
> + STATIC: 表示静态库，集成到代码中会在编译时调用；
> + MODULE: 只有在使用 dyId 的系统有效，如果不支持 dyId，则被当作 SHARED 对待；
> + EXCLUDE_FROM_ALL: 表示这个库不被默认构建，除非其他组件依赖或手工构建

```
将compress.c 编译成 libcompress.so 的共享库 
add_library(compress SHARED compress.c)
```
2.target_link_libraries 指令
**语法：target_link_libraries(target library <debug | optimized> library2…)**

这个指令可以用来为 target 添加需要的链接的共享库，同样也可以用于为自己编写的共享库添加
共享库链接
```
指定 compress 工程需要用到 libjpeg 库和 log 库 
target_link_libraries(compress libjpeg ${log-lib})
```
3.find_library 指令
**语法：find_library( name1 path1 path2 ...)**
 VAR 变量表示找到的库全路径，包含库文件名
```
find_library(libX X11 /usr/lib) find_library(log-lib log) 
路径为空，应该是查找系统环境变量路径
```

####7.Abi架构
ABI（Application binary interface）应用程序二进制接口。不同的CPU 与指令集的每种组合都有
定义的 ABI (应用程序二进制接口)，一段程序只有遵循这个接口规范才能在该 CPU 上运行，所以
同样的程序代码为了兼容多个不同的CPU，需要为不同的 ABI 构建不同的库文件。当然对于CPU来
说，不同的架构并不意味着一定互不兼容

> + armeabi设备只兼容armeabi；
> + armeabi-v7a设备兼容armeabi-v7a、armeabi；
> + arm64-v8a设备兼容arm64-v8a、armeabi-v7a、armeabi；
> + X86设备兼容X86、armeabi；
> + X86_64设备兼容X86_64、X86、armeabi；
> + mips64设备兼容mips64、mips；
> + mips只兼容mips

总结规律:
> 1. armeabi的SO文件基本上都兼容，它能运行在除了mips和mips64的设备上，但在非
armeabi设备上运行性能还是有所损耗；
> 2. 64位的CPU架构总能向下兼容其对应的32位指令集，如：x86_64兼容X86，arm64-v8a兼容
armeabi-v7a，mips64兼容mips













  

       
