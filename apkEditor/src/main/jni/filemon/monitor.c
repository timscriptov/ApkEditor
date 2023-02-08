#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <pthread.h>
#include <android/log.h>
#include <errno.h>
#include <stdio.h>

#include "common.h"

char request_filepath[256];
char response_filepath[256];
char queue_filepath[256];

// Return NULL mean failed
// NOT USED any more
static void *init_queue(const char *path, int size, int writable) {
	// Init shared memory for response
	int flag = (writable ? O_RDWR : O_RDONLY);
	int fd = open(path, flag);
	if (fd < 0) {
		LOGD("Can not open %s", path);
		return NULL;
	}

	flag = (writable ? (PROT_READ | PROT_WRITE) : PROT_READ);
	void *ptr = mmap(NULL, size, flag, MAP_SHARED, fd, 0);
	if (ptr == NULL) {
                close(fd);
		LOGD("Can not map %s", path);
                return NULL;
	}

	return ptr;
}

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

void *response_request(int request_fd, int response_fd) {
	char buf[512];

	// To indicate that I'm ready, send a zero
	buf[0] = 0;
	write(response_fd, buf, 1);

	while (1) {
		int read_bytes = read(request_fd, buf, 1);
		if (read_bytes < 0) {
			break;
		} else if (read_bytes == 0) {
			continue;
		}

		int is_stop_command = 0;
		switch (buf[0]) {
		case 'a': // Add a directory
			break;
		case 's': // Stop
			is_stop_command = 1;
			break;
		}

		if (is_stop_command) {
			break;
		}
	}
	return NULL;
}

// Looply read the file operation events
void *read_thread(void *param) {

	int pipe_id = *((int *)param);

	struct inotify_event *event =  NULL;

start:
	event = getNextEvent();

	// Read error
	if (event == NULL) {
		int lastErr = errno;
		if (lastErr == EINTR || lastErr == EWOULDBLOCK || lastErr == EAGAIN) {
			goto start;
		} else {
			return NULL;
		}
	}

	// Directory event observed by parent, omit it
	if ((event->mask & IN_ISDIR) && event->len > 0) {
		goto start;
	}

	// Ignored
	if (event->mask & IN_IGNORED) {
		goto start;
	}

	write(pipe_id, event, sizeof(struct inotify_event) + event->len);

	goto start;

	return NULL;
}

int main(int argc, char **argv) {
	if (argc != 3) {
		printf("Usage: %s working_dir watch_dir\n", argv[0]);
		return -1;
	}

	char *a = argv[1];
	snprintf(request_filepath, sizeof(request_filepath), "%s/%s", a, REQUEST_FILENAME);
	snprintf(response_filepath, sizeof(response_filepath), "%s/%s", a, RESPONSE_FILENAME);
	snprintf(queue_filepath, sizeof(queue_filepath), "%s/%s", a, EVENT_QUEUE_NAME);

	// Init the request FIFO
	int request_fd = init_fifo(request_filepath, 1);
	if (request_fd < 0) {
		LOGD("Can not open %s", request_filepath);
		return -1;
	}

	// Init the response FIFO
	int response_fd = init_fifo(response_filepath, 0);
	if (response_fd < 0) {
		LOGD("Can not open %s", response_filepath);
		return -1;
	}

	// Init event queue
	int event_queue_fd = init_fifo(queue_filepath, 0);
	if (event_queue_fd < 0) {
		LOGD("Can not open %s", queue_filepath);
		return -1;
	}

	startFileMon(argv[2]);

	// Prepare the reading thread
	pthread_t tid;
	pthread_create(&tid, NULL, read_thread, (void *)&event_queue_fd);

	// Wait for request, and then reply
	response_request(request_fd, response_fd);

	return 0;  
}

