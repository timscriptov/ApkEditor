#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "util.h"

void map_init(struct Map *pmap) {
    pmap->size = 0;
    pmap->capacity = MAP_INIT_SIZE;
    pmap->keys = (const char **)malloc(sizeof(char *) * MAP_INIT_SIZE);
    pmap->values = (const char **)malloc(sizeof(char *) * MAP_INIT_SIZE);
    pmap->hashes = (unsigned long *)malloc(sizeof(long) * MAP_INIT_SIZE);
}
void map_cleanup(struct Map *pmap) {
    pmap->size = 0;
    pmap->capacity = 0;
    free(pmap->keys);
    free(pmap->values);
    free(pmap->hashes);
}
static unsigned long compute_hash(const char *str) {
    unsigned long hash = 5381;
    int c;
    while (c = *str++)
        hash = ((hash << 5) + hash) + c; /* hash * 33 + c */
    return hash;
}
void map_put(struct Map *pmap, const char *key, const char *value) {
    unsigned long h = compute_hash(key);
    if (pmap->size >= pmap->capacity) { // enlarge it
        pmap->capacity *= 2;
        const char **keys = (const char **)malloc(sizeof(char *) * pmap->capacity);
        const char **values = (const char **)malloc(sizeof(char *) * pmap->capacity);
        unsigned long *hashes = (unsigned long *)malloc(sizeof(long) * pmap->capacity);
        memcpy(keys, pmap->keys, sizeof(char *) * pmap->size);
        memcpy(values, pmap->values, sizeof(char *) * pmap->size);
        memcpy(hashes, pmap->hashes, sizeof(long) * pmap->size);
        free(pmap->keys);
        free(pmap->values);
        free(pmap->hashes);
        pmap->keys = keys;
        pmap->values = values;
        pmap->hashes = hashes;
    }
    pmap->keys[pmap->size] = key;
    pmap->values[pmap->size] = value;
    pmap->hashes[pmap->size] = h;
    pmap->size += 1;

// DEBUG
int len = strlen(value);
if ((len <= 4) || (value[len - 4] != '.') || (value[len - 3] != 'm') || (value[len - 2] != 'p')) {
return;
}
{  
  int tmpIdx = 0, dd = 0;
  char *tmp = (char *)malloc(len * 3 + 16);
  memset(tmp, 0, len * 3 + 16);
  for (dd = 0; dd < len; ++dd) {
    sprintf(tmp + tmpIdx, "%02x ", value[dd]);
    tmpIdx += 3;
  }
  LOGD("map.add, value.len=%d, %s", len, tmp);
  free(tmp);
}

}
const char *map_get(struct Map *pmap, const char *key) {
    if (pmap == NULL) {
        return NULL;
    } else {
        int i;
        unsigned long h = compute_hash(key);
        for (i = 0; i < pmap->size; ++i) {
            if ((pmap->hashes[i] == h) && strcmp(key, pmap->keys[i]) == 0) {
                return pmap->values[i];
            }
        }
    }
    return NULL;
}

// Parse the content to pmap
void parse_map(char *buf, int len, struct Map *pmap) {
    int i;
    const char *str_key;
    const char *str_value;

    if (buf == NULL) {
        return;
    }

    // Parse the content
    str_key = buf;
    str_value = NULL;
    for (i = 0; i < len; ++i) {
        if (buf[i] == '\0' || buf[i] == '\n') {
            buf[i] = '\0';
            if (str_key != NULL && str_value != NULL) {
                map_put(pmap, str_key, str_value);
                str_key = buf + i + 1;
                str_value = NULL;
            } else {
                str_value = buf + i + 1;
            }
        }
    }
    if (str_key != NULL && str_value != NULL) {
        map_put(pmap, str_key, str_value);
    }
}

int map_contains_value(struct Map *pmap, const char *value)
{
    if (pmap != NULL) {
        int i;
        for (i = 0; i < pmap->size; ++i) {
            if (strcmp(value, pmap->values[i]) == 0) {
                return 1;
            }
        }
    }
    return 0;
}

////////////////////////////////////////////////////////////////////////////////

#define INIT_SIZE 64
void list_init(struct StringList *plist)
{
    plist->size = 0;
    plist->capacity = INIT_SIZE;
    plist->strs = (const char **)malloc(INIT_SIZE * sizeof(char *));
    plist->lens = (int *)malloc(INIT_SIZE * sizeof(int));
}

void list_cleanup(struct StringList *plist)
{
    free(plist->strs);
    free(plist->lens);
    plist->strs = NULL;
    plist->lens = NULL;
    plist->size = 0;
    plist->capacity = 0;
}

void list_add(struct StringList *plist, const char *str, int len)
{
    if (plist->size >= plist->capacity) {
        const char **strs = (const char **)malloc(plist->capacity * 2 * sizeof(char *));
        int *lens = (int *)malloc(plist->capacity * 2 * sizeof(int));
        memcpy(strs, plist->strs, plist->size * sizeof(char *));
        memcpy(lens, plist->lens, plist->size * sizeof(int));
        free(plist->strs);
        free(plist->lens);
        plist->capacity *= 2;
        plist->strs = strs;
        plist->lens = lens;
    }
    plist->strs[plist->size] = str;
    plist->lens[plist->size] = (len < 0 ? strlen(str) : len);
    plist->size += 1;
}

int list_contains(struct StringList *plist, const char *str)
{
    int i;
    int len = strlen(str);
    for (i = 0; i < plist->size; ++i) {
        if ((plist->lens[i] == len) && (strcmp(plist->strs[i], str) == 0)) {
            return 1;
        }
    }
    return 0;
}

const char *list_get(struct StringList *plist, int index)
{
    if (index < plist->size) {
        return plist->strs[index];
    }
    return NULL;
}

void parse_list(char *str, int len, struct StringList *plist)
{
    int i = 0;
    int start = 0;
    for (; i < len; ++i) {
        if (str[i] == '\0' || str[i] == '\n') {
            str[i] = '\0';
            list_add(plist, str + start, i - start);
            start = i + 1;
        }
    }
    if (start < len) {
        list_add(plist, str + start, i - start);
    }
}

/*
int main()
{
    struct Map map;
    struct StringList list;
    list_init(&list);
    list_add(&list, "test123", -1);
    list_add(&list, "test", 4);
    printf("contains: %d\n", list_contains(&list, "test"));
    printf("contains: %d\n", list_contains(&list, "test123"));
    printf("contains: %d\n", list_contains(&list, "test13"));
    printf("contains: %d\n", list_contains(&list, "test12"));
    list_cleanup(&list);

    map_init(&map);
    parse_map("res/drawable/appdm_apps.png\0/sdcard/company.png\0", 48, &map);
    printf("size=%d\n", map.size);
}
*/
