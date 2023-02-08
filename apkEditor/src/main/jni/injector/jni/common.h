#ifndef __COMMON_H
#define __COMMON_H

#define REQUEST_INITIALIZE 0
#define REQUEST_SEARCH 1
#define REQUEST_SEARCH_AGAIN 2
#define REQUEST_SET_VALUE 3

// Save x address at most
#define MAX_RESULT_SIZE 1024

// Size for request file
#define REQUEST_SIZE 512

// Size for response file
#define RESPONSE_SIZE 4096

#define REQUEST_FILENAME "appdm_request"
#define RESPONSE_FILENAME "appdm_response"

#define LOG_TAG "DEBUG"  
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
//#define LOGD(format,args...)

extern int g_result_size;
extern void *g_search_result[];

typedef struct _request_hdr {
	// Owner
	int pid;
	
	// Request index
	int index;

	// Request code
	int code;

	// Param Numbers
	int param_num;

	// Params
	int params[1];
} request_hdr;

typedef struct _response_hddr {
	// Response pid
	int pid;

	// Index corresponding the request
	int index;

	// Return value, 0 means succeed
	int ret;

	// Result
	int result_num;
	void *results[1];
} response_hdr;

extern int memctl_init(const char *dirpath, int pid);
extern int memctl_search(int pid, int val, void *result[], int size);

#endif
