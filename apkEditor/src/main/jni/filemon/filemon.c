#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <stdio.h>  
#include <stdlib.h>  
#include <errno.h>  
#include <dirent.h> 
#include <unistd.h>  
#include <sys/inotify.h>
  
typedef struct _Node {
	int wd;

	//short level;
	//short isDir;

	// For directory, it is the total block count	
	//unsigned int blockCount;

	//time_t lastAccess;

	// The offset for parent node
	//int parentOffset;
	// Offset for first child
	//int firstChildOffset;
	// Offset for neighbour brother
	//int nextBrotherOffset;

	// The pathLen include the end '\0'
	int pathLen;
	char *path;
} Node;

#define MAX_NODE_SIZE 8192
static Node g_nodes[MAX_NODE_SIZE];
static int g_nodeSize = 0; 
 
// inotify file descriptor
static int fd = 0;
static int event_id = 0;

// is_running = 1 when startMon called
static int is_running = 0;

// Root mode or not
static int is_rootMode = 0;

const char * event_array[] = {  
    "File was accessed",  
    "File was modified",  
    "File attributes were changed",  
    "writtable file closed",  
    "Unwrittable file closed",  
    "File was opened",  
    "File was moved from X",  
    "File was moved to Y",  
    "Subfile was created",  
    "Subfile was deleted",  
    "Self was deleted",  
    "Self was moved",  
    "",  
    "Backing fs was unmounted",  
    "Event queued overflowed",  
    "File was ignored"  
};

#define EVENT_NUM 16  
#define MAX_BUF_SIZE 1024  

static Node *getNextNode(Node *p)
{
	Node *next = (Node *)((char *)p + sizeof(Node));
	if (next < &g_nodes[g_nodeSize]) {
		return next;
	}

	return NULL;
}

// Allocate a free node in the tail, and update g_tail_node
static Node *allocateNode()
{
	if (g_nodeSize < MAX_NODE_SIZE) {
		return &g_nodes[g_nodeSize++];
	}

	return NULL;
}

static Node *findNodeByWd(int wd)
{
	Node *node = NULL;

	if (g_nodeSize > 0) {
		int offset_tail = wd - g_nodes[g_nodeSize - 1].wd;
		int offset = g_nodeSize - 1 - offset_tail;

		if (g_nodes[offset].wd == wd) {
			node = &g_nodes[offset];
		}
		else if (g_nodes[offset].wd > wd) {
			int i = offset + 1;
			for (; i < g_nodeSize; i++) {
				if (g_nodes[i].wd == wd) {
					node = &g_nodes[i];
					break;
				}
			}
		}
		else if (g_nodes[offset].wd < wd) {
			int i = offset - 1;
			for (; i >= 0; i--) {
				if (g_nodes[i].wd == wd) {
					node = &g_nodes[i];
					break;
				}
			}
		}
	}

	return node;
}

// parent: parent node
// child: child node
// ent: child entry
static void initChildNode(Node *parent, Node *child, struct dirent *ent)
{
	child->pathLen = parent->pathLen + 1 + strlen(ent->d_name);
	child->path = (char *)malloc(child->pathLen);

	memcpy(child->path, parent->path, parent->pathLen);
	child->path[parent->pathLen - 1] = '/';
	strcpy(child->path + parent->pathLen, ent->d_name);
}

static int expandChildNode(Node *p)
{
	char *path = p->path;
	DIR *pDir;
	struct dirent *ent;
	int ret = 0;

	//printf("%s\n", path);

	pDir = opendir(path);
	if (pDir == NULL) {
		return -1;
	}

	while ((ent = readdir(pDir)) != NULL) {
		// Only interested for directory
		if (ent->d_type & DT_DIR) {
			if (strcmp(ent->d_name,".") == 0 || strcmp(ent->d_name, "..") == 0)
				continue;

			Node *child = allocateNode();
			if (child == NULL) {
				ret = -1;
				break;
			}
			
			initChildNode(p, child, ent);
		}
	}

	closedir(pDir);
	
	return ret;
}

