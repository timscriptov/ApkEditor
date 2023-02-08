#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include "util.h"

#include "config.h"

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifndef HAVE_GETOPT
#include "getopt.h"
#endif

#include "zip.h"
#include "compat.h"

// Mofity the source zip file, and save the result to target
// tname: target zip file path
// sname: source zip file path
// Return 0 when succeed
static int modify(const char *tname, const char *sname, 
                  struct Map *added, struct StringList *removed, struct Map *replaced)
{
    zip_t *za = NULL;
    zip_t *zs = NULL;
    zip_source_t *source;
    int c, err;
    zip_int64_t zs_entries;
    zip_uint64_t i;
    int k;
    const char *fname = NULL;
    const char *name = NULL;

    // Open the target zip file
    if ((za=zip_open(tname, ZIP_CREATE|ZIP_TRUNCATE, &err)) == NULL) {
        zip_error_t error;
        zip_error_init_with_code(&error, err);
        LOGE("Can't open '%s': %s\n", tname, zip_error_strerror(&error));
        zip_error_fini(&error);
        return -1;
    }

    // Open the source zip file
    if ((zs=zip_open(sname, 0, &err)) == NULL) {
        zip_close(za);
        zip_error_t error;
        zip_error_init_with_code(&error, err);
        LOGE("Can't open '%s': %s\n", sname, zip_error_strerror(&error));
        zip_error_fini(&error);
        return -1;
    }

    // Get entry number
    zs_entries = zip_get_num_entries(zs, 0);
    if (zs_entries < 0) {
        zip_close(za);
        zip_close(zs);
        LOGE("Can't get # for '%s': %s\n", sname, zip_strerror(zs));
        return -1;
    }

    // Enumerate the source zip
    for (i=0; i<zs_entries; i++) {
        fname = zip_get_name(zs, i, 0);
        if (fname == NULL) {
            continue;
        }

        // Removed
        if (list_contains(removed, fname)) {
            LOGD("removed: %s", fname);
            continue;
        }

        // Replace from file
        name = map_get(replaced, fname);
        if (name != NULL) {
            LOGD("replace %s with %s", fname, name);
            if ((source=zip_source_file(za, name, 0, 0)) == NULL
                 || zip_add(za, fname, source) < 0) {
                zip_source_free(source);
                LOGE("Can't add '%s' to `%s': %s\n",
                    fname, tname, zip_strerror(za));
            }
            continue;
        }

        // Directly add the entry in source zip
        //if ((source=zip_source_zip(za, zs, i, 0, 0, 0)) == NULL
        if ((source=zip_source_zip(za, zs, i, ZIP_FL_UNCHANGED, 0, -1)) == NULL
             || zip_add(za, fname, source) < 0) {
            zip_source_free(source);
            LOGE("Can't add '%s' in `%s': %s\n",
                fname, tname, zip_strerror(za));
        }
    } // end for

    // Add the added entries to target zip
    for (k=0; k<added->size; k++) {
        fname = added->keys[k];
        name = added->values[k];
        LOGD("adding %s -> %s", fname, name);
        if ((source=zip_source_file(za, name, 0, 0)) == NULL
             || zip_file_add(za, fname, source, ZIP_FL_ENC_UTF_8) < 0) {
            zip_source_free(source);
            LOGE("Can't add '%s' to `%s': %s\n",
                fname, tname, zip_strerror(za));
        }
    }

    // Save the target zip
    if (zip_close(za) < 0) {
        LOGE("Can't save '%s': %s\n", tname, zip_strerror(za));
        zip_close(zs);
        return -1;
    }

    zip_close(zs);
    return 0;
}


// Modify a zip file
__attribute__((section (".mtext")))
void zip_modify(JNIEnv* env, jobject thiz, jstring _tname, jstring _sname,
                jstring _added, int len1, jstring _removed, int len2, jstring _replaced, int len3)
{
    const char *tname = (*env)->GetStringUTFChars(env,_tname,NULL);
    const char *sname = (*env)->GetStringUTFChars(env,_sname,NULL);
    const char *str_added = (*env)->GetStringUTFChars(env,_added,NULL);
    const char *str_removed = (*env)->GetStringUTFChars(env,_removed,NULL);
    const char *str_replaced = (*env)->GetStringUTFChars(env,_replaced,NULL);

    struct Map added, replaced;
    struct StringList removed;
    map_init(&added);
    map_init(&replaced);
    list_init(&removed);

    if (len1 > 0) {
        parse_map(str_added, len1, &added);
    }
    if (len3 > 0) {
        parse_map(str_replaced, len3, &replaced);
    }
    if (len2 > 0) {
        parse_list(str_removed, len2, &removed);
    }

    //
    LOGD("add: %d, remove: %d, replace: %d", added.size, removed.size, replaced.size);
    modify(tname, sname, &added, &removed, &replaced);

    map_cleanup(&added);
    map_cleanup(&replaced);
    list_cleanup(&removed);

    (*env)->ReleaseStringUTFChars(env, _tname, tname);
    (*env)->ReleaseStringUTFChars(env, _sname, sname);
    (*env)->ReleaseStringUTFChars(env, _added, str_added);
    (*env)->ReleaseStringUTFChars(env, _removed, str_removed);
    (*env)->ReleaseStringUTFChars(env, _replaced, str_replaced);
}
