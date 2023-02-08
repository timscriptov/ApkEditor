#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <android/log.h>
#include "common.h"

char request_filepath[256];
char response_filepath[256];
char queue_filepath[256];

// Init a FIFO
static int init_fifo(const char *path, int read_only) {
	int ret = mkfifo(path, 0777);
	if (ret != 0) {
		LOGD("Can not create FIFO %s", path);
		return -1;
	}

	int flag = (read_only ? O_RDONLY : O_RDWR);
	int fd = open(path, flag);
	return fd;
}

int main(int argc, char **argv) {
	if (argc != 2) {
		printf("Usage: %s working_dir\n", argv[0]);
		return -1;
	}

	char *a = argv[1];
	snprintf(request_filepath, sizeof(request_filepath), "%s/%s", a, REQUEST_FILENAME);
	snprintf(response_filepath, sizeof(response_filepath), "%s/%s", a, RESPONSE_FILENAME);
	snprintf(queue_filepath, sizeof(queue_filepath), "%s/%s", a, EVENT_QUEUE_NAME);

	// Init the request FIFO
	int request_fd = init_fifo(request_filepath, 0);
	if (request_fd < 0) {
		LOGD("Can not open %s", request_filepath);
		return -1;
	}

	// Init the response FIFO
	int response_fd = init_fifo(response_filepath, 1);
	if (response_fd < 0) {
		LOGD("Can not open %s", response_filepath);
		return -1;
	}

	// Init event queue
	int event_queue_fd = init_fifo(queue_filepath, 1);
	if (event_queue_fd < 0) {
		LOGD("Can not open %s", queue_filepath);
		return -1;
	}

	// Wait for server ready
	char buf[256];
	int read_bytes = read(response_fd, buf, 1);
	printf("read_bytes = %d\n", read_bytes);
	return 0;
}
