#include <jni.h>
#include <android/log.h>

#include "common.h"

int g_result_size = 0;
void *g_search_result[MAX_RESULT_SIZE];

extern void setvalue_int(unsigned int val);
extern void search_int(unsigned int *pvalue);

JNIEXPORT jint JNICALL Java_apkeditor_patch_memeditor_HackMemoryService_memctlSearch32
  (JNIEnv *env, jobject thiz, jint val)
{
	g_result_size = 0;
	search_int((unsigned int *)&val);
	
        return g_result_size;
}

// Search again in the result set
JNIEXPORT jint JNICALL Java_apkeditor_patch_memeditor_HackMemoryService_memctlSearch32Again
  (JNIEnv *env, jobject thiz, jint val)
{
	search_int_again((unsigned int *)&val);
        return g_result_size;
}

JNIEXPORT jint JNICALL Java_apkeditor_patch_memeditor_HackMemoryService_memctlSetValue
  (JNIEnv *env, jobject thiz, jint val)
{
	setvalue_int((unsigned int)val);
	return 0;
}
