#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include "util.h"

#include "libzip/config.h"

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifndef HAVE_GETOPT
#include "getopt.h"
#endif

#include "libzip/zip.h"
#include "libzip/compat.h"

zip_flags_t name_flags;


////////////////////////////////////////////////////////////////////////////////

int is_common_img(const char *fname)
{
    int len = strlen(fname);
    if (len > 4) {
        // jpg file
        if (fname[len - 1] == 'g' && 
            fname[len - 2] == 'p' &&
            fname[len - 3] == 'j' &&
            fname[len - 4] == '.') {
            return 1;
        }
        // png file
        if (fname[len - 1] == 'g' && 
            fname[len - 2] == 'n' &&
            fname[len - 3] == 'p' &&
            fname[len - 4] == '.') {
            if (len > 6 && fname[len - 5] == '9' && fname[len - 6] == '.') {
                return 0;
            } else {
                return 1;
            }
        }
    }
    return 0;
}

int is_png_file(const char *fname) 
{
    int len = strlen(fname);
    if (len > 4) {
        // png file
        if (fname[len - 1] == 'g' && 
            fname[len - 2] == 'n' &&
            fname[len - 3] == 'p' &&
            fname[len - 4] == '.') {
            if (len > 6 && fname[len - 5] == '9' && fname[len - 6] == '.') {
                return 0;
            } else {
                return 1;
            }
        }
    }
    return 0;
}

// Is res/... resources.arsc, AndroidManifest.xml
int is_resource_files(const char *name, struct Map *mapping)
{
    int len = strlen(name);
    if (len > 4 && name[0] == 'r' && name[1] == 'e' && name[2] == 's' && name[3] == '/') {
        return 1;
    }
    if (len > 2 && name[0] == 'r' && name[1] == '/') { // "r/"
        return 1;
    }
    if (len == 14 && strcmp(name, "resources.arsc") == 0) {
        return 1;
    }
    if (len == 19 && strcmp(name, "AndroidManifest.xml") == 0) {
        return 1;
    }

    // For protected apk, the resource may not in res/ directory
    if (map_contains_value(mapping, name)) {
        return 1;
    }

    return 0;
}

// Return 1 when succeed
// "res/drawable-hdpi-v4/ic_launcher.png" --> "res/drawable-hdpi/ic_launcher.png"
static int get_name_without_version(const char *name, char *buf, int len) {
    if (name[0] == 'r' && name[1] == 'e' && name[2] == 's' && name[3] == '/') {

        do {
            // Find '/' after strings like 'drawable-hdpi-v4'
            const char *pslash = strchr(name + 4, '/');
            if (pslash == NULL) break;

            const char *pdash = pslash - 1;
            while ((pdash > name + 3) && *pdash != '-') {
                pdash--;
            }

            // Cannot find '-'
            if (*pdash != '-') break;

            // After dash is not 'v'
            if (pdash[1] != 'v') break;

            // After 'v' should be digits
            int is_digit = 1;
            const char *p = pdash + 2;
            while (p < pslash) {
                if (*p < '0' || *p > '9') { is_digit = 0; break; }
                p++;
            }
            if (!is_digit) { break; }

            int head_size = pdash - name;
            memcpy(buf, name, head_size);
            strncat(buf, pslash, len - head_size);
        
            return 1;
        } while (0);
    }

    return 0;
}