// Return: 0 - succeed; -1, error; 1 - partly succeed
int startFileMon(const char *dirPath)
{
	event_id = 1;
	if ((fd = inotify_init()) < 0) {
		__android_log_print(ANDROID_LOG_INFO, "DEBUG", "ERROR: Fail to initialize inotify.");
		return -1;
	}

	Node *root = allocateNode();
	root->pathLen = strlen(dirPath) + 1;
	root->path = (char *)malloc(root->pathLen);
	strcpy(root->path, dirPath);

	// expand all sub-directory
	Node *curNode = root;
	while (curNode != NULL) {
		expandChildNode(curNode);
		curNode = getNextNode(curNode);
	}

	// Add to notify
	int wd, i;
	for (i = g_nodeSize - 1; i >= 0; i--) {
		curNode = &g_nodes[i];
		if ((wd = inotify_add_watch(fd, curNode->path, IN_ALL_EVENTS)) >= 0) {
			curNode->wd = wd;
		} else {
			__android_log_print(ANDROID_LOG_INFO, "DEBUG", "ERROR: cannot add watch for %s\n", curNode->path);
		}
	}

	return 0;
}

static jclass fileop_class;
static jfieldID idFieldId;
static jfieldID timeFieldId;
static jfieldID opCodeFieldId;
static jfieldID filePathFieldId;

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_filemon_FilemonService_startFilemon
  (JNIEnv *env, jobject thiz, jstring inputStr)
{
	const char *path = (const char *)(*env)->GetStringUTFChars(env, inputStr, JNI_FALSE);
	int ret = startFileMon(path);
	if (ret == 0) {
		is_running = 1;
	}
	//__android_log_print(ANDROID_LOG_INFO, "DEBUG", "open(%s, %d)=%d, O_RDONLY=%d, errno=%d", fileName, f, fd, O_RDONLY, errno);
	fileop_class = (*env)->FindClass(env, "com/gmail/heagoo/filemon/types/FileOperation");
	idFieldId = (*env)->GetFieldID(env, fileop_class, "id", "I");
	timeFieldId = (*env)->GetFieldID(env, fileop_class, "time", "I");
	opCodeFieldId = (*env)->GetFieldID(env, fileop_class, "opCode", "I");
	filePathFieldId = (*env)->GetFieldID(env, fileop_class, "filePath", "Ljava/lang/String;");
	return ret;
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_filemon_FilemonService_stopFilemon
  (JNIEnv *env, jobject thiz)
{
	int i;

	is_running = 0;

	for (i = 0; i < g_nodeSize; i++) {
		inotify_rm_watch(fd, g_nodes[i].wd);
		free(g_nodes[i].path);
		g_nodes[i].path = NULL;
	}

	g_nodeSize = 0;

	close(fd);
}

static void setRecordValue
  (JNIEnv *env, jobject record, int id, int seconds, struct inotify_event *event)
{
    (*env)->SetIntField(env, record, idFieldId, id);
    (*env)->SetIntField(env, record, timeFieldId, seconds);
    (*env)->SetIntField(env, record, opCodeFieldId, event->mask);

    Node *watchNode = findNodeByWd(event->wd);
    if (watchNode == NULL) { // This is an error
__android_log_print(ANDROID_LOG_INFO, "DEBUG", "ERROR: cannot find watch node");
        return;
    }

    if (event->len == 0) {
        (*env)->SetObjectField(env, record, filePathFieldId, (*env)->NewStringUTF(env, watchNode->path));
    } else {
        int pathLen = strlen(watchNode->path);
        char *buf = (char *)malloc(pathLen + event->len + 2);
        memcpy(buf, watchNode->path, pathLen);
        buf[pathLen] = '/';
        memcpy(buf + pathLen + 1, event->name, event->len);
        buf[pathLen + event->len + 1] = '\0';
        (*env)->SetObjectField(env, record, filePathFieldId, (*env)->NewStringUTF(env, buf));
        free(buf);
    }
}

struct inotify_event *getNextEvent()
{
	static char buffer[MAX_BUF_SIZE + 1];
	static char *offset = NULL;
	static int readLen = 0;
	struct inotify_event *event;

	// Some remaining event in the buffer
	if (offset != NULL && (offset - buffer) < readLen) {
		event = (struct inotify_event *)offset;

		int tmp_len = sizeof(struct inotify_event) + event->len;
		offset += tmp_len;
	}

	else if ((readLen = read(fd, buffer, MAX_BUF_SIZE)) > 0) {
		event = (struct inotify_event *)buffer;

		int tmp_len = sizeof(struct inotify_event) + event->len;
		offset = buffer + tmp_len;
	}
	
	else {
		offset = NULL;
		readLen = 0;
		event = NULL;
	}

	return event;
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_filemon_FilemonService_readEvent
  (JNIEnv *env, jobject thiz, jobject result)
{
	struct inotify_event *event =  NULL;

start:
	event = getNextEvent();

	// Monitor is not running any more
	if (!is_running) {
		return -2;
	}

	// Read error
	if (event == NULL) {
		int lastErr = errno;
		if (lastErr == EINTR || lastErr == EWOULDBLOCK || lastErr == EAGAIN) {
			goto start;
		} else {
			return -1;
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

	int id = event_id++;
	int readTime = (int)(time(NULL) % 86400);
	setRecordValue(env, result, id, readTime, event);

	return 0;
}

JNIEXPORT jint JNICALL Java_com_gmail_heagoo_filemon_FilemonService_readEvents
  (JNIEnv *env, jobject thiz, jobject eventList, jint size)
{
    static jclass      jArrayClass;
    static jmethodID   mGetID;
    static int inited = 0;
    if (!inited) {
        jArrayClass     = (*env)->GetObjectClass(env, eventList);  
        mGetID      = (*env)->GetMethodID(env, jArrayClass,"get","(I)Ljava/lang/Object;");  
        inited = 1;
    } 

	// Read inotify event
	char buffer[MAX_BUF_SIZE + 1];
	char *offset = NULL;
	struct inotify_event *event;
	int len, index = 0;

	if (len = read(fd, buffer, MAX_BUF_SIZE)) {
		int seconds = (int)(time(NULL) % 86400);
		offset = buffer;
		event = (struct inotify_event *)buffer;
		while (((char *)event - buffer) < len) {
			// oneEvent = eventList.get(i)
			jobject oneEvent = (*env)->CallObjectMethod(env, eventList, mGetID, index++);  

			int id = event_id++;
			setRecordValue(env, oneEvent, id, seconds, event);

			int tmp_len = sizeof(struct inotify_event) + event->len;
			offset += tmp_len;
			event = (struct inotify_event *)(offset);
		}
	}

	return index;
}

int main_test(int argc,char *argv[])
{
	if (argc < 2) {
		printf("Usage: %s path\n", argv[0]);
		exit(-1);
	}

	startFileMon(argv[1]);

	// Read inotify event
	char buffer[MAX_BUF_SIZE + 1];
	char *offset = NULL;
	struct inotify_event *event;
	int len, i;

	while (len = read(fd, buffer, MAX_BUF_SIZE)) {
		offset = buffer;
		event = (struct inotify_event *)buffer;
		while (((char *)event - buffer) < len) {
			printf("Object type: %s\n",
				event->mask & IN_ISDIR ? "Direcotory" : "File");
			printf("Object name: %s\n", event->name);
			printf("Event mask: %08X\n", event->mask);
			for (i = 0; i < EVENT_NUM; i++) {
				if (event_array[i][0] == '\0')
					continue;
				if (event->mask & (1<<i)) {
					printf("Event: %s\n", event_array[i]);
				}
			} // end for
			int tmp_len = sizeof(struct inotify_event) + event->len;
			offset += tmp_len;
			event = (struct inotify_event *)(offset);
		}
	}

	return 0;
}
