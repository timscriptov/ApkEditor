#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <pthread.h>
#include <android/log.h>  
      
#include <stdio.h>  
#include <stdlib.h>  

#include "common.h"

static int g_already_injected = 0;

int g_result_size = 0;
void *g_search_result[MAX_RESULT_SIZE];

char request_filepath[128];
char response_filepath[128];

extern void search_int(unsigned int *pvalue);
extern void setvalue_int(unsigned int val);

// Response to the request
void do_request(request_hdr *req, response_hdr *res) {
	int i;
	switch (req->code) {
	case REQUEST_SEARCH:
		g_result_size = 0;
		search_int(&req->params[0]);

		// Return the search result
		res->ret = 0;
		res->result_num = g_result_size;
		int saved_size = (g_result_size < MAX_RESULT_SIZE ? g_result_size : MAX_RESULT_SIZE);
		for (i = 0; i < saved_size; i++) {
			res->results[i] = g_search_result[i];
		}
		res->index = req->index;
		res->pid = req->pid;
		break;
	case REQUEST_SEARCH_AGAIN:
		search_int_again(&req->params[0]);
		// Return (Note: The address is not returned)
		res->ret = 0;
		res->result_num = g_result_size;
		res->index = req->index;
		res->pid = req->pid;
		break;
	case REQUEST_SET_VALUE:
		setvalue_int((int)req->params[0]);
		// Return
		res->ret = 0;
		res->index = req->index;
		res->pid = req->pid;
		break;
	default:
		LOGD("Unknown request code: %d", req->code);
		break;
	}
}

void *service_thread(void *param) {
	// Init shared memory for request
	int rfd = open(request_filepath, O_RDONLY);
	if (rfd < 0) {
		LOGD("Can not open request file.");
		return NULL;
	}

	int read_len = 512;
	void *read_ptr = mmap(NULL, read_len, PROT_READ, MAP_SHARED, rfd, 0);
	if (read_ptr == NULL) {
		close(rfd);
		LOGD("mmap error.");
		return NULL;
	}

	// Init shared memory for response
	int wfd = open(response_filepath, O_RDWR);
	if (wfd < 0) {
		munmap(read_ptr, read_len);
		close(rfd);
		LOGD("Can not open reponse file.");
		return NULL;
	}

	int write_len = 4096;
	void *write_ptr = mmap(NULL, write_len, PROT_READ | PROT_WRITE, MAP_SHARED, wfd, 0);
	if (write_ptr == NULL) {
		close(wfd);
		munmap(read_ptr, read_len);
                close(rfd);
		LOGD("Can not map reponse file.");
                return NULL;
	}

	// Do Task
	uid_t my_pid = getpid();
	int last_request = -1;
	request_hdr *req = (request_hdr *)read_ptr;
	response_hdr *res = (response_hdr *)write_ptr;
	while (1) {
		if (req->pid == my_pid) {
			if (req->index == 0) {
				//LOGD("Do the initialization.");
				res->pid = my_pid;
				res->ret = 0;
				last_request = 0;
			}
			else if (req->index != last_request) {
				LOGD("request detected, index = %d.", req->index);
				do_request(req, res);
				last_request = req->index;
			}
		}
		usleep(20 * 1000);
	}
	return NULL;
}

int so_entry(char * a) {
	LOGD("so_entry, pid = %d\n", getpid());
	//search_int(0x52ab1204);
	if (g_already_injected) {
		LOGD("Already injected.\n");
	} else {
		snprintf(request_filepath, sizeof(request_filepath), "%s/%s", a, REQUEST_FILENAME);
		snprintf(response_filepath, sizeof(response_filepath), "%s/%s", a, RESPONSE_FILENAME);

		pthread_t tid;
		pthread_create(&tid, NULL, service_thread, NULL);
		g_already_injected = 1;
	}
	return 0;  
}