// tname: target zip file, new build apk (contains new built resource)
// sname: source zip file, original apk
// mapping: map the entry from new build apk to the original apk ; also called as fileEntry2ZipEntry
// replaces: file replaces, entry name to file path
int merge(const char *tname, const char *sname, struct Map *mapping, struct Map *replaces)
{
    zip_t *za = NULL;
    zip_t *zs = NULL;
    zip_source_t *source;
    int c, err;
    zip_int64_t za_entries, zs_entries, idx;
    zip_uint64_t i;
    const char *fname = NULL;
    const char *name = NULL;
//int dd;
    name_flags = 0;

    // Open the target zip file
    if ((za=zip_open(tname, ZIP_CREATE, &err)) == NULL) {
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
    za_entries = zip_get_num_entries(za, 0);
    zs_entries = zip_get_num_entries(zs, 0);
    if (za_entries < 0) {
        zip_close(za);
        zip_close(zs);
        LOGE("Can't get # for '%s': %s\n", tname, zip_strerror(za));
        return -1;
    }
    if (zs_entries < 0) {
        zip_close(za);
        zip_close(zs);
        LOGE("Can't get # for '%s': %s\n", sname, zip_strerror(zs));
        return -1;
    }

    // Enumerate the new generated resource apk
    for (i=0; i<za_entries; i++) {
        fname = zip_get_name(za, i, 0);
        if (fname == NULL) {
            continue;
        }

        // Replace from file
        name = map_get(replaces, fname);
        if (name != NULL) {
            LOGD("Replace %s with %s\n", fname, name);
            if ((source=zip_source_file(za, name, 0, 0)) == NULL
                 || zip_replace(za, (zip_uint64_t)i, source) < 0) {
                zip_source_free(source);
                LOGE("Can't replace '%s' in `%s': %s\n",
                    fname, tname, zip_strerror(za));
            }
            continue;
        }

        if (!is_common_img(fname)) {
            continue;
        }

        // Check if the png file is already be replaced or not
        // If the png file already be replaced, then skip this file
        if (is_png_file(fname)) {
            struct zip_stat sb;
            zip_stat_index(za, i, 0, &sb);
            if (sb.size != 68) { // Dummy png is 68 bytes
                LOGE("Entry %s is not 68 bytes, %lld bytes\n", fname, sb.size);
                continue;
            }
        }

        // Replace from the original apk
        name = map_get(mapping, fname);
        if (name == NULL) { name = fname; }
#if defined(RELEASE)
        {
            int ret = (dex_size - correct_dex_size[0]) * (dex_size - correct_dex_size[1]);
            if (ret != 0) {
                continue;
            }
        }
#endif
        idx = zip_name_locate(zs, name, name_flags);
        if (idx < 0) {
            char name_nover[128] = { '\0' };
            if (get_name_without_version(name, name_nover, sizeof(name_nover))) {
                LOGE("Cannot find %s, try %s\n", name, name_nover);
                idx = zip_name_locate(zs, name_nover, name_flags);
            } else {
                LOGE("Cannot get name_noversion for %s\n", name);
            }
        }
        if (idx >= 0) {
            //LOGD("Replace %s with %s\n", fname, name);
            if ((source=zip_source_zip(za, zs, idx, 0, 0, 0)) == NULL
                 || zip_replace(za, (zip_uint64_t)i, source) < 0) {
                if (source != NULL) { zip_source_free(source); }
                LOGE("Can't replace '%s' in `%s': %s\n",
                    fname, tname, zip_strerror(za));
            }
        } else {
            LOGE("Cannot locate %s in source apk file.\n", name);
        }
    }

    // Enumerate the original apk to copy other files
    for (i=0; i<zs_entries; i++) {
        fname = zip_get_name(zs, i, ZIP_FL_UNCHANGED | ZIP_FL_ENC_RAW);
        if (fname == NULL) {
            continue;
        }
        if (is_resource_files(fname, mapping)) {
            continue;
        }
/*
LOGD("add file: %s\n", fname);
int namelen = strlen(fname);
{  
  int tmpIdx = 0;
  char *tmp = (char *)malloc(namelen * 3 + 16);
  memset(tmp, 0, namelen * 3 + 16);
  for (dd = 0; dd < namelen; ++dd) {
    sprintf(tmp + tmpIdx, "%02x ", fname[dd]);
    tmpIdx += 3;
  }
  LOGD("len=%d, %s", namelen, tmp);
  free(tmp);
}
*/
        if ((source=zip_source_zip(za, zs, i, 0, 0, 0)) == NULL
             || zip_add(za, fname, source) < 0) {
            zip_source_free(source);
            LOGE("Can't add '%s' to `%s': %s\n",
                    fname, tname, zip_strerror(za));
        }
    }

    // Save the target zip (generated resource apk)
    if (zip_close(za) < 0) {
        LOGE("Can't save '%s': %s\n", tname, zip_strerror(za));
        zip_close(zs);
        return -1;
    }

    zip_close(zs);

    return 0;
}


// Merge 
__attribute__((section (".mtext"))) void m(JNIEnv* env, jobject thiz, jstring _tname, jstring _sname,
                                           jstring _replace, int len2, jstring _mapping, int len1)
{
    const char *tname = (*env)->GetStringUTFChars(env,_tname,NULL);
    const char *sname = (*env)->GetStringUTFChars(env,_sname,NULL);
    const char *str_mapping = (*env)->GetStringUTFChars(env,_mapping,NULL);
    const char *str_replace = (*env)->GetStringUTFChars(env,_replace,NULL);

    struct Map mapping, replaces;
    map_init(&mapping);
    map_init(&replaces);
    if (len1 > 0) {
        parse_map(str_mapping, len1, &mapping);
        LOGD("mapping->size=%d", mapping.size);
    }
    if (len2 > 0) {
        parse_map(str_replace, len2, &replaces);
    }

    merge(tname, sname, &mapping, &replaces);

    map_cleanup(&mapping);
    map_cleanup(&replaces);

    (*env)->ReleaseStringUTFChars(env, _tname, tname);
    (*env)->ReleaseStringUTFChars(env, _sname, sname);
    (*env)->ReleaseStringUTFChars(env, _mapping, str_mapping);
    (*env)->ReleaseStringUTFChars(env, _replace, str_replace);
}
