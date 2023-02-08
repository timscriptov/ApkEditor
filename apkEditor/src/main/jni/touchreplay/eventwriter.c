/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include <dirent.h>
#include <fcntl.h>
#include <linux/input.h>
#include <poll.h>
#include <errno.h>

#define APPNAME "eventwriter"

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */
static int readableAfterWrite = 0;

/*
struct input_event {
	struct timeval time;
	__u16 type;
	__u16 code;
	__s32 value;
};
*/

static int get_device_name(const char *device, char *name_buf, int buf_len)
{
    int fd;

    fd = open(device, O_RDWR);
    if(fd < 0) {
        return -1;
    }
    
    name_buf[buf_len - 1] = '\0';
    if (ioctl(fd, EVIOCGNAME(buf_len - 1), name_buf) < 1) {
        //fprintf(stderr, "could not get device name for %s, %s\n", device, strerror(errno));
        name_buf[0] = '\0';
    }

    close(fd);

    return 0;
}

// result is an array list
// devname is the device path like /dev/input/event0
static void set_device_info(JNIEnv *env, jobject result, int index, char *devname, char *real_name)
{
    jclass      jArrayClass     = (*env)->GetObjectClass(env, result);  
    jmethodID   mGetID      = (*env)->GetMethodID(env, jArrayClass,"get","(I)Ljava/lang/Object;");  

    jobject jDevInfo = (*env)->CallObjectMethod(env, result, mGetID, index);  

    jstring path = (*env)->NewStringUTF(env, devname);
    jstring name = (*env)->NewStringUTF(env, real_name);

    jclass      jDevInfoClass     = (*env)->GetObjectClass(env, jDevInfo);  
    jfieldID pathFieldID = (*env)->GetFieldID(env, jDevInfoClass, "path", "Ljava/lang/String;");
    (*env)->SetObjectField(env, jDevInfo, pathFieldID, path);
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_touchreplay_MainActivity_listInputDevice
  (JNIEnv *env, jobject thiz, jobject result, int size)
{
    char *dirname = "/dev/input";
    char real_name[80];
    char devname[PATH_MAX];
    char *filename;
    DIR *dir;
    struct dirent *de;
    int index = 0;

    dir = opendir(dirname);
    if (dir == NULL)
        return -1;
    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';
    while((de = readdir(dir))) {
        if(de->d_name[0] == '.' &&
           (de->d_name[1] == '\0' ||
            (de->d_name[1] == '.' && de->d_name[2] == '\0')))
            continue;
        strcpy(filename, de->d_name);
        if (get_device_name(devname, real_name, sizeof(real_name)) == 0) {
            if (index < size) {
                set_device_info(env, result, index, devname, real_name);
                index += 1;
            } else {
                break;
            }
        }
    }
    closedir(dir);
    return index;
}


/*
 * Class:     com_gmail_heagoo_touchreplay_TouchReplayService
 * Method:    openFile
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_gmail_heagoo_touchreplay_TouchReplayService_openFile
  (JNIEnv *env, jobject thiz, jstring inputStr, jint flags)
{
	int f = (flags != 0 ? (O_RDWR | O_SYNC) : O_RDONLY);
	const char *fileName = (const char *)(*env)->GetStringUTFChars( env,inputStr, JNI_FALSE );
	int fd = open(fileName, f);
	//__android_log_print(ANDROID_LOG_INFO, "DEBUG", "open(%s, %d)=%d, O_RDONLY=%d, errno=%d", fileName, f, fd, O_RDONLY, errno);
	return fd;
}


/*
 * Class:     com_gmail_heagoo_touchreplay_TouchReplayService
 * Method:    writeFile
 * Signature: (ILcom/gmail/heagoo/touchreplay/InputEvent;)I
 */
JNIEXPORT jint JNICALL Java_com_gmail_heagoo_touchreplay_TouchReplayService_writeFile
  (JNIEnv *env, jobject thiz, jint fd, jint type, jint code, jint value)
{
    struct input_event event;
    memset(&event, 0, sizeof(event));
    event.type = type;
    event.code = code;
    event.value = value;
    int ret = write(fd, &event, sizeof(event));
    if (ret < sizeof(event)) {
        return -1;
    }    
    return 0;
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_touchreplay_TouchReplayService_writeEvents
  (JNIEnv *env, jobject thiz, jint fd, jobject eventList, jint size)
{
    int i;
    struct input_event event;
    
    jclass      jArrayClass     = (*env)->GetObjectClass(env, eventList);  
    jmethodID   mGetID      = (*env)->GetMethodID(env, jArrayClass,"get","(I)Ljava/lang/Object;");  

    jclass class_event = (*env)->FindClass(env, "com/gmail/heagoo/touchreplay/InputEvent");
    jfieldID type = (*env)->GetFieldID(env, class_event, "type", "I");
    jfieldID code = (*env)->GetFieldID(env, class_event, "code", "I");
    jfieldID value = (*env)->GetFieldID(env, class_event, "value", "I");

    for (i = 0; i < size; i++) {
        // oneEvent = eventList.get(i)
        jobject oneEvent = (*env)->CallObjectMethod(env, eventList, mGetID, i);  
        memset(&event, 0, sizeof(event));
        // event.type = oneEvent.type;
        event.type = (*env)->GetIntField(env, oneEvent, type);
        event.code = (*env)->GetIntField(env, oneEvent, code);
        event.value = (*env)->GetIntField(env, oneEvent, value);
        int ret = write(fd, &event, sizeof(event));
        //__android_log_print(ANDROID_LOG_INFO, "DEBUG", "(%d, %d, %d) = %d", event.type, event.code, event.value, ret);
        if (ret < sizeof(event)) {
            return -1;
        }
    }
    return 0;
}


JNIEXPORT jint JNICALL Java_com_gmail_heagoo_touchreplay_TouchReplayService_readFile
  (JNIEnv *env, jobject thiz, jint fd, jobject result)
{
    struct input_event event;
    int ret = read(fd, &event, sizeof(event));
    if (ret < sizeof(event)) {
        return -1;
    }

    jclass class_event = (*env)->FindClass(env, "com/gmail/heagoo/touchreplay/InputEvent");
    jfieldID time = (*env)->GetFieldID(env, class_event, "time", "F");
    (*env)->SetFloatField(env, result, time, event.time.tv_sec + event.time.tv_usec / 1000000.0f);

    jfieldID type = (*env)->GetFieldID(env, class_event, "type", "I");
    (*env)->SetIntField(env, result, type, event.type);
    jfieldID code = (*env)->GetFieldID(env, class_event, "code", "I");
    (*env)->SetIntField(env, result, code, event.code);
    jfieldID value = (*env)->GetFieldID(env, class_event, "value", "I");
    (*env)->SetIntField(env, result, value, event.value);
    return 0;
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_touchreplay_TouchReplayService_poll
  (JNIEnv *env, jobject thiz, jint fd, jint timeout)
{
    struct pollfd ufds[1];
    int nfds = 1;
    ufds[0].fd = fd;
    ufds[0].events = POLLIN;
    int pollres = poll(ufds, nfds, timeout);
    return pollres;
}

/*
 * Class:     com_gmail_heagoo_touchreplay_TouchReplayService
 * Method:    closeFile
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_gmail_heagoo_touchreplay_TouchReplayService_closeFile
  (JNIEnv *env, jobject thiz, jint fd)
{
    close(fd);
}
