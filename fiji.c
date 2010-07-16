#define _BSD_SOURCE
#include <stdlib.h>
#include "jni.h"
#include <stdlib.h>
#include <limits.h>
#include <string.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>

#ifdef MACOSX
#include <stdlib.h>
#include <pthread.h>
#include <CoreFoundation/CoreFoundation.h>

struct string;
static void append_icon_path(struct string *str);
static void set_path_to_JVM(void);
static int get_fiji_bundle_variable(const char *key, struct string *value);
#endif

#ifdef WIN32
#include <io.h>
#include <process.h>
#define PATH_SEP ";"

static void open_win_console();

/* TODO: use dup2() and freopen() and a thread to handle the output */
#else
#define PATH_SEP ":"
#endif

static void error(const char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);
	fputc('\n', stderr);
}

__attribute__((__noreturn__))
__attribute__((format (printf, 1, 2)))
static void die(const char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);
	fputc('\n', stderr);

	exit(1);
}

static void *xmalloc(size_t size)
{
	void *result = malloc(size);
	if (!result)
		die("Out of memory");
	return result;
}

static void *xcalloc(size_t size, size_t nmemb)
{
	void *result = calloc(size, nmemb);
	if (!result)
		die("Out of memory");
	return result;
}

static void *xrealloc(void *p, size_t size)
{
	void *result = realloc(p, size);
	if (!result)
		die("Out of memory");
	return result;
}

static char *xstrdup(const char *buffer)
{
	char *result = strdup(buffer);
	if (!result)
		die("Out of memory");
	return result;
}

static char *xstrndup(const char *buffer, size_t max_length)
{
	char *eos = memchr(buffer, '\0', max_length - 1);
	int len = eos ? eos - buffer : max_length;
	char *result = xmalloc(len + 1);

	if (!result)
		die("Out of memory");

	memcpy(result, buffer, len);
	result[len] = '\0';

	return result;
}

struct string {
	int alloc, length;
	char *buffer;
};

static void string_ensure_alloc(struct string *string, int length)
{
	if (string->alloc <= length) {
		char *new_buffer = xrealloc(string->buffer, length + 1);

		string->buffer = new_buffer;
		string->alloc = length;
	}
}

static void string_set_length(struct string *string, int length)
{
	if (length > string->length)
		die("Cannot enlarge strings");
	string->length = length;
	string->buffer[length] = '\0';
}

static void string_set(struct string *string, const char *buffer)
{
	free(string->buffer);
	string->buffer = xstrdup(buffer);
	string->alloc = string->length = strlen(buffer);
}

static struct string *string_init(int length)
{
	struct string *string = xcalloc(sizeof(struct string), 1);

	string_ensure_alloc(string, length);
	string->buffer[0] = '\0';
	return string;
}

static struct string *string_copy(const char *string)
{
	int len = strlen(string);
	struct string *result = string_init(len);

	memcpy(result->buffer, string, len + 1);
	result->length = len;

	return result;
}

static void string_release(struct string *string)
{
	if (string) {
		free(string->buffer);
		free(string);
	}
}

static void string_add_char(struct string *string, char c)
{
	if (string->alloc == string->length)
		string_ensure_alloc(string, 3 * (string->alloc + 16) / 2);
	string->buffer[string->length++] = c;
	string->buffer[string->length] = '\0';
}

static void string_append(struct string *string, const char *append)
{
	int len = strlen(append);

	string_ensure_alloc(string, string->length + len);
	memcpy(string->buffer + string->length, append, len + 1);
	string->length += len;
}

static void string_append_path_list(struct string *string, const char *append)
{
	int len = strlen(append);

	if (string->length)
		string_append(string, PATH_SEP);

	string_ensure_alloc(string, string->length + len + 1);
	memcpy(string->buffer + string->length, append, len + 1);
	string->length += len;
}

static void string_append_at_most(struct string *string, const char *append, int length)
{
	int len = strlen(append);

	if (len > length)
		len = length;

	string_ensure_alloc(string, string->length + len);
	memcpy(string->buffer + string->length, append, len + 1);
	string->length += len;
	string->buffer[string->length] = '\0';
}

static int number_length(unsigned long number, long base)
{
        int length = 1;
        while (number >= base) {
                number /= base;
                length++;
        }
        return length;
}

static inline int is_digit(char c)
{
	return c >= '0' && c <= '9';
}

static void string_vaddf(struct string *string, const char *fmt, va_list ap)
{
	while (*fmt) {
		char fill = '\0';
		int size = -1, max_size = -1;
		char *p = (char *)fmt;

		if (*p != '%' || *(++p) == '%') {
			string_add_char(string, *p++);
			fmt = p;
			continue;
		}
		if (*p == ' ' || *p == '0')
			fill = *p++;
		if (is_digit(*p))
			size = (int)strtol(p, &p, 10);
		else if (p[0] == '.' && p[1] == '*') {
			max_size = va_arg(ap, int);
			p += 2;
		}
		switch (*p) {
		case 's': {
			const char *s = va_arg(ap, const char *);
			if (fill) {
				int len = size - strlen(s);
				while (len-- > 0)
					string_add_char(string, fill);
			}
			while (*s && max_size--)
				string_add_char(string, *s++);
			break;
		}
		case 'c':
			string_add_char(string, va_arg(ap, int));
			break;
		case 'u':
		case 'i':
		case 'l':
		case 'd':
		case 'o':
		case 'x':
		case 'X': {
			int base = *p == 'x' || *p == 'X' ? 16 :
				*p == 'o' ? 8 : 10;
			int negative = 0, len;
			unsigned long number, power;

			if (*p == 'u')
				number = va_arg(ap, unsigned int);
			else {
				long signed_number;
				if (*p == 'l')
					signed_number = va_arg(ap, long);
				else
					signed_number = va_arg(ap, int);
				if (signed_number < 0) {
					negative = 1;
					number = -signed_number;
				} else
					number = signed_number;
			}

			/* pad */
			len = number_length(number, base);
			while (size-- > len + negative)
				string_add_char(string, fill ? fill : ' ');
			if (negative)
				string_add_char(string, '-');

			/* output number */
			power = 1;
			while (len-- > 1)
				power *= base;
			while (power) {
				int digit = number / power;
				string_add_char(string, digit < 10 ? '0' + digit
					: *p + 'A' - 'X' + digit - 10);
				number -= digit * power;
				power /= base;
			}

			break;
		}
		default:
			/* unknown / invalid format: copy verbatim */
			string_append_at_most(string, fmt, p - fmt + 1);
		}
		fmt = p + (*p != '\0');
	}
}

__attribute__((format (printf, 2, 3)))
static void string_addf(struct string *string, const char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	string_vaddf(string, fmt, ap);
	va_end(ap);
}

__attribute__((format (printf, 2, 3)))
static void string_addf_path_list(struct string *string, const char *fmt, ...)
{
	va_list ap;

	if (string->length)
		string_append(string, PATH_SEP);

	va_start(ap, fmt);
	string_vaddf(string, fmt, ap);
	va_end(ap);
}

__attribute__((format (printf, 2, 3)))
static void string_setf(struct string *string, const char *fmt, ...)
{
	va_list ap;

	string_ensure_alloc(string, strlen(fmt) + 64);
	string->length = 0;
	string->buffer[0] = '\0';
	va_start(ap, fmt);
	string_vaddf(string, fmt, ap);
	va_end(ap);
}

__attribute__((format (printf, 1, 2)))
static struct string *string_initf(const char *fmt, ...)
{
	struct string *string = string_init(strlen(fmt) + 64);
	va_list ap;

	va_start(ap, fmt);
	string_vaddf(string, fmt, ap);
	va_end(ap);

	return string;
}

static void string_replace(struct string *string, char from, char to)
{
	int j;
	for (j = 0; j < string->length; j++)
		if (string->buffer[j] == from)
			string->buffer[j] = to;
}

/*
 * If set, overrides the environment variable JAVA_HOME, which in turn
 * overrides relative_java_home.
 */
static const char *absolute_java_home;
static const char *relative_java_home = JAVA_HOME;
static const char *library_path = JAVA_LIB_PATH;
static const char *default_main_class = "fiji.Main";

static int is_default_main_class(const char *name)
{
	return !strcmp(name, default_main_class) || !strcmp(name, "ij.ImageJ");
}

/* Dynamic library loading stuff */

#ifdef WIN32
#include <windows.h>
#define RTLD_LAZY 0
static char *dlerror_value;

static char *get_win_error(void)
{
	DWORD error_code = GetLastError();
	LPSTR buffer;

	FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
			FORMAT_MESSAGE_FROM_SYSTEM |
			FORMAT_MESSAGE_IGNORE_INSERTS,
			NULL,
			error_code,
			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
			(LPSTR)&buffer,
			0, NULL);
	return buffer;
}

static void *dlopen(const char *name, int flags)
{
	void *result = LoadLibrary(name);

	dlerror_value = get_win_error();

	return result;
}

static char *dlerror(void)
{
	/* We need to reset the error */
	char *result = dlerror_value;
	dlerror_value = NULL;
	return result;
}

static void *dlsym(void *handle, const char *name)
{
	void *result = (void *)GetProcAddress((HMODULE)handle, name);
	dlerror_value = result ? NULL : (char *)"function not found";
	return result;
}

static void sleep(int seconds)
{
	Sleep(seconds * 1000);
}

// There is no setenv on Windows, so it should be safe for us to
// define this compatible version
static int setenv(const char *name, const char *value, int overwrite)
{
	struct string *string;

	if (!overwrite && getenv(name))
		return 0;
	if ((!name) || (!value))
		return 0;

	string = string_initf("%s=%s", name, value);
	return putenv(string->buffer);
}

// Similarly we can do the same for unsetenv:
static int unsetenv(const char *name)
{
	struct string *string = string_initf("%s=", name);
	return putenv(string->buffer);
}

#else
#include <dlfcn.h>
#endif

// A wrapper for setenv that exits on error
void setenv_or_exit(const char *name, const char *value, int overwrite)
{
	int result;
	if (!value) {
#ifdef MACOSX
		unsetenv(name);
#else
		result = unsetenv(name);
		if (result)
			die("Unsetting environment variable %s failed", name);
#endif
		return;
	}
	result = setenv(name, value, overwrite);
	if (result)
		die("Setting environment variable %s to %s failed", name, value);
}

/* Determining heap size */

#ifdef MACOSX
#include <mach/mach_init.h>
#include <mach/mach_host.h>

