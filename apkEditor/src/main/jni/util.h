
#if defined(RELEASE)
#define LOGD(fmt, args...)
#define LOGE(fmt, args...)
#else
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, "DEBUG", fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, "DEBUG", fmt, ##args)
#endif

#define MAP_INIT_SIZE 128

struct Map {
    int size;
    int capacity;
    const char **keys;
    const char **values;
    unsigned long *hashes;
};

void map_init(struct Map *pmap);
void map_cleanup(struct Map *pmap);
void map_put(struct Map *pmap, const char *key, const char *value);
const char *map_get(struct Map *pmap, const char *key);
int map_contains_value(struct Map *pmap, const char *value);

////////////////////////////////////////////////////////////////////////////////

struct StringList {
    int size;
    int capacity;
    const char **strs;
    int *lens;
};

void list_init(struct StringList *plist);
void list_cleanup(struct StringList *plist);
void list_add(struct StringList *plist, const char *str, int len);
int list_contains(struct StringList *plist, const char *str);
const char *list_get(struct StringList *plist, int index);

////////////////////////////////////////////////////////////////////////////////

extern int correct_dex_size[];
extern int correct_manifest_size[];
extern int correct_lib_size[];
extern int dex_size;
extern int manifest_size;

