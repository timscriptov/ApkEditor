#include <sys/types.h>
#include <unistd.h>
#include <android/log.h>

#include <stdio.h>
#include "common.h"


void search_one_section(long start_addr, long end_addr, unsigned int *pvalue)
{
	unsigned int *addr = (unsigned int *)start_addr;
	while (addr <(unsigned int *) end_addr) {
		// Find one value
		if (*addr == *pvalue && addr != pvalue) {
			if (g_result_size < MAX_RESULT_SIZE) {
				g_search_result[g_result_size] = (void *)addr;
			}
			g_result_size += 1;
			//LOGD("Found, addr=%p\n", addr);
		}
		addr += 1;
	}
}

void setvalue_int(unsigned int val)
{
	int i;
	int size = (g_result_size < MAX_RESULT_SIZE ? g_result_size : MAX_RESULT_SIZE);
	for (i = 0; i < size; i++) {
		LOGD("Set addr %p to value of %d.\n", g_search_result[i], val);
		*((int *)g_search_result[i]) = val;
	}
}

void search_int(unsigned int *pvalue)
{
	FILE *fp;
	long start_addr, end_addr;
	char *pch;
	char filename[32];
	char line[1024];
	int i;

	pid_t pid = getpid();
	snprintf( filename, sizeof(filename), "/proc/%d/maps", pid );

	fp = fopen( filename, "r" );

	if ( fp != NULL )
	{
		while ( fgets( line, sizeof(line), fp ) )
		{
			// Only search private memory for read/write access
			// And not contain '/'
			if ( strstr( line, "rw-p" ) && !strchr(line, '/') )
			{
				pch = strchr( line, '-' );
				if (pch == NULL) { continue; }
				*pch = '\0';
				pch += 1;
				for (i = 0; i < 20; i++) {
					char c = *(pch + i);
					if (c >= '0' && c <= '9') continue;
					if (c >= 'a' && c <= 'f') continue;
					if (c >= 'A' && c <= 'F') continue;
					*(pch + i) = '\0';
					break;
				}
				start_addr = strtoul( line, NULL, 16 );
				end_addr = strtoul( pch, NULL, 16 );

				search_one_section(start_addr, end_addr, pvalue);

				//if ( addr == 0x8000 )
				//	addr = 0;
			}
		}

		fclose( fp ) ;
	}
}

void search_int_again(unsigned int *pvalue)
{
	int i, matched = 0;
	int size = (g_result_size < MAX_RESULT_SIZE ? g_result_size : MAX_RESULT_SIZE);
	void **matched_addr = (void **)malloc(size * sizeof(void *));

	for (i = 0; i < size; i++) {
		if (*((unsigned int *)g_search_result[i]) == *pvalue) {
			matched_addr[matched++] = g_search_result[i];
		}
	}

	// Copy result back
	g_result_size = matched;
	for (i = 0; i < matched; i++) {
		g_search_result[i] = matched_addr[i];
	}

	free(matched_addr);
}