size_t get_memory_size(int available_only)
{
	host_priv_t host = mach_host_self();
	vm_size_t page_size;
	vm_statistics_data_t host_info;
	mach_msg_type_number_t host_count =
		sizeof(host_info) / sizeof(integer_t);

	host_page_size(host, &page_size);
	return host_statistics(host, HOST_VM_INFO,
			(host_info_t)&host_info, &host_count) ?
		0 : ((size_t)(available_only ? host_info.free_count :
				host_info.active_count +
				host_info.inactive_count +
				host_info.wire_count) * (size_t)page_size);
}
#elif defined(linux)
size_t get_memory_size(int available_only)
{
	ssize_t page_size = sysconf(_SC_PAGESIZE);
	ssize_t available_pages = sysconf(available_only ?
			_SC_AVPHYS_PAGES : _SC_PHYS_PAGES);
	return page_size < 0 || available_pages < 0 ?
		0 : (size_t)page_size * (size_t)available_pages;
}
#elif defined(WIN32)
#include <windows.h>

size_t get_memory_size(int available_only)
{
	MEMORYSTATUS status;

	GlobalMemoryStatus(&status);
	return available_only ? status.dwAvailPhys : status.dwTotalPhys;
}
#else
size_t get_memory_size(int available_only)
{
	fprintf(stderr, "Unsupported\n");
	return 0;
}
#endif

static long long parse_memory(const char *amount)
{
	char *endp;
	long long result = strtoll(amount, &endp, 0);

	if (endp)
		switch (*endp) {
		case 't': case 'T':
			result <<= 10;
			/* fall through */
		case 'g': case 'G':
			result <<= 10;
			/* fall through */
		case 'm': case 'M':
			result <<= 10;
			/* fall through */
		case 'k': case 'K':
			result <<= 10;
			break;
		case '\0':
			/* fall back to megabyte */
			if (result < 1024)
				result <<= 20;
		}

	return result;
}

__attribute__((unused))
static int parse_bool(const char *value)
{
	return strcmp(value, "0") && strcmp(value, "false") &&
		strcmp(value, "False") && strcmp(value, "FALSE");
}

/* work around a SuSE IPv6 setup bug */

#ifdef IPV6_MAYBE_BROKEN
#include <netinet/ip6.h>
#include <fcntl.h>
#endif

static int is_ipv6_broken(void)
{
#ifndef IPV6_MAYBE_BROKEN
	return 0;
#else
	int sock = socket(AF_INET6, SOCK_STREAM, 0);
	struct sockaddr_in6 address = {
		AF_INET6, 57294 + 7, 0, in6addr_loopback, 0
	};
	long flags;

	if (sock < 0)
		return 1;

	flags = fcntl(sock, F_GETFL, NULL);
	if (fcntl(sock, F_SETFL, flags | O_NONBLOCK) < 0) {
		close(sock);
		return 1;
	}


	int result = 0;
	if (connect(sock, (struct sockaddr *)&address, sizeof(address)) < 0) {
		if (errno == EINPROGRESS) {
			struct timeval tv;
			fd_set fdset;

			tv.tv_sec = 0;
			tv.tv_usec = 50000;
			FD_ZERO(&fdset);
			FD_SET(sock, &fdset);
			if (select(sock + 1, NULL, &fdset, NULL, &tv) > 0) {
				int error;
				socklen_t length = sizeof(int);
				if (getsockopt(sock, SOL_SOCKET, SO_ERROR,
						(void*)&error, &length) < 0)
					result = 1;
				else
					result = (error == EACCES) |
						(error == EPERM) |
						(error == EAFNOSUPPORT) |
						(error == EINPROGRESS);
			} else
				result = 1;
		} else
			result = (errno == EACCES) | (errno == EPERM) |
				(errno == EAFNOSUPPORT);
	}

	close(sock);
	return result;
#endif
}


/* Java stuff */

#ifndef JNI_CREATEVM
#define JNI_CREATEVM "JNI_CreateJavaVM"
#endif

const char *fiji_dir;

static const char *fiji_path(const char *relative_path)
{
	static struct string *string[3];
	static int counter;

	counter = ((counter + 1) % (sizeof(string) / sizeof(string[0])));
	if (!string[counter])
		string[counter] = string_initf("%s/%s", fiji_dir, relative_path);
	else
		string_setf(string[counter], "%s/%s", fiji_dir, relative_path);
	return string[counter]->buffer;
}

char *main_argv0;
char **main_argv, **main_argv_backup;
int main_argc, main_argc_backup;
const char *main_class;
int run_precompiled = 0;

static int dir_exists(const char *directory);

static const char *get_java_home(void)
{
	if (absolute_java_home)
		return absolute_java_home;
	const char *env = getenv("JAVA_HOME");
	if (env) {
		if (dir_exists(env))
			return env;
		else {
			error("Ignoring invalid JAVA_HOME: %s", env);
			unsetenv("JAVA_HOME");
		}
	}
	return fiji_path(relative_java_home);
}

static const char *get_jre_home(void)
{
	const char *result = get_java_home();
	int len = strlen(result);
	static struct string *jre;

	if (jre)
		return jre->buffer;

	if (len > 4 && !strcmp(result + len - 4, "/jre"))
		return result;

	jre = string_initf("%s/jre", result);
	if (dir_exists(jre->buffer))
		return jre->buffer;
	string_setf(jre, "%s", result);
	return result;
}

static size_t mystrlcpy(char *dest, const char *src, size_t size)
{
	size_t ret = strlen(src);

	if (size) {
		size_t len = (ret >= size) ? size - 1 : ret;
		memcpy(dest, src, len);
		dest[len] = '\0';
	}
	return ret;
}

const char *last_slash(const char *path)
{
	const char *slash = strrchr(path, '/');
#ifdef WIN32
	const char *backslash = strrchr(path, '\\');

	if (backslash && slash < backslash)
		slash = backslash;
#endif
	return slash;
}

static const char *make_absolute_path(const char *path)
{
	static char bufs[2][PATH_MAX + 1], *buf = bufs[0];
	char cwd[1024] = "";
#ifndef WIN32
	static char *next_buf = bufs[1];
	int buf_index = 1, len;
#endif

	int depth = 20;
	char *last_elem = NULL;
	struct stat st;

	if (mystrlcpy(buf, path, PATH_MAX) >= PATH_MAX)
		die("Too long path: %s", path);

	while (depth--) {
		if (stat(buf, &st) || !S_ISDIR(st.st_mode)) {
			const char *slash = last_slash(buf);
			if (slash) {
				buf[slash-buf] = '\0';
				last_elem = xstrdup(slash + 1);
			} else {
				last_elem = xstrdup(buf);
				*buf = '\0';
			}
		}

		if (*buf) {
			if (!*cwd && !getcwd(cwd, sizeof(cwd)))
				die("Could not get current working dir");

			if (chdir(buf))
				die("Could not switch to %s", buf);
		}
		if (!getcwd(buf, PATH_MAX))
			die("Could not get current working directory");

		if (last_elem) {
			int len = strlen(buf);
			if (len + strlen(last_elem) + 2 > PATH_MAX)
				die("Too long path name: %s/%s", buf, last_elem);
			buf[len] = '/';
			strcpy(buf + len + 1, last_elem);
			free(last_elem);
			last_elem = NULL;
		}

#ifndef WIN32
		if (!lstat(buf, &st) && S_ISLNK(st.st_mode)) {
			len = readlink(buf, next_buf, PATH_MAX);
			if (len < 0)
				die("Invalid symlink: %s", buf);
			next_buf[len] = '\0';
			buf = next_buf;
			buf_index = 1 - buf_index;
			next_buf = bufs[buf_index];
		} else
#endif
			break;
	}

	if (*cwd && chdir(cwd))
		die("Could not change back to %s", cwd);

	return buf;
}

static int is_absolute_path(const char *path)
{
#ifdef WIN32
	if (((path[0] >= 'A' && path[0] <= 'Z') ||
			(path[0] >= 'a' && path[0] <= 'z')) && path[1] == ':')
		return 1;
#endif
	return path[0] == '/';
}

static int file_exists(const char *path)
{
	return !access(path, R_OK);
}

static inline int suffixcmp(const char *string, int len, const char *suffix)
{
	int suffix_len = strlen(suffix);
	if (len < suffix_len)
		return -1;
	return strncmp(string + len - suffix_len, suffix, suffix_len);
}

static const char *find_in_path(const char *path)
{
	const char *p = getenv("PATH");
	struct string *buffer;

#ifdef WIN32
	int len = strlen(path);
	struct string *path_with_suffix = NULL;
	const char *in_cwd;

	if (suffixcmp(path, len, ".exe") && suffixcmp(path, len, ".EXE")) {
		path_with_suffix = string_initf("%s.exe", path);
		path = path_with_suffix->buffer;
	}
	in_cwd = make_absolute_path(path);
	if (file_exists(in_cwd)) {
		string_release(path_with_suffix);
		return in_cwd;
	}
#endif

	if (!p)
		die("Could not get PATH");

	buffer = string_init(32);
	for (;;) {
		const char *colon = strchr(p, PATH_SEP[0]), *orig_p = p;
		int len = colon ? colon - p : strlen(p);
		struct stat st;

		if (!len)
			die("Could not find %s in PATH", path);

		p += len + !!colon;
		if (!is_absolute_path(orig_p))
			continue;
		string_setf(buffer, "%.*s/%s", len, orig_p, path);
#ifdef WIN32
#define S_IX S_IXUSR
#else
#define S_IX (S_IXUSR | S_IXGRP | S_IXOTH)
#endif
		if (!stat(buffer->buffer, &st) && S_ISREG(st.st_mode) &&
				(st.st_mode & S_IX)) {
			const char *result = make_absolute_path(buffer->buffer);
			string_release(buffer);
#ifdef WIN32
			string_release(path_with_suffix);
#endif
			return result;
		}
	}
}

#ifdef WIN32
static char *dos_path(const char *path)
{
	int size = GetShortPathName(path, NULL, 0);
	char *buffer = (char *)xmalloc(size);
	GetShortPathName(path, buffer, size);
	return buffer;
}
#endif

__attribute__((unused))
static struct string *get_parent_directory(const char *path)
{
	const char *slash = last_slash(path);

	if (!slash || slash == path)
		return string_initf("/");
	return string_initf("%.*s", (int)(slash - path), path);
}

