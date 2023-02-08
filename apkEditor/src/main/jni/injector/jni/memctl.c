#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <android/log.h>
#include <stdio.h>
#include "common.h"

static request_hdr *req = NULL;
static response_hdr *res = NULL;
static int request_index = 1; // start from 1

int memctl_init(const char *dirpath, int pid)
{
	char filepath[128];
	// Initialize the response file
	{
		snprintf(filepath, sizeof(filepath), "%s/%s", dirpath, RESPONSE_FILENAME);
        	int fd = open(filepath, O_RDONLY);
	        if (fd < 0) {
        	        LOGD("Can not open response file.");
                	return -1;
	        }

        	int len = RESPONSE_SIZE;
	        void *ptr = mmap(NULL, len, PROT_READ, MAP_SHARED, fd, 0);
        	if (ptr == NULL) {
			LOGD("mmap response error.");
			return -1;
		}
		
		res = (response_hdr *)ptr;
	}

	// Initialize the control file
	{
		snprintf(filepath, sizeof(filepath), "%s/%s", dirpath, REQUEST_FILENAME);
        	int fd = open(filepath, O_RDWR | O_CREAT);
	        if (fd < 0) {
        	        LOGD("Can not open request file.");
                	return -1;
	        }

        	int len = REQUEST_SIZE;
	        void *ptr = mmap(NULL, len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
        	if (ptr == NULL) {
			LOGD("mmap request error.");
			return -1;
		}
		
		req = (request_hdr *)ptr;

		req->pid = pid;
		req->index = 0;
		req->code = 0;
		req->param_num = 0;
	}

	return 0;
}

// Wait for injection finish
int memctl_init_wait(int pid)
{
	int milli_seconds = 0;
	int ret = -1;

	while (milli_seconds < 5000) {
		if (res->pid == pid) {
			ret = res->ret;
			break;
		}
		usleep(20 * 1000);
		milli_seconds += 20;
	}

	return ret;
}

// Search a value in the target pid
// Save the address to result
// Return how many addresses matches the target value
// Return -1 if timeout
int memctl_search(int pid, int val, void *result[], int size)
{
	int i = 0;
	int milli_seconds = 0;
	int ret = -1;

	req->code = REQUEST_SEARCH;
	req->pid = pid;
	req->params[0] = val;
	req->index = request_index;

	while (milli_seconds < 8000) {
		if (res->pid == pid && res->index == request_index) {
			int recorded_size = res->result_num > MAX_RESULT_SIZE ? MAX_RESULT_SIZE : res->result_num;
			if (recorded_size < size) { size = recorded_size; }
			for (i = 0; i < size; i++) {
				result[i] = res->results[i];
				LOGD("Address: %p\n", res->results[i]);
			}
			// Return matched address NUM
			ret = res->result_num;
			break;
		}
		usleep(20 * 1000);
		milli_seconds += 20;
	}

	LOGD("res->pid=%d, res->index=%d\n", res->pid, res->index);

	request_index += 1;
	return ret;
}

// Search address again (second search)
// The result is not returned
int memctl_search_again(int pid, int val)
{
	int i = 0;
	int milli_seconds = 0;
	int ret = -1;

	req->code = REQUEST_SEARCH_AGAIN;
	req->pid = pid;
	req->params[0] = val;
	req->index = request_index;

	while (milli_seconds < 5000) {
		if (res->pid == pid && res->index == request_index) {
			// Return matched address NUM
			ret = res->result_num;
			break;
		}
		usleep(20 * 1000);
		milli_seconds += 20;
	}

	LOGD("res->pid=%d, res->index=%d\n", res->pid, res->index);

	request_index += 1;
	return ret;
}

// Set values for searched address
int memctl_setvalue(int pid, int val)
{
	int i = 0;
	int milli_seconds = 0;
	int ret = -1;

	req->code = REQUEST_SET_VALUE;
	req->pid = pid;
	req->params[0] = val;
	req->index = request_index;

	while (milli_seconds < 8000) {
		if (res->pid == pid && res->index == request_index) {
			ret = res->ret;
			break;
		}
		usleep(20 * 1000);
		milli_seconds += 20;
	}

	request_index += 1;
	return ret;
}

int __main(int argc, char **argv)
{
	int i;
	char buf[64] = { '\0' };



	int index = 1;
	int addr, val, func_code;
	while (1) {
		printf("Please select a function\n[1-search, 2-second search, 3-setvalue]: ");
		scanf("%d", &func_code);
		switch (func_code) {
		case 1:
			printf("Please input a value to search: ");
			scanf("%d", &val);
			if (val == -1) break;

			req->code = REQUEST_SEARCH;
			req->params[0] = val;
			req->index = index;

			//wait_for_result(res, index);
			break;
		case 2:
			break;
		case REQUEST_SET_VALUE:
			printf("Please input an address: ");
			scanf("%d", &addr);
			printf("Please input the new value: ");
			scanf("%d", &val);

			req->code = func_code;
			req->param_num = 2;
			req->params[0] = addr;
			req->params[1] = val;
			req->index = index;
			break;
		default:
			break;
		}

		index += 1;
	}
	
	return 0;
}
