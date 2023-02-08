#include <jni.h>
#include <android/log.h>

#include "common.h"


static void set_memory_info(JNIEnv *env, jobject result, int index, char *addr, char *value)
{
    jclass      jArrayClass     = (*env)->GetObjectClass(env, result);
    jmethodID   mGetID      = (*env)->GetMethodID(env, jArrayClass,"get","(I)Ljava/lang/Object;");

    jobject jMemInfo = (*env)->CallObjectMethod(env, result, mGetID, index);

    jstring strAddr = (*env)->NewStringUTF(env, addr);
    jstring strValue = (*env)->NewStringUTF(env, value);

    jclass      jMemInfoClass     = (*env)->GetObjectClass(env, jMemInfo);
    jfieldID addrFieldID = (*env)->GetFieldID(env, jMemInfoClass, "addr", "Ljava/lang/String;");
    (*env)->SetObjectField(env, jMemInfo, addrFieldID, strAddr);

    jfieldID valueFieldID = (*env)->GetFieldID(env, jMemInfoClass, "value", "Ljava/lang/String;");
    (*env)->SetObjectField(env, jMemInfo, valueFieldID, strValue);
}

// Initialize the memctl
JNIEXPORT jint JNICALL Java_com_gmail_heagoo_appdm_HackMemoryService_memctlInit
  (JNIEnv *env, jobject thiz, jstring dirpath, jint pid)
{
	const char *directory = (const char *)(*env)->GetStringUTFChars(env, dirpath, JNI_FALSE);
	return memctl_init(directory, pid);
}

// Wait for memctl finish
JNIEXPORT jint JNICALL Java_com_gmail_heagoo_appdm_HackMemoryService_memctlInitWait
  (JNIEnv *env, jobject thiz, jint pid)
{
	return memctl_init_wait(pid);
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_appdm_HackMemoryService_memctlSearch32
  (JNIEnv *env, jobject thiz, jint pid, jint val)
{
	// We maximum return 8 addresses
#define ADDR_ARRAY_SIZE 8
	void *address[ADDR_ARRAY_SIZE];
	int matched = memctl_search(pid, val, address, ADDR_ARRAY_SIZE);

	return matched;
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_appdm_HackMemoryService_memctlSearch32Again
  (JNIEnv *env, jobject thiz, jint pid, jint val)
{
	int matched = memctl_search_again(pid, val);

	return matched;
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_appdm_HackMemoryService_memctlSetValue
  (JNIEnv *env, jobject thiz, jint pid, jint val)
{
	return memctl_setvalue(pid, val);
}