__attribute__((unused))
static int path_list_contains(const char *list, const char *path)
{
	size_t len = strlen(path);
	const char *p = list;
	while (p && *p) {
		if (!strncmp(p, path, len) &&
				(p[len] == PATH_SEP[0] || !p[len]))
			return 1;
		p = strchr(p, PATH_SEP[0]);
		if (!p)
			break;
		p++;
	}
	return 0;
}

/*
 * On Linux, JDK5 does not find the library path with libmlib_image.so,
 * so we have to add that explicitely to the LD_LIBRARY_PATH.
 *
 * Unfortunately, ld.so only looks at LD_LIBRARY_PATH at startup, so we
 * have to reexec after setting that variable.
 *
 * See also line 140ff of
 * http://hg.openjdk.java.net/jdk6/jdk6/hotspot/file/14f7b2425c86/src/os/solaris/launcher/java_md.c
 */
static void maybe_reexec_with_correct_lib_path(void)
{
#ifdef linux
	struct string *path = string_initf("%s/%s", get_jre_home(), library_path);
	struct string *parent = get_parent_directory(path->buffer);
	struct string *lib_path = get_parent_directory(parent->buffer);
	struct string *jli = string_initf("%s/jli", lib_path->buffer);
	const char *original;

	string_release(path);
	string_release(parent);

	// Is this JDK6?
	if (dir_exists(jli->buffer)) {
		string_release(lib_path);
		string_release(jli);
		return;
	}
	string_release(jli);

	original = getenv("LD_LIBRARY_PATH");
	if (original && path_list_contains(original, lib_path->buffer)) {
		string_release(lib_path);
		return;
	}

	if (original)
		string_append_path_list(lib_path, original);
	setenv_or_exit("LD_LIBRARY_PATH", lib_path->buffer, 1);
	error("Re-executing with correct library lookup path");
	execv(main_argv_backup[0], main_argv_backup);
	die("Could not re-exec with correct library lookup!");
#endif
}

static const char *get_fiji_dir(const char *argv0)
{
	static const char *buffer;
	const char *slash;
	int len;

	if (buffer)
		return buffer;

	if (!last_slash(argv0))
		buffer = find_in_path(argv0);
	else
		buffer = make_absolute_path(argv0);
	argv0 = buffer;

	slash = last_slash(argv0);
	if (!slash)
		die("Could not get absolute path for executable");

	len = slash - argv0;
	if (!suffixcmp(argv0, len, "/precompiled") ||
			!suffixcmp(argv0, len, "\\precompiled")) {
		slash -= strlen("/precompiled");
		run_precompiled = 1;
	}
#ifdef MACOSX
	else if (!suffixcmp(argv0, len, "/Fiji.app/Contents/MacOS"))
		slash -= strlen("/Contents/MacOS");
#endif
#ifdef WIN32
	else if (!suffixcmp(argv0, len, "/PRECOM~1") ||
			!suffixcmp(argv0, len, "\\PRECOM~1")) {
		slash -= strlen("/PRECOM~1");
		run_precompiled = 1;
	}
#endif

	buffer = xstrndup(buffer, slash - argv0);
#ifdef WIN32
	buffer = dos_path(buffer);
#endif
	return buffer;
}

static int create_java_vm(JavaVM **vm, void **env, JavaVMInitArgs *args)
{
#ifdef MACOSX
	set_path_to_JVM();
#else
	// Save the original value of JAVA_HOME: if creating the JVM this
	// way doesn't work, set it back so that calling the system JVM
	// can use the JAVA_HOME variable if it's set...
	char *original_java_home_env = getenv("JAVA_HOME");
	struct string *buffer = string_init(32);
	void *handle;
	char *err;
	static jint (*JNI_CreateJavaVM)(JavaVM **pvm, void **penv, void *args);
	const char *java_home = get_jre_home();

#ifdef WIN32
	/* Windows automatically adds the path of the executable to PATH */
	struct string *path = string_initf("%s;%s/bin",
		getenv("PATH"), java_home);
	setenv_or_exit("PATH", path->buffer, 1);
	string_release(path);

	// on Windows, a setenv() invalidates strings obtained by getenv()
	if (original_java_home_env)
		original_java_home_env = xstrdup(original_java_home_env);
#endif
	setenv_or_exit("JAVA_HOME", java_home, 1);

	string_addf(buffer, "%s/%s", java_home, library_path);

	handle = dlopen(buffer->buffer, RTLD_LAZY);
	if (!handle) {
		setenv_or_exit("JAVA_HOME", original_java_home_env, 1);
		if (!file_exists(java_home))
			return 2;

		const char *err = dlerror();
		if (!err)
			err = "(unknown error)";
		error("Could not load Java library '%s': %s",
			buffer->buffer, err);
		return 1;
	}
	dlerror(); /* Clear any existing error */

	JNI_CreateJavaVM = (typeof(JNI_CreateJavaVM))dlsym(handle,
			JNI_CREATEVM);
	err = dlerror();
	if (err) {
		error("Error loading libjvm: %s", err);
		setenv_or_exit("JAVA_HOME", original_java_home_env, 1);
		return 1;
	}
#endif

	return JNI_CreateJavaVM(vm, env, args);
}

/* Windows specific stuff */

#ifdef WIN32
static int console_opened = 0;

static void sleep_a_while(void)
{
	sleep(60);
}

static void open_win_console(void)
{
	static int initialized = 0;
	struct string *kernel32_dll_path;
	void *kernel32_dll;
	BOOL WINAPI (*attach_console)(DWORD process_id) = NULL;
	HANDLE handle;

	if (initialized)
		return;
	initialized = 1;
	if (!isatty(1) && !isatty(2))
		return;

	kernel32_dll_path = string_initf("%s\\system32\\kernel32.dll",
		getenv("WINDIR"));
	kernel32_dll = dlopen(kernel32_dll_path->buffer, RTLD_LAZY);
	string_release(kernel32_dll_path);
	if (kernel32_dll)
		attach_console = (typeof(attach_console))
			dlsym(kernel32_dll, "AttachConsole");
	if (!attach_console || !attach_console((DWORD)-1)) {
		AllocConsole();
		console_opened = 1;
		atexit(sleep_a_while);
	} else {
		char title[1024];
		if (GetConsoleTitle(title, sizeof(title)) &&
				!strncmp(title, "rxvt", 4))
			return; // console already opened
	}

	handle = CreateFile("CONOUT$", GENERIC_WRITE, FILE_SHARE_WRITE,
		NULL, OPEN_EXISTING, 0, NULL);
	if (isatty(1)) {
		freopen("CONOUT$", "wt", stdout);
		SetStdHandle(STD_OUTPUT_HANDLE, handle);
	}
	if (isatty(2)) {
		freopen("CONOUT$", "wb", stderr);
		SetStdHandle(STD_ERROR_HANDLE, handle);
	}
}


static int fake_posix_mkdir(const char *name, int mode)
{
	return mkdir(name);
}
#define mkdir fake_posix_mkdir


struct entry {
	char d_name[PATH_MAX];
	int d_namlen;
} entry;

struct dir {
	struct string *pattern;
	HANDLE handle;
	WIN32_FIND_DATA find_data;
	int done;
	struct entry entry;
};

struct dir *open_dir(const char *path)
{
	struct dir *result = xcalloc(sizeof(struct dir), 1);
	if (!result)
		return result;
	result->pattern = string_initf("%s/*", path);
	result->handle = FindFirstFile(result->pattern->buffer,
			&(result->find_data));
	if (result->handle == INVALID_HANDLE_VALUE) {
		free(result);
		return NULL;
	}
	result->done = 0;
	return result;
}

struct entry *read_dir(struct dir *dir)
{
	if (dir->done)
		return NULL;
	strcpy(dir->entry.d_name, dir->find_data.cFileName);
	dir->entry.d_namlen = strlen(dir->entry.d_name);
	if (FindNextFile(dir->handle, &dir->find_data) == 0)
		dir->done = 1;
	return &dir->entry;
}

int close_dir(struct dir *dir)
{
	string_release(dir->pattern);
	FindClose(dir->handle);
	free(dir);
	return 0;
}

#define DIR struct dir
#define dirent entry
#define opendir open_dir
#define readdir read_dir
#define closedir close_dir
#else
#include <dirent.h>
#endif

static int dir_exists(const char *path)
{
	DIR *dir = opendir(path);
	if (dir) {
		closedir(dir);
		return 1;
	}
	return 0;
}

static int mkdir_p(const char *path)
{
	const char *slash;
	struct string *buffer;
	if (dir_exists(path))
		return 0;

	buffer = string_copy(path);
	for (;;) {
		int result;
		slash = last_slash(buffer->buffer);
		if (!slash)
			return -1;
		string_set_length(buffer, slash - buffer->buffer);
		result = mkdir(buffer->buffer, 0777);
		if (result)
			return result;
	}
	return 0;
}

static void add_java_home_to_path(void)
{
	const char *java_home = absolute_java_home;
	struct string *new_path = string_init(32), *buffer;
	const char *env;

	if (!java_home) {
		const char *env = getenv("JAVA_HOME");
		if (env)
			java_home = env;
		else {
			int len;
			java_home = fiji_path(relative_java_home);
			len = strlen(java_home);
			if (len > 4 && !strcmp(java_home + len - 4,
						"/jre"))
				java_home = xstrndup(java_home, len - 4);
			else
				java_home = xstrdup(java_home);
			absolute_java_home = java_home;
		}
	}

	buffer = string_initf("%s/bin", java_home);
	if (dir_exists(buffer->buffer))
		string_append_path_list(new_path, buffer->buffer);
	string_setf(buffer, "%s/jre/bin", java_home);
	if (dir_exists(buffer->buffer))
		string_append_path_list(new_path, buffer->buffer);

	env = getenv("PATH");
	string_append_path_list(new_path, env ? env : fiji_dir);
	setenv_or_exit("PATH", new_path->buffer, 1);
	string_release(buffer);
	string_release(new_path);
}

static int headless, headless_argc;

