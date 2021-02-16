###1.CMake编译演变
目前android studio 支持的NDK开发编译工具就是Cmake,在此之前，用的比较多的是
gcc 编译工具，gcc 过程太过繁琐，后面有了makefile 语法，但是makefile 语法只支持linux 环境
后面就有了cmake 语法（底层依然是makefile），它简化编译指令，支持window linux mac 环境

NDK 在18以后是使用clang 编译器编译，18之前使用的是gcc
cmake 只是对clang 进行了一个封装，简化步骤


####1.1 NDK 编译过程
一般NDK开发的相关功能，需要编译生成.a 、.so等可执行文件来给外部调用
编译过程:
.h/.cpp等->预编译->编译->汇编->链接——>生产可执行文件
 
eg:有一个helloworld.c 
```
#include <printf.h>
int main(){
    printf("hello world!!!");
    return 0;
}
```
程序要运行起来就需要经过好几个步骤:
mac 环境中进行编译过程
```
1.预处理 :主要进行宏替换
gcc -E helloworld.c -o hello.i -E

2.编译: 将C代码转换为汇编代码
gcc -S hello.i -o hello.s -S
3.汇编:仅执行编译操作，不进行连接操作
gcc -c hello.c -o hello.o -c
4.连接 :生成可执行文件main
gcc hello.o -o hello

或者一步到位的编译(在不需要链接其他库的时候可以用),生成可执行文件hello
gcc helloworld.c -o hello
```

####1.2 CMake 如何在android studio 中工作
#####1.创建C++ 工程项目
app 目录下 build.gradle 中自动添加了如下配置
```
android{
    defaultConfig {
        externalNativeBuild {
                    cmake {
                        cppFlags ""
                    }
                }
    }
}
//编译入口
externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.10.2"
        }
    }
```
#####2.Make Project时 CMake 做了什么
######1.生成.cxx目录
c++ 工程构建时，cmake会自动生成支持不同ABI cpu 架构的中间文件 
在 app-> .cxx-> debug（release） 路径下
不同架构的版本存在向上兼容的效果,比如arm-v8a 编译出的so 库支持在arm-v7a的cpu 下运行
v7a 编译出的so 也可以在arm-v8a的cpu 运行
项目中一般使用arm-v7a ,可以在build.gradle中指定
```
externalNativeBuild {
            cmake {
                cppFlags ""
            }
            ndk{
                abiFilters 'armeabi-v7a'
            }
        }
```
**注意:修改后需要手动删除一下.cxx 和build文件夹
生成的动态库so 在  app-> build-> intermediates -> cmake-> debug ->obj->armeabi-v7a**

######2. build_command.txt
build.gradle 中的配置都会体现在cmake 命令文件build_command.txt中
```
cmake的可执行文件在那个路径 （绝对路径） 
Executable : /Users/qinglin/programFiles/android/sdk/cmake/3.10.2.4988404/bin/cmake
执行cmake时 携带的参数 
arguments : 
编译的源码放在哪个文件夹 
-H/Users/qinglin/AndroidStudioProjects/JniLearnProject/app/src/main/cpp
导入系统的库 如liblog.so libjnigraphics.so
-DCMAKE_TOOLCHAIN_FILE=/Users/qinglin/programFiles/android/sdk/ndk/21.1.6352462/build/cmake/android.toolchain.cmake
编译平台 
-DANDROID_ABI=armeabi-v7a
当前编译 生成so的版本 
-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a
输出路径：
-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=/Users/qinglin/AndroidStudioProjects/JniLearnProject/app/build/intermediates/cmake/debug/obj/armeabi-v7a

```
如果需要修改配置，需要在build.gradle中修改
```
externalNativeBuild {
            cmake {
                cppFlags ""
                arguments "-DANDROID_PLATFORM=android-24","-DANDROID_TOOLCHAIN=clang"
            }
            ndk{
                abiFilters 'armeabi-v7a'
            }
        }
```
编译完成后，将so 文件通过android  studio 脚本 与java 编译的dex 文件一起打包成apk文件


###2.手写CMake编译命令
cmake 指令不区分大小写

