#ifndef _COMMON_H
#define _COMMON_H

#include <sys/inotify.h>

#define LOG_TAG "DEBUG"  
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
//#define LOGD(format,args...)

#define REQUEST_FILENAME "q"
#define RESPONSE_FILENAME "a"
#define EVENT_QUEUE_NAME "eq"

int startFileMon(const char *dirPath);
struct inotify_event *getNextEvent();

#endif