int build_classpath_for_string(struct string *result, struct string *jar_directory, int no_error) {
	const char *extension = ".jar";
	int extension_length = strlen(extension);
	DIR *directory = opendir(jar_directory->buffer);
	struct dirent *entry;

	if (!directory) {
		if (no_error)
			return 0;
		error("Failed to open: %s", jar_directory);
		return 1;
	}
	while (NULL != (entry = readdir(directory))) {
		const char *filename = entry->d_name;
		int len = strlen(filename);
		if (len > extension_length && !strcmp(filename + len - extension_length, extension))
			string_addf_path_list(result, "%s/%s", jar_directory->buffer, filename);
		else {
			if (filename[0] == '.')
				continue;
			len = jar_directory->length;
			string_addf(jar_directory, "/%s", filename);
			if (build_classpath_for_string(result, jar_directory, 1)) {
				string_set_length(jar_directory, len);
				return 1;
			}
			string_set_length(jar_directory, len);
		}

	}
	return 0;
}

int build_classpath(struct string *result, const char *jar_directory, int no_error) {
	struct string *string = string_copy(jar_directory);
	int res = build_classpath_for_string(result, string, no_error);

	string_release(string);
	return res;
}

static struct string *set_property(JNIEnv *env,
		const char *key, const char *value)
{
	static jclass system_class = NULL;
	static jmethodID set_property_method = NULL;

	if (!system_class) {
		system_class = (*env)->FindClass(env, "java/lang/System");
		if (!system_class)
			return NULL;
	}

	if (!set_property_method) {
		set_property_method = (*env)->GetStaticMethodID(env, system_class,
				"setProperty",
				"(Ljava/lang/String;Ljava/lang/String;)"
				"Ljava/lang/String;");
		if (!set_property_method)
			return NULL;
	}

	jstring result =
		(jstring)(*env)->CallStaticObjectMethod(env, system_class,
				set_property_method,
				(*env)->NewStringUTF(env, key),
				(*env)->NewStringUTF(env, value));
	if (result) {
		const char *chars = (*env)->GetStringUTFChars(env, result, NULL);
		struct string *res = string_copy(chars);
		(*env)->ReleaseStringUTFChars(env, result, chars);
		return res;
	}

	return NULL;
}

struct string_array {
	char **list;
	int nr, alloc;
};

static void append_string(struct string_array *array, char *str)
{
	if (array->nr >= array->alloc) {
		array->alloc = 2 * array->nr + 16;
		array->list = (char **)xrealloc(array->list,
				array->alloc * sizeof(str));
	}
	array->list[array->nr++] = str;
}

static void prepend_string(struct string_array *array, char *str)
{
	if (array->nr >= array->alloc) {
		array->alloc = 2 * array->nr + 16;
		array->list = (char **)xrealloc(array->list,
				array->alloc * sizeof(str));
	}
	memmove(array->list + 1, array->list, array->nr * sizeof(str));
	array->list[0] = str;
	array->nr++;
}

static void prepend_string_copy(struct string_array *array, const char *str)
{
	prepend_string(array, xstrdup(str));
}

static void append_string_array(struct string_array *target,
		struct string_array *source)
{
	if (target->alloc - target->nr < source->nr) {
		target->alloc += source->nr;
		target->list = (char **)xrealloc(target->list,
				target->alloc * sizeof(target->list[0]));
	}
	memcpy(target->list + target->nr, source->list,
			source->nr * sizeof(target->list[0]));
	target->nr += source->nr;
}

static JavaVMOption *prepare_java_options(struct string_array *array)
{
	JavaVMOption *result = (JavaVMOption *)xcalloc(array->nr,
			sizeof(JavaVMOption));
	int i;

	for (i = 0; i < array->nr; i++)
		result[i].optionString = array->list[i];

	return result;
}

static jobjectArray prepare_ij_options(JNIEnv *env, struct string_array* array)
{
	jstring jstr;
	jobjectArray result;
	int i;

	if (!(jstr = (*env)->NewStringUTF(env, array->nr ? array->list[0] : ""))) {
fail:
		(*env)->ExceptionDescribe(env);
		die("Failed to create ImageJ option array");
	}

	result = (*env)->NewObjectArray(env, array->nr,
			(*env)->FindClass(env, "java/lang/String"), jstr);
	if (!result)
		goto fail;
	for (i = 1; i < array->nr; i++) {
		if (!(jstr = (*env)->NewStringUTF(env, array->list[i])))
			goto fail;
		(*env)->SetObjectArrayElement(env, result, i, jstr);
	}
	return result;
}

struct options {
	struct string_array java_options, ij_options;
	int debug, use_system_jvm;
};

static void add_option(struct options *options, char *option, int for_ij)
{
	append_string(for_ij ?
			&options->ij_options : &options->java_options, option);
}

static void add_option_copy(struct options *options, const char *option, int for_ij)
{
	add_option(options, xstrdup(option), for_ij);
}

static void add_option_string(struct options *options, struct string *option, int for_ij)
{
	add_option(options, xstrdup(option->buffer), for_ij);
}

static int is_quote(char c)
{
	return c == '\'' || c == '"';
}

static int find_closing_quote(const char *s, char quote, int index, int len)
{
	int i;

	for (i = index; i < len; i++) {
		char c = s[i];
		if (c == quote)
			return i;
		if (is_quote(c))
			i = find_closing_quote(s, c, i + 1, len);
	}
	fprintf(stderr, "Unclosed quote: %s\n               ", s);
	for (i = 0; i < index; i++)
		fputc(' ', stderr);
	die("^");
}

static void add_options(struct options *options, const char *cmd_line, int for_ij)
{
	int len = strlen(cmd_line), i;
	struct string *current = string_init(32);

	for (i = 0; i < len; i++) {
		char c = cmd_line[i];
		if (is_quote(c)) {
			int i2 = find_closing_quote(cmd_line, c, i + 1, len);
			string_append_at_most(current, cmd_line + i + 1, i2 - i - 1);
			i = i2;
			continue;
		}
		if (c == ' ' || c == '\t' || c == '\n') {
			if (!current->length)
				continue;
			add_option_string(options, current, for_ij);
			string_set_length(current, 0);
		} else
			string_add_char(current, c);
	}
	if (current->length)
		add_option_string(options, current, for_ij);

	string_release(current);
}

__attribute__((unused))
static void read_file_as_string(const char *file_name, struct string *contents)
{
	char buffer[1024];
	FILE *in = fopen(file_name, "r");

	string_set_length(contents, 0);
	if (!in)
		return;

	while (!feof(in)) {
		int count = fread(buffer, 1, sizeof(buffer), in);
		string_append_at_most(contents, buffer, count);
	}
	fclose(in);
}

static struct string *quote_if_necessary(const char *option)
{
	struct string *result = string_init(32);
	for (; *option; option++)
		switch (*option) {
		case '\n':
			string_append(result, "\\n");
			break;
		case '\t':
			string_append(result, "\\t");
			break;
		case ' ': case '"': case '\\':
			string_add_char(result, '\\');
			/* fallthru */
		default:
			string_add_char(result, *option);
			break;
		}
	return result;
}

#ifdef WIN32
/* fantastic win32 quoting */
static char *quote_win32(char *option)
{
	char *p, *result, *r1;
	int backslashes = 0;

	for (p = option; *p; p++)
		if (strchr(" \"\t", *p))
			backslashes++;

	if (!backslashes)
		return option;

	result = (char *)xmalloc(strlen(option) + backslashes + 2 + 1);
	r1 = result;
	*(r1++) = '"';
	for (p = option; *p; p++) {
		if (*p == '"')
			*(r1++) = '\\';
		*(r1++) = *p;
	}
	*(r1++) = '"';
	*(r1++) = '\0';

	return result;
}
#endif

static void show_commandline(struct options *options)
{
	int j;

	printf("java");
	for (j = 0; j < options->java_options.nr; j++) {
		struct string *quoted = quote_if_necessary(options->java_options.list[j]);
		printf(" %s", quoted->buffer);
		string_release(quoted);
	}
	printf(" %s", main_class);
	for (j = 0; j < options->ij_options.nr; j++) {
		struct string *quoted = quote_if_necessary(options->ij_options.list[j]);
		printf(" %s", quoted->buffer);
		string_release(quoted);
	}
	fputc('\n', stdout);
}

int file_is_newer(const char *path, const char *than)
{
	struct stat st1, st2;

	if (stat(path, &st1))
		return 0;
	return stat(than, &st2) || st1.st_mtime > st2.st_mtime;
}

int handle_one_option(int *i, const char *option, struct string *arg)
{
	string_set_length(arg, 0);
	if (!strcmp(main_argv[*i], option)) {
		if (++(*i) >= main_argc || !main_argv[*i])
			die("Option %s needs an argument!", option);
		string_append(arg, main_argv[*i]);
		return 1;
	}
	int len = strlen(option);
	if (!strncmp(main_argv[*i], option, len) && main_argv[*i][len] == '=') {
		string_append(arg, main_argv[*i] + len + 1);
		return 1;
	}
	return 0;
}

static int is_file_empty(const char *path)
{
	struct stat st;

	return !stat(path, &st) && !st.st_size;
}

static int update_files(struct string *relative_path)
{
	int len = relative_path->length, source_len, target_len;
	struct string *source = string_initf("%s/update%s",
		fiji_dir, relative_path->buffer), *target;
	DIR *directory = opendir(source->buffer);
	struct dirent *entry;

	if (!directory) {
		string_release(source);
		return 0;
	}
	target = string_copy(fiji_path(relative_path->buffer));
	if (mkdir_p(target->buffer)) {
		string_release(source);
		string_release(target);
		die("Could not create directory: %s", relative_path->buffer);
	}
	string_add_char(source, '/');
	source_len = source->length;
	string_add_char(target, '/');
	target_len = target->length;
	while (NULL != (entry = readdir(directory))) {
		const char *filename = entry->d_name;

		if (!strcmp(filename, ".") || !strcmp(filename, ".."))
			continue;

		string_set_length(relative_path, len);
		string_addf(relative_path, "/%s", filename);
		if (update_files(relative_path)) {
			continue;
		}

		string_set_length(source, source_len);
		string_append(source, filename);
		string_set_length(target, target_len);
		string_append(target, filename);

		if (is_file_empty(source->buffer)) {
			if (unlink(source->buffer))
				error("Could not remove %s", source);
			if (unlink(target->buffer))
				error("Could not remove %s", target);
			continue;
		}

#ifdef WIN32
		if (file_exists(target->buffer) && unlink(target->buffer)) {
			if (!strcmp(filename, "fiji.exe") || !strcmp(filename, "fiji-win32.exe") || !strcmp(filename, "fiji-win64.exe"))
				die("Could not remove old version of %s.  Please move %s to %s manually!", target->buffer, source->buffer, target->buffer);
			else
				die("Could not remove old version of %s.  Please remove it manually!", target->buffer);
		}
#endif
		if (rename(source->buffer, target->buffer))
			die("Could not move %s to %s: %s", source->buffer,
				target->buffer, strerror(errno));
	}
	closedir(directory);
	string_set_length(source, source_len - 1);
	rmdir(source->buffer);

	string_release(source);
	string_release(target);
	string_set_length(relative_path, len);

	return 1;
}