####1.示例工程 
[CMakeTestProjects](https://github.com/prsioner/CMakeTestProjects)
1.cmake文件,注意，文件名必须是 CMakeLists.txt
2.编辑cmakelists.txt文件
```
cmake_minimum_required(VERSION 3.17)
project(CMakeTestProjects C)

set(CMAKE_C_STANDARD 99)

# 编译时需包含include里的相关头文件
include_directories(include)

#需要把相关的.c 都加进去编译
add_executable(CMakeTestProjects main.c include/add.c)
```
3.运行即可 

####2.cmake 的常用指令
cmake_minimum_required: 指定需要CMAKE的最小版本
include_directories: 指定 原生代码 或 so库 的头文件路径
add_library: 添加 源文件或库
set_target_properties(<> PROPERTIES IMPORTED_LOCATION):  指定 导入库的路径
set_target_properties(<> PROPERTIES LIBRARY_OUTPUT_DIRECTORY): 指定生成的目标库的导出路径
ænd_library: 添加NDK API 
target_link_libraries: 将预构建库关联到原生库
aux_source_directory: 查找在某个路径下的所有源文件

message ()： 可以进行cmake打印调试日志,系统变量可以通过arguments 里的相关字段取出来
eg:
message(${PROJECT_SOURCE_DIR}) //打印工程路径
message(${ANDROID_NDK})   //打印ndk 本地路径


###3.动态库与静态库的区别
本质上来说库是一种可可执行代码的二进制形式，可以被操作系统载入内存执行，库有两种：静态库
（.a、.lib）和动态库（.so、.so）
####1.静态库:
是因为在链接阶段，会将汇编生成的目标文件.o与引用到的库一起链接打包到可执行文件
中，对应的链接方式成为静态链接
优点:程序在运行时与函数库就没有关系，移植方便,代码装载速度快，执行速度略比动态链接库快
缺点:浪费空间和资源，所有相关的目标文件与牵涉的函数库被链接合成一个可执行文件
如果静态库需要更新，则所有使用它的应用程序都需要重
新编译、发布给用户（一个小的改动，可能导致整个程序重新下载）
如何生成静态库？
```
cmake_minimum_required(VERSION 3.17)
project(CMakeTestProjects C)

set(CMAKE_C_STANDARD 99)

# 编译时需包含include里的相关头文件
include_directories(include)

#打印工程路径
message(STATUS,"-------",${PROJECT_SOURCE_DIR})

#需要把相关的.c 都加进去编译
#add_executable(CMakeTestProjects main.c include/add.c)

# 不写SHARED默认也是生成静态库  libCMakeTestProjects.a
add_library(CMakeTestProjects main.c include/add.c)
```


####2.动态库
动态库实现的是与目标文件之间分离，在目标文件需要使用库时动态加载,比如 使用静态代码块加载so库
```
public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }
...
}
```
可以实现进程之间资源共享（因此动态库也称为共享库），动态库它在内存中只存在一份拷贝，避免了静态库的内存空间浪费
动态链接库的优点
> 1. 更加节省内存并减少页面交换；
> 2. so文件与EXE文件独立，只要输出接口不变（即名称、参数、返回值类型和调用约定不变），更换so
文件不会对EXE文件造成任何影响，因而极大地提高了可维护性和可扩展性；
> 3. 不同编程语言编写的程序只要按照函数调用约定就可以调用同一个so函数；
> 4. 适用于大规模的软件开发，使开发过程独立、耦合度小，便于不同开发者和开发组织之间进行开发和测试

如何生成动态库？

```
...
#生成一个动态库，动态库基于静态库的生成，所以，也会生产.a 再生成 libCMakeTestProjects.dylib
add_library(CMakeTestProjects SHARED main.c include/add.c)
```

####3.动态库/静态库提供给其他工程使用
类似与Java中工程导入第三方的jar 包

1.需要导入so库或者静态库（放到目标工程的lib 文件夹下面），
还需要提供库的头文件(.h文件,放到include文件夹下),

2.下一步就是目标工程配置 CmakeLists.txt 加上如下指令

```
#编译头文件
include_directories(${PROJECT_SOURCE_DIR}/include)
add_library(staticFiled STATIC IMPORTED)
SET_PROPERTY(TARGET PROPERTY IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/lib/libCMakeTestProjects.a)
#链接到目标文件库
target_link_libraries( # Specifies the target library.
                       native-lib
                       ${log-lib} )
``` 
这样就可以把so 库合并到目标工程的native-lib库中
