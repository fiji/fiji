#include "libavutil/log.h"

extern void avSetLogCallback(void (*callback)(const char *));

static size_t log_to_buffer(char *line, size_t size, void* ptr, int level, const char* fmt, va_list vl)
{
    AVClass* avc= ptr ? *(AVClass**)ptr : NULL;
    line[0]=0;

    if(avc) {
        if(avc->version >= (50<<16 | 15<<8 | 3) && avc->parent_log_context_offset){
            AVClass** parent= *(AVClass***)(((uint8_t*)ptr) + avc->parent_log_context_offset);
            if(parent && *parent){
                snprintf(line, size, "[%s @ %p] ", (*parent)->item_name(parent), parent);
            }
        }
        snprintf(line + strlen(line), size - strlen(line), "[%s @ %p] ", avc->item_name(ptr), ptr);
    }

    return strlen(line) + vsnprintf(line + strlen(line), size - strlen(line), fmt, vl);
}

static void (*line_log_callback)(const char *) = NULL;

static void helper(void *ptr, int level, const char *fmt, va_list vl)
{
    static char line[16384];
    static size_t offset;
    if(level>av_log_get_level())
        return;

    offset += log_to_buffer(line + offset, sizeof(line) - offset, ptr, level, fmt, vl);
    /* Call only for full lines */
    if (offset + 1 >= sizeof(line) ||
		(offset > 0 && line[offset - 1] == '\n' &&
			(line[offset - 1] = '\0') == '\0')) {
        line_log_callback(line);
        offset = 0;
    }
}

void avSetLogCallback(void (*callback)(const char *))
{
    line_log_callback = callback;
    av_log_set_callback(helper);
}