static void update_all_files(void)
{
	struct string *buffer = string_init(32);
	update_files(buffer);
	string_release(buffer);
}

static void __attribute__((__noreturn__)) usage(void)
{
	die("Usage: %s [<Java options>.. --] [<ImageJ options>..] [<files>..]\n"
		"\n"
		"Java options are passed to the Java Runtime, ImageJ\n"
		"options to ImageJ (or Jython, JRuby, ...).\n"
		"\n"
		"In addition, the following options are supported by Fiji:\n"
		"General options:\n"
		"--help, -h\n"
		"\tshow this help\n"
		"--dry-run\n"
		"\tshow the command line, but do not run anything\n"
		"--system\n"
		"\tdo not try to run bundled Java\n"
		"--java-home <path>\n"
		"\tspecify JAVA_HOME explicitly\n"
		"--print-java-home\n"
		"\tprint Fiji's idea of JAVA_HOME\n"
		"--print-fiji-dir\n"
		"\tprint where Fiji thinks it is located\n"
#ifdef WIN32
		"--console\n"
		"\talways open an error console\n"
#endif
		"--headless\n"
		"\trun in text mode\n"
		"--fiji-dir <path>\n"
		"\tset the fiji directory to <path> (used to find\n"
		"\t jars/, plugins/ and macros/)\n"
		"--heap, --mem, --memory <amount>\n"
		"\tset Java's heap size to <amount> (e.g. 512M)\n"
		"--class-path, --classpath, -classpath, --cp, -cp <path>\n"
		"\tappend <path> to the class path\n"
		"--jar-path, --jarpath, -jarpath <path>\n"
		"\tappend .jar files in <path> to the class path\n"
		"--ext <path>\n"
		"\tset Java's extension directory to <path>\n"
		"--default-gc\n"
		"\tdo not use advanced garbage collector settings by default\n"
		"\t(-Xincgc -XX:PermSize=128m)\n"
		"--gc-g1\n"
		"\tuse the G1 garbage collector\n"
		"--debug-gc\n"
		"\tshow debug info about the garbage collector on stderr\n"
		"\n"
		"Options for ImageJ:\n"
		"--allow-multiple\n"
		"\tdo not reuse existing ImageJ instance\n"
		"--plugins <dir>\n"
		"\tuse <dir> to discover plugins\n"
		"--run <plugin> [<arg>]\n"
		"\trun <plugin> in ImageJ, optionally with arguments\n"
		"--compile-and-run <path-to-.java-file>\n"
		"\tcompile and run <plugin> in ImageJ\n"
		"--edit <file>\n"
		"\tedit the given file in the script editor\n"
		"\n"
		"Options to run programs other than ImageJ:\n"
		"--jdb\n"
		"\tstart in JDB, the Java debugger\n"
		"--jython\n"
		"\tstart Jython instead of ImageJ (this is the\n"
		"\tdefault when called with a file ending in .py)\n"
		"--jruby\n"
		"\tstart JRuby instead of ImageJ (this is the\n"
		"\tdefault when called with a file ending in .rb)\n"
		"--clojure\n"
		"\tstart Clojure instead of ImageJ (this is the ""\n"
		"\tdefault when called with a file ending in .clj)\n"
		"--main-class <class name> (this is the\n"
		"\tdefault when called with a file ending in .class)\n"
		"--beanshell, --bsh\n"
		"\tstart BeanShell instead of ImageJ (this is the ""\n"
		"\tdefault when called with a file ending in .bs or .bsh)"
		"\n"
		"--main-class <class name> (this is the\n"
		"\tdefault when called with a file ending in .class)\n"
		"\tstart the given class instead of ImageJ\n"
		"--build\n"
		"\tstart Fiji's build instead of ImageJ\n"
		"--javac\n"
		"\tstart JavaC, the Java Compiler, instead of ImageJ\n"
		"--ant\n"
		"\trun Apache Ant\n"
		"--javap\n"
		"\tstart javap instead of ImageJ\n"
		"--javadoc\n"
		"\tstart javadoc instead of ImageJ\n"
		"--retrotranslator\n"
		"\tuse Retrotranslator to support Java < 1.6\n\n",
		main_argv[0]);
}

/* the maximal size of the heap on 32-bit systems, in megabyte */
#ifdef WIN32
#define MAX_32BIT_HEAP 1638
#else
#define MAX_32BIT_HEAP 1920
#endif

struct string *make_memory_option(size_t memory_size)
{
	return string_initf("-Xmx%dm", (int)(memory_size >> 20));
}

static void try_with_less_memory(size_t memory_size)
{
	char **new_argv;
	int i, j, found_dashdash;
	struct string *buffer;
	size_t subtract;

	/* Try again, with 25% less memory */
	if (memory_size < 0)
		return;
	memory_size >>= 20; // turn into megabytes
	subtract = memory_size >> 2;
	if (!subtract)
		return;
	memory_size -= subtract;

	buffer = string_initf("--mem=%dm", (int)memory_size);

	main_argc = main_argc_backup;
	main_argv = main_argv_backup;
	new_argv = (char **)xmalloc((3 + main_argc) * sizeof(char *));
	new_argv[0] = main_argv[0];

	j = 1;
	new_argv[j++] = xstrdup(buffer->buffer);

	// strip out --mem options
	found_dashdash = 0;
	for (i = 1; i < main_argc; i++) {
		struct string *dummy = string_init(32);
		if (!found_dashdash && !strcmp(main_argv_backup[i], "--"))
			found_dashdash = 1;
		if ((!found_dashdash || is_default_main_class(main_class)) &&
				(handle_one_option(&i, "--mem", dummy) ||
				 handle_one_option(&i, "--memory", dummy)))
			continue;
		new_argv[j++] = main_argv[i];
	}
	new_argv[j] = NULL;

	error("Trying with a smaller heap: %s", buffer->buffer);

#ifdef WIN32
	new_argv[0] = dos_path(new_argv[0]);
	for (i = 0; i < j; i++)
		new_argv[i] = quote_win32(new_argv[i]);
	execve(new_argv[0], (const char * const *)new_argv, NULL);
#else
	execve(new_argv[0], new_argv, NULL);
#endif

	string_setf(buffer, "ERROR: failed to launch (errno=%d;%s):\n",
		errno, strerror(errno));
	for (i = 0; i < j; i++)
		string_addf(buffer, "%s ", new_argv[i]);
	string_add_char(buffer, '\n');
#ifdef WIN32
	MessageBox(NULL, buffer->buffer, "Error executing Fiji", MB_OK);
#endif
	die("%s", buffer->buffer);
}

static int is_building(const char *target)
{
	int i;
	if (main_argc < 3 ||
			(strcmp(main_argv[1], "--build") &&
			 strcmp(main_argv[1], "--fake")))
		return 0;
	for (i = 2; i < main_argc; i++)
		if (!strcmp(main_argv[i], target))
			return 1;
	return 0;
}

static int retrotranslator;

