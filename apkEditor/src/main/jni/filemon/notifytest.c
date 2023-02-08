#include <stdio.h>  
#include <stdlib.h>  
#include <string.h>  
#include <linux/unistd.h>  
#include <linux/inotify.h>  
  
char * event_array[] = {  
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
  
int main(int argc, char *argv[] )  
{  
    int fd, wd;  
    char buffer[ MAX_BUF_SIZE + 1 ];  
    char * offset = NULL;  
    struct inotify_event * event;  
    int i, len, tmp_len;  
    char strbuf[16];  
  
    if( argc != 2 )  
    {  
        printf( "%s file|folder\n", argv[0] );  
        exit( 0 );  
    }  
    if(( fd = inotify_init()) < 0 )  
    {  
        printf("Fail to initialize inotify.\n");  
        exit( 0 );  
    }  
    if(( wd = inotify_add_watch(fd, argv[1], IN_ALL_EVENTS)) < 0 )  
    {  
            printf("Can't add watch for %s.\n", argv[1]);  
            exit(0);  
    }  
    while( len = read(fd, buffer, MAX_BUF_SIZE))   
    {  
        offset = buffer;  
        event = (struct inotify_event *)buffer;  
        while(((char *)event - buffer) < len )   
        {  
            printf( "Object type: %s\n",   
                event->mask & IN_ISDIR ? "Direcotory" : "File" );  
            if(event->wd != wd)   
                continue;  
            printf("Object name: %s\n", event->name);  
            printf("Event mask: %08X\n", event->mask);  
            for(i=0; i<EVENT_NUM; i++)   
            {  
                if (event_array[i][0] == '\0')   
                    continue;  
                if (event->mask & (1<<i))   
                {  
                    printf("Event: %s\n", event_array[i]);  
                }  
            }  
            tmp_len = sizeof(struct inotify_event) + event->len;  
            event = (struct inotify_event *)(offset + tmp_len);   
            offset += tmp_len;  
        }  
    }  
}
