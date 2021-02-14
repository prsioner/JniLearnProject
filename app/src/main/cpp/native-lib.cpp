#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_prsioner_jnilearnproject_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_prsioner_jnilearnproject_MainActivity_sendDataToJni(JNIEnv *env, jobject thiz,
                                                             jstring class_name) {
    //char *String = "I Love XQQ";
    //也可以，但是要注意数据格式类型
    //jstring *className = (jstring *) env->GetStringUTFChars(class_name, 0);
    const char* className = env->GetStringUTFChars(class_name,0);

    __android_log_print(ANDROID_LOG_INFO, "sendDataToJni", "String:%s", className); //log i类型
    std::string jniData1 = "jni get className:";
    //回传数据给java
    jniData1.append(className);
    return env->NewStringUTF(jniData1.c_str());
}extern "C"
JNIEXPORT jstring JNICALL
Java_com_prsioner_jnilearnproject_MainActivity_getDataFromJniBtn(JNIEnv *env, jobject thiz) {

    std::string jniData2 = "data... from jni";
    return env->NewStringUTF(jniData2.c_str());
}extern "C"
JNIEXPORT void JNICALL
Java_com_prsioner_jnilearnproject_MainActivity_sendCallBackCmd(JNIEnv *env, jobject thiz) {

    jclass activityClass = env->GetObjectClass(thiz);
    //通过反射的方式获取到方法Id
    jmethodID idText = env->GetMethodID(activityClass,"jniCallBack","(I)V");
    //int code = 99;
    std::int32_t code1  = 99;
    env->CallVoidMethod(thiz,idText,code1);
    //std::string hello = "callbackCmd";
    //return env->NewStringUTF()
}