static int start_ij(void)
{
	JavaVM *vm;
	struct options options;
	JavaVMInitArgs args;
	JNIEnv *env;
	struct string *buffer = string_init(32);
	struct string *buffer2 = string_init(32);
	struct string *class_path = string_init(32);
	struct string *ext_option = string_init(32);
	struct string *jvm_options = string_init(32);
	struct string *default_arguments = string_init(32);
	struct string *arg = string_init(32);
	struct string *plugin_path = string_init(32);
	int dashdash = 0;
	int allow_multiple = 0, skip_build_classpath = 0;
	int jdb = 0, add_class_path_option = 0, advanced_gc = 1, debug_gc = 0;
	size_t memory_size = 0;
	int count = 1, i;

#ifdef WIN32
#define EXE_EXTENSION ".exe"
#else
#define EXE_EXTENSION
#endif

	string_setf(buffer, "%s/fiji" EXE_EXTENSION, fiji_dir);
	string_setf(buffer2, "%s/fiji.c", fiji_dir);
	if (file_exists(fiji_path("fiji" EXE_EXTENSION)) &&
			file_is_newer(fiji_path("fiji.c"), fiji_path("fiji" EXE_EXTENSION)) &&
			!is_building("fiji"))
		error("Warning: your Fiji executable is not up-to-date");

	memset(&options, 0, sizeof(options));

#ifdef MACOSX
	// When double-clicked Finder adds a psn argument
	if (main_argc > 1 && ! strncmp(main_argv[1], "-psn_", 5)) {
		/*
		 * Reset main_argc so that ImageJ won't try to open
		 * that empty argument as a file (the root directory).
		 */
		main_argc = 1;
		/*
		 * Additionally, change directory to the fiji dir to emulate
		 * the behavior of the regular ImageJ application which does
		 * not start up in the filesystem root.
		 */
		chdir(fiji_dir);
	}

	if (!get_fiji_bundle_variable("heap", arg) ||
			!get_fiji_bundle_variable("mem", arg) ||
			!get_fiji_bundle_variable("memory", arg))
		memory_size = parse_memory(arg->buffer);
	if (!get_fiji_bundle_variable("system", arg) &&
			atol(arg->buffer) > 0)
		options.use_system_jvm++;
	if (get_fiji_bundle_variable("ext", ext_option))
		string_setf(ext_option, "%s/Home/lib/ext:"
			"/Library/Java/Extensions:"
			"/System/Library/Java/Extensions:"
			"/System/Library/Frameworks/JavaVM.framework/"
				"Home/lib/ext", get_java_home());
	if (!get_fiji_bundle_variable("allowMultiple", arg))
		allow_multiple = parse_bool(arg->buffer);
	get_fiji_bundle_variable("JVMOptions", jvm_options);
	get_fiji_bundle_variable("DefaultArguments", default_arguments);
#else
	read_file_as_string(fiji_path("jvm.cfg"), jvm_options);
#endif

	for (i = 1; i < main_argc; i++)
		if (!strcmp(main_argv[i], "--") && !dashdash)
			dashdash = count;
		else if (dashdash && main_class &&
				!is_default_main_class(main_class))
			main_argv[count++] = main_argv[i];
		else if (!strcmp(main_argv[i], "--dry-run"))
			options.debug++;
		else if (handle_one_option(&i, "--java-home", arg)) {
			absolute_java_home = xstrdup(arg->buffer);
			setenv_or_exit("JAVA_HOME", xstrdup(arg->buffer), 1);
		}
		else if (!strcmp(main_argv[i], "--system"))
			options.use_system_jvm++;
#ifdef WIN32
		else if (!strcmp(main_argv[i], "--console"))
			open_win_console();
#endif
		else if (!strcmp(main_argv[i], "--jdb")) {
			string_addf_path_list(class_path, "%s/../lib/tools.jar", get_jre_home());
			add_class_path_option = 1;
			jdb = 1;
		}
		else if (!strcmp(main_argv[i], "--allow-multiple"))
			allow_multiple = 1;
		else if (handle_one_option(&i, "--plugins", arg))
			string_addf(plugin_path, "-Dplugins.dir=%s", arg->buffer);
		else if (handle_one_option(&i, "--run", arg)) {
			string_replace(arg, '_', ' ');
			if (i + 1 < main_argc && main_argv[i + 1][0] != '-')
				string_addf(arg, "\", \"%s", main_argv[++i]);
			add_option(&options, "-eval", 1);
			string_setf(buffer, "run(\"%s\");", arg->buffer);
			add_option_string(&options, buffer, 1);
			headless_argc++;
		}
		else if (handle_one_option(&i, "--compile-and-run", arg)) {
			add_option(&options, "-eval", 1);
			string_setf(buffer, "run(\"Refresh Javas\", \"%s \");",
				make_absolute_path(arg->buffer));
			add_option_string(&options, buffer, 1);
			headless_argc++;
		}
		else if (handle_one_option(&i, "--edit", arg))
			for (;;) {
				add_option(&options, "-eval", 1);
				string_setf(buffer, "run(\"Script Editor\", \"%s\");", *arg->buffer && strncmp(arg->buffer, "class:", 6) ? make_absolute_path(arg->buffer) : arg->buffer);
				add_option_string(&options, buffer, 1);
				if (i + 1 >= main_argc)
					break;
				string_setf(arg, "%s", main_argv[++i]);
			}
		else if (handle_one_option(&i, "--heap", arg) ||
				handle_one_option(&i, "--mem", arg) ||
				handle_one_option(&i, "--memory", arg))
			memory_size = parse_memory(arg->buffer);
		else if (!strcmp(main_argv[i], "--headless")) {
			headless = 1;
			/* handle "--headless script.ijm" gracefully */
			if (i + 2 == main_argc && main_argv[i + 1][0] != '-')
				dashdash = count;
		}
		else if (!strcmp(main_argv[i], "--jython")) {
			main_class = "org.python.util.jython";
			/* When running on Debian / Ubuntu we depend on the
			   external version of jython, so add its jar: */
			string_append_path_list(class_path, "/usr/share/java/jython.jar");
		}
		else if (!strcmp(main_argv[i], "--jruby"))
			main_class = "org.jruby.Main";
		else if (!strcmp(main_argv[i], "--clojure")) {
			main_class = "clojure.lang.Repl";
			/* When running on Debian / Ubuntu we depend on the
			   external version of clojure, so add its jar: */
			string_append_path_list(class_path, "/usr/share/java/clojure.jar");
		} else if (!strcmp(main_argv[i], "--beanshell") ||
			   !strcmp(main_argv[i], "--bsh")) {
			main_class = "bsh.Interpreter";
			/* When running on Debian / Ubuntu we depend on the
			   external version of beanshell, so add its jar: */
			string_append_path_list(class_path, "/usr/share/java/bsh.jar");
		}
		else if (handle_one_option(&i, "--main-class", arg)) {
			string_append_path_list(class_path, ".");
			main_class = xstrdup(arg->buffer);
		}
		else if (handle_one_option(&i, "--jar", arg)) {
			string_addf_path_list(class_path, "%s", arg->buffer);
			main_class = "fiji.JarLauncher";
			add_option_string(&options, arg, 1);
		}
		else if (handle_one_option(&i, "--class-path", arg) ||
				handle_one_option(&i, "--classpath", arg) ||
				handle_one_option(&i, "-classpath", arg) ||
				handle_one_option(&i, "--cp", arg) ||
				handle_one_option(&i, "-cp", arg))
			string_addf_path_list(class_path, "%s", arg->buffer);
		else if (handle_one_option(&i, "--jar-path", arg) ||
				handle_one_option(&i, "--jarpath", arg) ||
				handle_one_option(&i, "-jarpath", arg)) {
			struct string *jars = string_init(32);
			build_classpath_for_string(jars, arg, 0);
			if (jars->length)
				string_addf_path_list(class_path, "%s", jars->buffer);
			string_release(jars);
		}
		else if (handle_one_option(&i, "--ext", arg)) {
			string_append_path_list(ext_option, arg->buffer);
		}
		else if (!strcmp(main_argv[i], "--build") ||
				!strcmp(main_argv[i], "--fake")) {
			const char *fake_jar, *precompiled_fake_jar;
#ifdef WIN32
			open_win_console();
#endif
			skip_build_classpath = 1;
			headless = 1;
			fake_jar = fiji_path("jars/fake.jar");
			precompiled_fake_jar = fiji_path("precompiled/fake.jar");
			if (run_precompiled || !file_exists(fake_jar) ||
					file_is_newer(precompiled_fake_jar, fake_jar))
				fake_jar = precompiled_fake_jar;
			if (file_is_newer(fiji_path("src-plugins/fake/fiji/build/Fake.java"), fake_jar) &&
					!is_building("jars/fake.jar"))
				error("Warning: jars/fake.jar is not up-to-date");
			string_addf_path_list(class_path, "%s", fake_jar);
			main_class = "fiji.build.Fake";
		}
		else if (!strcmp(main_argv[i], "--javac") ||
				!strcmp(main_argv[i], "--javap") ||
				!strcmp(main_argv[i], "--javadoc")) {
			add_class_path_option = 1;
			headless = 1;
			if (!strcmp(main_argv[i], "--javac")) {
				const char *javac;

				javac = fiji_path("jars/javac.jar");
				if (run_precompiled || !file_exists(javac))
					string_append_path_list(class_path, fiji_path("precompiled/javac.jar"));
				else
					string_append_path_list(class_path, javac);
			}
			string_addf_path_list(class_path, "%s/../lib/tools.jar", get_jre_home());
			if (!strcmp(main_argv[i], "--javac"))
				main_class = "com.sun.tools.javac.Main";
			else if (!strcmp(main_argv[i], "--javap"))
				main_class = "sun.tools.javap.Main";
			else if (!strcmp(main_argv[i], "--javadoc"))
				main_class = "com.sun.tools.javadoc.Main";
			else
				die("Unknown tool: %s", main_argv[i]);
		}
		else if (!strcmp(main_argv[i], "--ant")) {
			main_class = "org.apache.tools.ant.Main";
			string_addf_path_list(class_path, "%s/../lib/tools.jar", get_jre_home());
			/* When running on Debian / Ubuntu we depend on the
			   external version of ant, so add those jars too: */
			string_append_path_list(class_path, "/usr/share/java/ant.jar");
			string_append_path_list(class_path, "/usr/share/java/ant-launcher.jar");
			string_append_path_list(class_path, "/usr/share/java/ant-nodeps.jar");
		}
		else if (!strcmp(main_argv[i], "--retrotranslator") ||
				!strcmp(main_argv[i], "--retro"))
			retrotranslator = 1;
		else if (handle_one_option(&i, "--fiji-dir", arg))
			fiji_dir = xstrdup(arg->buffer);
		else if (!strcmp("--print-fiji-dir", main_argv[i])) {
			printf("%s\n", fiji_dir);
			exit(0);
		}
		else if (!strcmp("--print-java-home", main_argv[i])) {
			printf("%s\n", get_java_home());
			exit(0);
		}
		else if (!strcmp("--default-gc", main_argv[i]))
			advanced_gc = 0;
		else if (!strcmp("--gc-g1", main_argv[i]) ||
				!strcmp("--g1", main_argv[i]))
			advanced_gc = 2;
		else if (!strcmp("--debug-gc", main_argv[i]))
			debug_gc = 1;
		else if (!strcmp("--help", main_argv[i]) ||
				!strcmp("-h", main_argv[i]))
			usage();
		else
			main_argv[count++] = main_argv[i];

	main_argc = count;

	if (!headless &&
#ifdef MACOSX
			!getenv("SECURITYSESSIONID") && !getenv("DISPLAY")
#elif defined(__linux__)
			!getenv("DISPLAY")
#else
			0
#endif
			) {
		error("No GUI detected.  Falling back to headless mode.");
		headless = 1;
	}

	if (ext_option->length) {
		string_setf(buffer, "-Djava.ext.dirs=%s", ext_option->buffer);
		add_option_string(&options, buffer, 0);
	}

	/* Avoid Jython's huge startup cost: */
	add_option(&options, "-Dpython.cachedir.skip=true", 0);
	if (!plugin_path->length)
		string_setf(plugin_path, "-Dplugins.dir=%s", fiji_dir);
	add_option(&options, plugin_path->buffer, 0);

	// if arguments don't set the memory size, set it after available memory
	if (memory_size == 0) {
		memory_size = get_memory_size(0);
		/* 0.75x, but avoid multiplication to avoid overflow */
		memory_size -= memory_size >> 2;
		if (sizeof(void *) == 4 &&
				(memory_size >> 20) > MAX_32BIT_HEAP)
			memory_size = (MAX_32BIT_HEAP << 20);
	}

	if (memory_size > 0)
		add_option(&options, make_memory_option(memory_size)->buffer, 0);

	if (headless)
		add_option(&options, "-Djava.awt.headless=true", 0);

	if (is_ipv6_broken())
		add_option(&options, "-Djava.net.preferIPv4Stack=true", 0);

	if (advanced_gc == 1) {
		add_option(&options, "-Xincgc", 0);
		add_option(&options, "-XX:PermSize=128m", 0);
	}
	else if (advanced_gc == 2) {
		add_option(&options, "-XX:PermSize=128m", 0);
		add_option(&options, "-XX:+UseCompressedOops", 0);
		add_option(&options, "-XX:+UnlockExperimentalVMOptions", 0);
		add_option(&options, "-XX:+UseG1GC", 0);
		add_option(&options, "-XX:+G1ParallelRSetUpdatingEnabled", 0);
		add_option(&options, "-XX:+G1ParallelRSetScanningEnabled", 0);
		add_option(&options, "-XX:NewRatio=5", 0);
	}

	if (debug_gc)
		add_option(&options, "-verbose:gc", 0);

	if (!main_class) {
		int index = dashdash ? dashdash : 1;
		const char *first = main_argv[index];
		int len = main_argc > index ? strlen(first) : 0;

		if (len > 1 && !strncmp(first, "--", 2))
			len = 0;
		if (len > 3 && !strcmp(first + len - 3, ".py"))
			main_class = "org.python.util.jython";
		else if (len > 3 && !strcmp(first + len - 3, ".rb"))
			main_class = "org.jruby.Main";
		else if (len > 4 && !strcmp(first + len - 4, ".clj"))
			main_class = "clojure.lang.Script";
		else if ((len > 4 && !strcmp(first + len - 4, ".bsh")) ||
				(len > 3 && !strcmp(first + len - 3, ".bs")))
			main_class = "bsh.Interpreter";
		else if (len > 6 && !strcmp(first + len - 6, ".class")) {
			struct string *dotted = string_copy(first);
			string_append_path_list(class_path, ".");
			string_replace(dotted, '/', '.');
			string_set_length(dotted, len - 6);
			main_class = xstrdup(dotted->buffer);
			main_argv++;
			main_argc--;
			string_release(dotted);
		}
		else
			main_class = default_main_class;
	}

	maybe_reexec_with_correct_lib_path();

	if (retrotranslator && build_classpath(class_path, fiji_path("retro"), 0))
		return 1;

	/* set up class path */
	if (skip_build_classpath) {
		/* strip trailing ":" */
		int len = class_path->length;
		if (len > 0 && class_path->buffer[len - 1] == PATH_SEP[0])
			string_set_length(class_path, len - 1);
	}
	else {
		if (headless)
			string_append_path_list(class_path, fiji_path("misc/headless.jar"));

		if (is_default_main_class(main_class)) {
			update_all_files();
			string_append_path_list(class_path, fiji_path("jars/Fiji.jar"));
			string_append_path_list(class_path, fiji_path("jars/ij.jar"));
		}
		else {
			if (build_classpath(class_path, fiji_path("plugins"), 0))
				return 1;
			build_classpath(class_path, fiji_path("jars"), 0);
		}
	}
	if (class_path->length) {
		string_setf(buffer, "-Djava.class.path=%s", class_path->buffer);
		add_option_string(&options, buffer, 0);
	}

	if (jvm_options->length)
		add_options(&options, jvm_options->buffer, 0);
	if (default_arguments->length)
		add_options(&options, default_arguments->buffer, 1);

	if (dashdash) {
		for (i = 1; i < dashdash; i++)
			add_option(&options, main_argv[i], 0);
		main_argv += dashdash - 1;
		main_argc -= dashdash - 1;
	}

	if (add_class_path_option) {
		add_option(&options, "-classpath", 1);
		add_option_copy(&options, class_path->buffer, 1);
	}

	if (!strcmp(main_class, "org.apache.tools.ant.Main"))
		add_java_home_to_path();

	if (jdb)
		add_option_copy(&options, main_class, 1);
	if (is_default_main_class(main_class)) {
		if (allow_multiple)
			add_option(&options, "-port0", 1);
		else
			add_option(&options, "-port7", 1);
		add_option(&options, "-Dsun.java.command=Fiji", 0);

		update_all_files();
	}

	/* handle "--headless script.ijm" gracefully */
	if (headless && is_default_main_class(main_class)) {
		if (main_argc + headless_argc < 2) {
			error("--headless without a parameter?");
			if (!options.debug)
				exit(1);
		}
		if (main_argc > 1 && *main_argv[1] != '-')
			add_option(&options, "-batch", 1);
	}

	if (jdb)
		main_class = "com.sun.tools.example.debug.tty.TTY";

	if (retrotranslator) {
		add_option(&options, "-advanced", 1);
		add_option_copy(&options, main_class, 1);
		main_class =
			"net.sf.retrotranslator.transformer.JITRetrotranslator";
	}

	for (i = 1; i < main_argc; i++)
		add_option(&options, main_argv[i], 1);

	const char *properties[] = {
		"fiji.dir", fiji_dir,
		"fiji.defaultLibPath", JAVA_LIB_PATH,
		"fiji.executable", main_argv0,
		NULL
	};

	if (options.debug) {
		for (i = 0; properties[i]; i += 2) {
			string_setf(buffer, "-D%s=%s", properties[i], properties[i + 1]);
			add_option_string(&options, buffer, 0);
		}

		show_commandline(&options);
		exit(0);
	}

	memset(&args, 0, sizeof(args));
	/* JNI_VERSION_1_4 is used on Mac OS X to indicate 1.4.x and later */
	args.version = JNI_VERSION_1_4;
	args.options = prepare_java_options(&options.java_options);
	args.nOptions = options.java_options.nr;
	args.ignoreUnrecognized = JNI_FALSE;

	if (options.use_system_jvm)
		env = NULL;
	else {
		int result = create_java_vm(&vm, (void **)&env, &args);
		if (result == JNI_ENOMEM) {
			try_with_less_memory(memory_size);
			die("Out of memory!");
		}
		if (result) {
			if (result != 2)
				error("Warning: falling back to System JVM");
			env = NULL;
		} else {
			string_setf(buffer, "-Djava.home=%s", get_java_home());
			prepend_string_copy(&options.java_options, buffer->buffer);
		}
	}

	if (env) {
		jclass instance;
		jmethodID method;
		jobjectArray args;
		struct string *slashed = string_copy(main_class);

		for (i = 0; properties[i]; i += 2)
			set_property(env, properties[i], properties[i + 1]);

		string_replace(slashed, '.', '/');
		if (!(instance = (*env)->FindClass(env, slashed->buffer))) {
			(*env)->ExceptionDescribe(env);
			die("Could not find %s", slashed->buffer);
		}
		else if (!(method = (*env)->GetStaticMethodID(env, instance,
				"main", "([Ljava/lang/String;)V"))) {
			(*env)->ExceptionDescribe(env);
			die("Could not find main method of %s", slashed->buffer);
		}
		string_release(slashed);

		args = prepare_ij_options(env, &options.ij_options);
		(*env)->CallStaticVoidMethodA(env, instance,
				method, (jvalue *)&args);
		if ((*vm)->DetachCurrentThread(vm))
			error("Could not detach current thread");
		/* This does not return until ImageJ exits */
		(*vm)->DestroyJavaVM(vm);
	} else {
		/* fall back to system-wide Java */
		const char *java_home_env;
#ifdef MACOSX
		struct string *icon_option;
		/*
		 * On MacOSX, one must (stupidly) fork() before exec() to
		 * clean up some pthread state somehow, otherwise the exec()
		 * will fail with "Operation not supported".
		 */
		if (fork())
			exit(0);

		add_option(&options, "-Xdock:name=Fiji", 0);
		icon_option = string_copy("-Xdock:icon=");
		append_icon_path(icon_option);
		add_option_string(&options, icon_option, 0);
		string_release(icon_option);
#endif

		for (i = 0; properties[i]; i += 2) {
			string_setf(buffer, "-D%s=%s", properties[i], properties[i + 1]);
			add_option_string(&options, buffer, 0);
		}

		/* fall back to system-wide Java */
		add_option_copy(&options, main_class, 0);
		append_string_array(&options.java_options, &options.ij_options);
		append_string(&options.java_options, NULL);
		prepend_string(&options.java_options, "java");

		string_set(buffer, "java");
		java_home_env = getenv("JAVA_HOME");
		if (java_home_env && strlen(java_home_env) > 0) {
			error("Found that JAVA_HOME was: '%s'", java_home_env);
			string_setf(buffer, "%s/bin/java", java_home_env);
		}
		options.java_options.list[0] = buffer->buffer;
#ifndef WIN32
		if (execvp(buffer->buffer, options.java_options.list))
			error("Could not launch system-wide Java (%s)", strerror(errno));
#else
		if (console_opened)
			sleep(5); // sleep 5 seconds

		FreeConsole(); // java.exe cannot reuse the console anyway
		for (i = 0; i < options.java_options.nr - 1; i++)
			options.java_options.list[i] =
				quote_win32(options.java_options.list[i]);
		execvp(buffer->buffer, (const char * const *)options.java_options.list);
		char message[16384];
		int off = sprintf(message, "Error: '%s' while executing\n\n",
				strerror(errno));
		for (i = 0; options.java_options.list[i]; i++)
			off += sprintf(message + off, "'%s'\n",
					options.java_options.list[i]);
		MessageBox(NULL, message, "Error", MB_OK);
#endif
		exit(1);
	}
	return 0;
}

#ifdef MACOSX
static void append_icon_path(struct string *str)
{
	/*
	 * Check if we're launched from within an Application bundle or
	 * command line.  If from a bundle, Fiji.app should be in the path.
	 */
	string_append(str, fiji_path(!suffixcmp(fiji_dir, strlen(fiji_dir), "Fiji.app") ?
		"Contents/Resources/Fiji.icns" : "images/Fiji.icns"));
}

#include <sys/types.h>
#include <sys/sysctl.h>
#include <mach/machine.h>
#include <unistd.h>
#include <sys/param.h>
#include <string.h>

static int is_intel(void)
{
	int mib[2] = { CTL_HW, HW_MACHINE };
	char result[128];
	size_t len = sizeof(result);;

	if (sysctl(mib, 2, result, &len, NULL, 0) < 0)
		return 0;
	return !strcmp(result, "i386");
}

static void set_path_to_JVM(void)
{
	/*
	 * MacOSX specific stuff for system java
	 * -------------------------------------
	 * Non-macosx works but places java into separate pid,
	 * which causes all kinds of strange behaviours (app can
	 * launch multiple times, etc).
	 *
	 * Search for system wide java >= 1.5
	 * and if found, launch Fiji with the system wide java.
	 * This is an adaptation from simple.c from Apple's
	 * simpleJavaLauncher code.
	 */

	/* Look for the JavaVM bundle using its identifier. */
	CFBundleRef JavaVMBundle =
		CFBundleGetBundleWithIdentifier(CFSTR("com.apple.JavaVM"));

	if (!JavaVMBundle)
		return;

	/* Get a path for the JavaVM bundle. */
	CFURLRef JavaVMBundleURL = CFBundleCopyBundleURL(JavaVMBundle);
	CFRelease(JavaVMBundle);
	if (!JavaVMBundleURL)
		return;

	/* Append to the path the Versions Component. */
	CFURLRef JavaVMBundlerVersionsDirURL =
		CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,
				JavaVMBundleURL, CFSTR("Versions"), 1);
	CFRelease(JavaVMBundleURL);
	if (!JavaVMBundlerVersionsDirURL)
		return;

	/* Append to the path the target JVM's Version. */
	CFURLRef TargetJavaVM = NULL;
	CFStringRef targetJVM; // Minimum Java5

	/* TODO: disable this test on 10.6+ */
	/* Try 1.6 only with 64-bit */
	if (is_intel() && sizeof(void *) > 4) {
		targetJVM = CFSTR("1.6");
		TargetJavaVM =
		CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,
				JavaVMBundlerVersionsDirURL, targetJVM, 1);
	}

	if (!TargetJavaVM) {
		targetJVM = CFSTR("1.5");
		TargetJavaVM =
		CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,
				JavaVMBundlerVersionsDirURL, targetJVM, 1);
	}

	CFRelease(JavaVMBundlerVersionsDirURL);
	if (!TargetJavaVM)
		return;

	UInt8 pathToTargetJVM[PATH_MAX] = "";
	Boolean result = CFURLGetFileSystemRepresentation(TargetJavaVM, 1,
				pathToTargetJVM, PATH_MAX);
	CFRelease(TargetJavaVM);
	if (!result)
		return;

	/*
	 * Check to see if the directory, or a symlink for the target
	 * JVM directory exists, and if so set the environment
	 * variable JAVA_JVM_VERSION to the target JVM.
	 */
	if (access((const char *)pathToTargetJVM, R_OK))
		return;

	/*
	 * Ok, the directory exists, so now we need to set the
	 * environment var JAVA_JVM_VERSION to the CFSTR targetJVM.
	 *
	 * We can reuse the pathToTargetJVM buffer to set the environment
	 * varable.
	 */
	if (CFStringGetCString(targetJVM, (char *)pathToTargetJVM,
				PATH_MAX, kCFStringEncodingUTF8))
		setenv("JAVA_JVM_VERSION",
				(const char *)pathToTargetJVM, 1);

}

static int get_fiji_bundle_variable(const char *key, struct string *value)
{
	/*
	 * Reading the command line options from the Info.plist file in the
	 * Application bundle.
	 *
	 * This routine expects a separate dictionary for fiji with the
	 * options from the command line as keys.
	 *
	 * If Info.plist is not present (i.e. if started from the cmd-line),
	 * the whole thing will be just skipped.
	 *
	 * Example: Setting the java heap to 1024m
	 * <key>fiji</key>
	 * <dict>
	 *	<key>heap</key>
	 *	<string>1024</string>
	 * </dict>
	 */

	static CFDictionaryRef fijiInfoDict;
	static int initialized = 0;

	if (!initialized) {
		initialized = 1;

		/* Get the main bundle for the app. */
		CFBundleRef fijiBundle = CFBundleGetMainBundle();
		if (!fijiBundle)
			return -1;

		/* Get an instance of the non-localized keys. */
		CFDictionaryRef bundleInfoDict =
			CFBundleGetInfoDictionary(fijiBundle);
		if (!bundleInfoDict)
			return -2;

		fijiInfoDict = (CFDictionaryRef)
			CFDictionaryGetValue(bundleInfoDict, CFSTR("fiji"));
	}

	if (!fijiInfoDict)
		return -3;

	CFStringRef key_ref =
		CFStringCreateWithCString(NULL, key,
			kCFStringEncodingMacRoman);
	if (!key_ref)
		return -4;

	CFStringRef propertyString = (CFStringRef)
		CFDictionaryGetValue(fijiInfoDict, key_ref);
	CFRelease(key_ref);
	if (!propertyString)
		return -5;

	string_set_length(value, 0);
	string_append(value, CFStringGetCStringPtr(propertyString,
			kCFStringEncodingMacRoman));

	return 0;
}

/* MacOSX needs to run Java in a new thread, AppKit in the main thread. */

static void dummy_call_back(void *info) {
}

static void *start_ij_aux(void *dummy)
{
	exit(start_ij());
}

static int start_ij_macosx(void)
{
	struct string *env_key, *icon_path;

	/* set the Application's name */
	env_key = string_initf("APP_NAME_%d", (int)getpid());
	setenv(env_key->buffer, "Fiji", 1);

	/* set the Dock icon */
	string_setf(env_key, "APP_ICON_%d", (int)getpid());
	icon_path = string_init(32);
	append_icon_path(icon_path);
	setenv(env_key->buffer, icon_path->buffer, 1);

	string_release(env_key);
	string_release(icon_path);

	pthread_t thread;
	pthread_attr_t attr;
	pthread_attr_init(&attr);
	pthread_attr_setscope(&attr, PTHREAD_SCOPE_SYSTEM);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

	/* Start the thread that we will start the JVM on. */
	pthread_create(&thread, &attr, start_ij_aux, NULL);
	pthread_attr_destroy(&attr);

	CFRunLoopSourceContext context;
	memset(&context, 0, sizeof(context));
	context.perform = &dummy_call_back;

	CFRunLoopSourceRef ref = CFRunLoopSourceCreate(NULL, 0, &context);
	CFRunLoopAddSource (CFRunLoopGetCurrent(), ref, kCFRunLoopCommonModes);
	CFRunLoopRun();

	return 0;
}
#define start_ij start_ij_macosx

/*
 * Them stupid Apple software designers -- in their infinite wisdom -- added
 * 64-bit support to Tiger without really supporting it.
 *
 * As a consequence, a universal binary will be executed in 64-bit mode on
 * a x86_64 machine, even if neither CoreFoundation nor Java can be linked,
 * and sure enough, the executable will crash.
 *
 * It does not even reach main(), so we have to have a binary that does _not_
 * provide 64-bit support, detect if it is actually on Leopard, and execute
 * another binary in that case that _does_ provide 64-bit support, even if
 * we'd rather meet the Apple software designers some night, with a baseball
 * bat in our hands, than execute an innocent binary that is not to blame.
 */
static int is_osrelease(int min)
{
	int mib[2] = { CTL_KERN, KERN_OSRELEASE };
	char os_release[128];
	size_t len = sizeof(os_release);;

	return sysctl(mib, 2, os_release, &len, NULL, 0) != -1 &&
		atoi(os_release) >= min;
}

static int is_leopard(void)
{
	return is_osrelease(9);
}

static int is_tiger(void)
{
	return is_osrelease(8);
}

static int launch_32bit_on_tiger(int argc, char **argv)
{
	const char *match, *replace;

	if (is_intel() && is_leopard()) {
		match = "-tiger";
		replace = "-macosx";
	}
	else { /* Tiger */
		if (!is_tiger())
			retrotranslator = 1;
		match = "-macosx";
		replace = "-tiger";
		if (sizeof(void *) < 8)
			return 0; /* already 32-bit, everything's fine */
	}

	int offset = strlen(argv[0]) - strlen(match);
	if (offset < 0 || strcmp(argv[0] + offset, match))
		return 0; /* suffix not found, no replacement */

	if (strlen(replace) > strlen(match)) {
		char *buffer = (char *)xmalloc(offset + strlen(replace) + 1);
		memcpy(buffer, argv[0], offset);
		argv[0] = buffer;
	}
	strcpy(argv[0] + offset, replace);
	execv(argv[0], argv);
	fprintf(stderr, "Could not execute %s: %d(%s)\n",
		argv[0], errno, strerror(errno));
	exit(1);
}
#endif

static void find_newest(struct string *relative_path, int max_depth, const char *file, struct string *result)
{
	int len = relative_path->length;
	DIR *directory;
	struct dirent *entry;

	string_add_char(relative_path, '/');

	string_append(relative_path, file);
	if (file_exists(fiji_path(relative_path->buffer))) {
		string_set_length(relative_path, len);
		if (!result->length || file_is_newer(fiji_path(relative_path->buffer), fiji_path(result->buffer)))
			string_set(result, relative_path->buffer);
	}

	if (max_depth <= 0)
		return;

	string_set_length(relative_path, len);
	directory = opendir(fiji_path(relative_path->buffer));
	if (!directory)
		return;
	string_add_char(relative_path, '/');
	while (NULL != (entry = readdir(directory))) {
		if (entry->d_name[0] == '.')
			continue;
		string_append(relative_path, entry->d_name);
		if (dir_exists(fiji_path(relative_path->buffer)))
			find_newest(relative_path, max_depth - 1, file, result);
		string_set_length(relative_path, len + 1);
	}
	string_set_length(relative_path, len);
}

static void adjust_java_home_if_necessary(void)
{
	struct string *result, *buffer, *jre_path;
#ifdef MACOSX
	/* On MacOSX, we use the system Java anyway. */
	return;
#endif
	buffer = string_copy("java");
	result = string_init(32);
	jre_path = string_initf("jre/%s", library_path);

	find_newest(buffer, 2, jre_path->buffer, result);
	if (result->length) {
		string_append(result, "/jre");
		relative_java_home = xstrdup(result->buffer);
	}
	else {
		find_newest(buffer, 3, library_path, buffer);
		if (result->length)
			relative_java_home = xstrdup(result->buffer);
	}
	string_release(buffer);
	string_release(result);
	string_release(jre_path);
}

int main(int argc, char **argv, char **e)
{
	int size;

#if defined(MACOSX)
	launch_32bit_on_tiger(argc, argv);
#elif defined(WIN32)
	int len;
#ifdef WIN64
	/* work around MinGW64 breakage */
	argc = __argc;
	argv = __argv;
	argv[0] = _pgmptr;
#endif
	len = strlen(argv[0]);
	if (!suffixcmp(argv[0], len, "fiji.exe") ||
			!suffixcmp(argv[0], len, "fiji"))
		open_win_console();
#endif
	fiji_dir = get_fiji_dir(argv[0]);
	adjust_java_home_if_necessary();
	main_argv0 = argv[0];
	main_argv = argv;
	main_argc = argc;

	/* save arguments in case we have to try with a smaller heap */
	size = (argc + 1) * sizeof(char *);
	main_argv_backup = (char **)xmalloc(size);
	memcpy(main_argv_backup, main_argv, size);
	main_argc_backup = argc;

	return start_ij();
}
