#define _BSD_SOURCE
#include <stdlib.h>
#include "jni.h"
#include <stdlib.h>
#include <limits.h>
#include <iostream>
#include <string.h>
using std::cerr;
using std::cout;
using std::endl;
using std::ostream;

#include <string>
using std::string;

#include <sstream>
using std::stringstream;

#include <fstream>
using std::ifstream;

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>

#ifdef MACOSX
#include <stdlib.h>
#include <pthread.h>
#include <CoreFoundation/CoreFoundation.h>

static void append_icon_path(string &str);
static void set_path_to_JVM(void);
static int get_fiji_bundle_variable(const char *key, string &value);
#endif

#include <algorithm>
using std::replace;

#ifdef WIN32
#include <io.h>
#include <process.h>
#define PATH_SEP ";"

static void open_win_console();

class win_cerr
{
	public:
		template<class T>
		ostream& operator<<(T t) {
			open_win_console();
			return cerr << t;
		}

		ostream& operator<<(std::ostream &(*manip)(std::ostream &s)) {
			open_win_console();
			return cerr << manip;
		}
};

static win_cerr fake_cerr;
#define cerr fake_cerr

class win_cout
{
	public:
		template<class T>
		ostream& operator<<(T t) {
			open_win_console();
			return cout << t;
		}

		ostream& operator<<(std::ostream &(*manip)(std::ostream &s)) {
			open_win_console();
			return cout << manip;
		}
};

static win_cout fake_cout;
#define cout fake_cout

#else
#define PATH_SEP ":"
#endif

/*
 * If set, overrides the environment variable JAVA_HOME, which in turn
 * overrides relative_java_home.
 */
string absolute_java_home;
static const char *relative_java_home = JAVA_HOME;
static const char *library_path = JAVA_LIB_PATH;
static const char *default_main_class = "fiji.Main";

static bool is_default_main_class(const char *name)
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
	if (!overwrite && getenv(name))
		return 0;
	if ((!name) || (!value))
		return 0;
	stringstream p;
	p << name << "=" << value;
	return putenv(strdup(p.str().c_str()));
}

// Similarly we can do the same for unsetenv:
static int unsetenv(const char *name)
{
	stringstream p;
	p << name << "=";
	return putenv(strdup(p.str().c_str()));
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
		if (result) {
			cerr << "Unsetting environment variable " <<
				name << "failed" << endl;
			exit(1);
		}
#endif
		return;
	}
	result = setenv(name, value, overwrite);
	if (result) {
		cerr << "Setting environment variable " << name <<
			" to " << value << " failed" << endl;
		exit(1);
	}
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

#ifdef MACOSX
static bool parse_bool(string &value)
{
	return value != "0" && value != "false" &&
		value != "False" && value != "FALSE";
}
#endif

/* work around a SuSE IPv6 setup bug */

#ifdef IPV6_MAYBE_BROKEN
#include <netinet/ip6.h>
#include <fcntl.h>
#endif

static bool is_ipv6_broken(void)
{
#ifndef IPV6_MAYBE_BROKEN
	return false;
#else
	int sock = socket(AF_INET6, SOCK_STREAM, 0);
	struct sockaddr_in6 address = {
		AF_INET6, 57294 + 7, 0, in6addr_loopback, 0
	};
	long flags;

	if (sock < 0)
		return true;

	flags = fcntl(sock, F_GETFL, NULL);
	if (fcntl(sock, F_SETFL, flags | O_NONBLOCK) < 0) {
		close(sock);
		return true;
	}
	

	bool result = false;
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
					result = true;
				else
					result = (error == EACCES) |
						(error == EPERM) |
						(error == EAFNOSUPPORT) |
						(error == EINPROGRESS);
			} else
				result = true;
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
char *main_argv0;
char **main_argv, **main_argv_backup;
int main_argc, main_argc_backup;
const char *main_class;
bool run_precompiled = false;

static bool dir_exists(string directory);

static string get_java_home(void)
{
	if (absolute_java_home != "")
		return absolute_java_home;
	const char *env = getenv("JAVA_HOME");
	if (env) {
		if (dir_exists(string(env)))
			return env;
		else {
			cerr << "Ignoring invalid JAVA_HOME: " << env << endl;
			unsetenv("JAVA_HOME");
		}
	}
	return string(fiji_dir) + "/" + relative_java_home;
}

static string get_jre_home(void)
{
	string result = get_java_home();
	int len = result.length();
	if (len > 4 && result.substr(len - 4) == "/jre")
		return result;
	return dir_exists(result + "/jre") ? result + "/jre" : result;
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

	if (mystrlcpy(buf, path, PATH_MAX) >= PATH_MAX) {
		cerr << "Too long path: " << path << endl;
		exit(1);
	}

	while (depth--) {
		if (stat(buf, &st) || !S_ISDIR(st.st_mode)) {
			const char *slash = last_slash(buf);
			if (slash) {
				buf[slash-buf] = '\0';
				last_elem = strdup(slash + 1);
			} else {
				last_elem = strdup(buf);
				*buf = '\0';
			}
		}

		if (*buf) {
			if (!*cwd && !getcwd(cwd, sizeof(cwd))) {
				cerr << "Could not get current working dir"
					<< endl;
				exit(1);
			}

			if (chdir(buf)) {
				cerr << "Could not switch to " << buf << endl;
				exit(1);
			}
		}
		if (!getcwd(buf, PATH_MAX)) {
			cerr << "Could not get current working directory"
				<< endl;
			exit(1);
		}

		if (last_elem) {
			int len = strlen(buf);
			if (len + strlen(last_elem) + 2 > PATH_MAX) {
				cerr << "Too long path name: "
					<< buf << "/" << last_elem << endl;
				exit(1);
			}
			buf[len] = '/';
			strcpy(buf + len + 1, last_elem);
			free(last_elem);
			last_elem = NULL;
		}

#ifndef WIN32
		if (!lstat(buf, &st) && S_ISLNK(st.st_mode)) {
			len = readlink(buf, next_buf, PATH_MAX);
			if (len < 0) {
				cerr << "Invalid symlink: " << buf << endl;
				exit(1);
			}
			next_buf[len] = '\0';
			buf = next_buf;
			buf_index = 1 - buf_index;
			next_buf = bufs[buf_index];
		} else
#endif
			break;
	}

	if (*cwd && chdir(cwd)) {
		cerr << "Could not change back to " << cwd << endl;
		exit(1);
	}

	return buf;
}

static bool is_absolute_path(const char *path)
{
#ifdef WIN32
	if (((path[0] >= 'A' && path[0] <= 'Z') ||
			(path[0] >= 'a' && path[0] <= 'z')) && path[1] == ':')
		return true;
#endif
	return path[0] == '/';
}

bool file_exists(string path)
{
	ifstream test(path.c_str());
	if (!test.is_open())
		return false;
	test.close();
	return true;
}

static inline int suffixcmp(const char *string, int len, const char *suffix)
{
	int suffix_len = strlen(suffix);
	if (len < suffix_len)
		return -1;
	return strncmp(string + len - suffix_len, suffix, suffix_len);
}

static string find_in_path(const char *path)
{
	const char *p = getenv("PATH");

#ifdef WIN32
	int len = strlen(path);
	string path_with_suffix;
	if (suffixcmp(path, len, ".exe") && suffixcmp(path, len, ".EXE")) {
		path_with_suffix = string(path) + ".exe";
		path = path_with_suffix.c_str();
	}
	string in_cwd = string(make_absolute_path(path));
	if (file_exists(in_cwd))
		return in_cwd;
#endif

	if (!p) {
		cerr << "Could not get PATH" << endl;
		exit(1);
	}

	for (;;) {
		const char *colon = strchr(p, PATH_SEP[0]), *orig_p = p;
		int len = colon ? colon - p : strlen(p);
		struct stat st;
		char buffer[PATH_MAX];

		if (!len) {
			cerr << "Could not find " << path << " in PATH" << endl;
			exit(1);
		}

		p += len + !!colon;
		if (!is_absolute_path(orig_p))
			continue;
		snprintf(buffer, sizeof(buffer), "%.*s/%s", len, orig_p, path);
#ifdef WIN32
#define S_IX S_IXUSR
#else
#define S_IX (S_IXUSR | S_IXGRP | S_IXOTH)
#endif
		if (!stat(buffer, &st) && S_ISREG(st.st_mode) &&
				(st.st_mode & S_IX))
			return make_absolute_path(buffer);
	}
}

#ifdef WIN32
static char *dos_path(const char *path)
{
	int size = GetShortPathName(path, NULL, 0);
	char *buffer = (char *)malloc(size);
	GetShortPathName(path, buffer, size);
	return buffer;
}
#endif

static __attribute__((unused)) string get_parent_directory(string path)
{
	size_t slash = path.find_last_of("/\\");
	if (slash == 0 || slash == path.npos)
		return string("/");
	return path.substr(0, slash);
}

int path_list_contains(const char *list, const char *path)
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
	string path = get_jre_home() + "/" + library_path;
	string lib_path = get_parent_directory(get_parent_directory(path));
	// Is this JDK6?
	if (dir_exists(lib_path + "/jli"))
		return;

	const char *original = getenv("LD_LIBRARY_PATH");
	if (original && path_list_contains(original, lib_path.c_str()))
		return;

	if (original)
		lib_path = string(original) + PATH_SEP + lib_path;
	setenv_or_exit("LD_LIBRARY_PATH", lib_path.c_str(), 1);
	cerr << "Re-executing with correct library lookup path" << endl;
	execv(main_argv_backup[0], main_argv_backup);
#endif
}

static const char *get_fiji_dir(const char *argv0)
{
	static string buffer;

	if (buffer != "")
		return buffer.c_str();

	if (!last_slash(argv0))
		buffer = find_in_path(argv0);
	else
		buffer = make_absolute_path(argv0);
	argv0 = buffer.c_str();

	const char *slash = last_slash(argv0);
	if (!slash) {
		cerr << "Could not get absolute path for executable" << endl;
		exit(1);
	}

	int len = slash - argv0;
	if (!suffixcmp(argv0, len, "/precompiled") ||
			!suffixcmp(argv0, len, "\\precompiled")) {
		slash -= strlen("/precompiled");
		run_precompiled = true;
	}
#ifdef MACOSX
	else if (!suffixcmp(argv0, len, "/Fiji.app/Contents/MacOS"))
		slash -= strlen("/Contents/MacOS");
#endif
#ifdef WIN32
	else if (!suffixcmp(argv0, len, "/PRECOM~1") ||
			!suffixcmp(argv0, len, "\\PRECOM~1")) {
		slash -= strlen("/PRECOM~1");
		run_precompiled = true;
	}
#endif

	buffer = buffer.substr(0, slash - argv0);
#ifdef WIN32
	buffer = dos_path(buffer.c_str());
#endif
	return buffer.c_str();
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
	stringstream buffer;
	void *handle;
	char *err;
	static jint (*JNI_CreateJavaVM)(JavaVM **pvm, void **penv, void *args);

	string java_home = get_jre_home();
#ifdef WIN32
	/* Windows automatically adds the path of the executable to PATH */
	stringstream path;
	path << getenv("PATH") << ";" << java_home << "/bin";
	setenv_or_exit("PATH", path.str().c_str(), 1);
	// on Windows, a setenv() invalidates strings obtained by getenv()
	if (original_java_home_env)
		original_java_home_env = strdup(original_java_home_env);
#endif
	setenv_or_exit("JAVA_HOME", java_home.c_str(), 1);
	buffer << java_home << "/" << library_path;

	handle = dlopen(buffer.str().c_str(), RTLD_LAZY);
	if (!handle) {
		setenv_or_exit("JAVA_HOME", original_java_home_env, 1);
		if (!file_exists(java_home))
			return 2;

		const char *error = dlerror();
		if (!error)
			error = "(unknown error)";
		cerr << "Could not load Java library '" <<
			buffer.str() << "': " << error << endl;
		return 1;
	}
	dlerror(); /* Clear any existing error */

	JNI_CreateJavaVM = (typeof(JNI_CreateJavaVM))dlsym(handle,
			JNI_CREATEVM);
	err = dlerror();
	if (err) {
		cerr << "Error loading libjvm: " << err << endl;
		setenv_or_exit("JAVA_HOME", original_java_home_env, 1);
		return 1;
	}
#endif

	return JNI_CreateJavaVM(vm, env, args);
}

/* Windows specific stuff */

#ifdef WIN32
static bool console_opened = false;

static void sleep_a_while(void)
{
	sleep(60);
}

static void open_win_console(void)
{
	static int initialized = 0;

	if (initialized)
		return;
	initialized = 1;
	if (!isatty(1) && !isatty(2))
		return;

	string kernel32_dll_path = string(getenv("WINDIR"))
			+ "\\system32\\kernel32.dll";
	void *kernel32_dll = dlopen(kernel32_dll_path.c_str(), RTLD_LAZY);
	BOOL WINAPI (*attach_console)(DWORD process_id) = NULL;
	if (kernel32_dll)
		attach_console = (typeof(attach_console))
			dlsym(kernel32_dll, "AttachConsole");
	if (!attach_console || !attach_console((DWORD)-1)) {
		AllocConsole();
		console_opened = true;
		atexit(sleep_a_while);
	} else {
		char title[1024];
		if (GetConsoleTitle(title, sizeof(title)) &&
				!strncmp(title, "rxvt", 4))
			return; // console already opened
	}

	HANDLE handle = CreateFile("CONOUT$", GENERIC_WRITE, FILE_SHARE_WRITE,
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
	string pattern;
	HANDLE handle;
	WIN32_FIND_DATA find_data;
	int done;
	struct entry entry;
};

struct dir *open_dir(const char *path)
{
	struct dir *result = new dir();
	if (!result)
		return result;
	result->pattern = path;
	result->pattern += "/*";
	result->handle = FindFirstFile(result->pattern.c_str(),
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
	FindClose(dir->handle);
	delete dir;
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

static bool dir_exists(string path)
{
	DIR *dir = opendir(path.c_str());
	if (dir) {
		closedir(dir);
		return true;
	}
	return false;
}

static int mkdir_p(string path)
{
	if (dir_exists(path))
		return 0;

	size_t slash = path.find_last_of("/\\");
	if (slash != 0 && slash != path.npos && mkdir_p(path.substr(0, slash)))
		return -1;

	return mkdir(path.c_str(), 0777);
}

static void add_java_home_to_path(void)
{
	string java_home = absolute_java_home;

	if (java_home == "") {
		const char *env = getenv("JAVA_HOME");
		if (env)
			java_home = env;
		else {
			java_home = string(fiji_dir) + "/" + relative_java_home;
			int len = java_home.length();
			if (len > 4 && java_home.substr(len - 4) == "/jre")
				java_home = java_home.substr(0, len - 4);
		}
	}

	string new_path;
	if (dir_exists(java_home + "/bin"))
		new_path += java_home + "/bin:";
	if (dir_exists(java_home + "/jre/bin"))
		new_path += java_home + "/jre/bin:";

	const char *env = getenv("PATH");
	new_path += env ? env : fiji_dir;
	setenv_or_exit("PATH", new_path.c_str(), 1);
}

static int headless;

int build_classpath(string &result, string jar_directory, int no_error) {
	DIR *directory = opendir(jar_directory.c_str());
	if (!directory) {
		if (no_error)
			return 0;
		cerr << "Failed to open: " << jar_directory << endl;
		return 1;
	}
	string extension(".jar");
	unsigned int extension_length = extension.size();
	struct dirent *entry;
	while (NULL != (entry = readdir(directory))) {
		string filename(entry->d_name);
		unsigned int n = filename.size();
		if (n <= extension_length)
			continue;
		unsigned int extension_start = n - extension_length;
		if (!filename.compare(extension_start,
					extension_length,
					extension)) {
			if (result != "")
				result += PATH_SEP;
			result += jar_directory + "/" + filename;
		} else {
			if (filename != "." && filename != ".." &&
					build_classpath(result, jar_directory
						+ "/" + filename, 1))
				return 1;
			continue;
		}

	}
	return 0;
}

static string set_property(JNIEnv *env, const char *key, const char *value)
{
	static jclass system_class = NULL;
	static jmethodID set_property_method = NULL;

	if (!system_class) {
		system_class = env->FindClass("java/lang/System");
		if (!system_class)
			return "";
	}

	if (!set_property_method) {
		set_property_method = env->GetStaticMethodID(system_class,
				"setProperty",
				"(Ljava/lang/String;Ljava/lang/String;)"
				"Ljava/lang/String;");
		if (!set_property_method)
			return "";
	}

	jstring result =
		(jstring)env->CallStaticObjectMethod(system_class,
				set_property_method,
				env->NewStringUTF(key),
				env->NewStringUTF(value));
	string previous;
	if (result) {
		const char *chars = env->GetStringUTFChars(result, NULL);
		previous = string(chars);
		env->ReleaseStringUTFChars(result, chars);
	}

	return previous;
}

struct string_array {
	char **list;
	int nr, alloc;
};

static void append_string(struct string_array& array, char *str)
{
	if (array.nr >= array.alloc) {
		array.alloc = 2 * array.nr + 16;
		array.list = (char **)realloc(array.list,
				array.alloc * sizeof(str));
	}
	array.list[array.nr++] = str;
}

static void prepend_string(struct string_array& array, char *str)
{
	if (array.nr >= array.alloc) {
		array.alloc = 2 * array.nr + 16;
		array.list = (char **)realloc(array.list,
				array.alloc * sizeof(str));
	}
	memmove(array.list + 1, array.list, array.nr * sizeof(str));
	array.list[0] = str;
	array.nr++;
}

static void prepend_string(struct string_array& array, const char *str)
{
	prepend_string(array, strdup(str));
}

static void append_string_array(struct string_array& target,
		struct string_array &source)
{
	if (target.alloc - target.nr < source.nr) {
		target.alloc += source.nr;
		target.list = (char **)realloc(target.list,
				target.alloc * sizeof(target.list[0]));
	}
	memcpy(target.list + target.nr, source.list,
			source.nr * sizeof(target.list[0]));
	target.nr += source.nr;
}

static JavaVMOption *prepare_java_options(struct string_array& array)
{
	JavaVMOption *result = (JavaVMOption *)calloc(array.nr,
			sizeof(JavaVMOption));

	for (int i = 0; i < array.nr; i++)
		result[i].optionString = array.list[i];

	return result;
}

static jobjectArray prepare_ij_options(JNIEnv *env, struct string_array& array)
{
	jstring jstr;
	jobjectArray result;

	if (!(jstr = env->NewStringUTF(array.nr ? array.list[0] : ""))) {
fail:
		env->ExceptionDescribe();
		cerr << "Failed to create ImageJ option array" << endl;
		exit(1);
	}

	result = env->NewObjectArray(array.nr,
			env->FindClass("java/lang/String"), jstr);
	if (!result)
		goto fail;
	for (int i = 1; i < array.nr; i++) {
		if (!(jstr = env->NewStringUTF(array.list[i])))
			goto fail;
		env->SetObjectArrayElement(result, i, jstr);
	}
	return result;
}

struct options {
	struct string_array java_options, ij_options;
	int debug, use_system_jvm;
};

static void add_option(struct options& options, char *option, int for_ij)
{
	append_string(for_ij ?
			options.ij_options : options.java_options,
			option);
}

static void add_option(struct options& options, const char *option, int for_ij)
{
	add_option(options, strdup(option), for_ij);
}

static void add_option(struct options& options, string &option, int for_ij)
{
	add_option(options, option.c_str(), for_ij);
}

static void add_option(struct options& options, stringstream &option, int for_ij)
{
	add_option(options, option.str().c_str(), for_ij);
}

static bool is_quote(char c)
{
	return c == '\'' || c == '"';
}

static int find_closing_quote(string s, char quote, int index, int len)
{
	for (int i = index; i < len; i++) {
		char c = s[i];
		if (c == quote)
			return i;
		if (is_quote(c))
			i = find_closing_quote(s, c, i + 1, len);
	}
	cerr << "Unclosed quote: " << s << endl << "               ";
	for (int i = 0; i < index; i++)
		cerr << " ";
	cerr << "^" << endl;
	exit(1);
}

static void add_options(struct options &options, string &cmd_line, int for_ij)
{
	int len = cmd_line.length();
	string current = "";

	for (int i = 0; i < len; i++) {
		char c = cmd_line[i];
		if (is_quote(c)) {
			int i2 = find_closing_quote(cmd_line, c, i + 1, len);
			current += cmd_line.substr(i + 1, i2 - i - 1);
			i = i2;
			continue;
		}
		if (c == ' ' || c == '\t' || c == '\n') {
			if (current == "")
				continue;
			add_option(options, current, for_ij);
			current = "";
		} else
			current += c;
	}
	if (current != "")
		add_option(options, current, for_ij);
}

#ifndef MACOSX
static void read_file_as_string(string file_name, string &contents)
{
	char buffer[1024];
	ifstream in(file_name.c_str());
	while (in.good()) {
		in.get(buffer, sizeof(buffer));
		contents += buffer;
	}
	in.close();
}
#endif

static string quote_if_necessary(const char *option)
{
	string result = "";
	for (; *option; option++)
		switch (*option) {
		case '\n':
			result += "\\n";
			break;
		case '\t':
			result += "\\t";
			break;
		case ' ': case '"': case '\\':
			result += "\\";
			/* fallthru */
		default:
			result += *option;
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

	result = (char *)malloc(strlen(option) + backslashes + 2 + 1);
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

static void show_commandline(struct options& options)
{
	cout << "java";
	for (int j = 0; j < options.java_options.nr; j++)
		cout << " " << quote_if_necessary(options.java_options.list[j]);
	cout << " " << main_class;
	for (int j = 0; j < options.ij_options.nr; j++)
		cout << " " << quote_if_necessary(options.ij_options.list[j]);
	cout << endl;
}

bool file_is_newer(string path, string than)
{
	struct stat st1, st2;

	if (stat(path.c_str(), &st1))
		return false;
	return stat(than.c_str(), &st2) || st1.st_mtime > st2.st_mtime;
}

bool handle_one_option(int &i, const char *option, string &arg)
{
	if (!strcmp(main_argv[i], option)) {
		if (++i >= main_argc || !main_argv[i]) {
			cerr << "Option " << option << " needs an argument!"
				<< endl;
			exit(1);
		}
		arg = main_argv[i];
		return true;
	}
	int len = strlen(option);
	if (!strncmp(main_argv[i], option, len) && main_argv[i][len] == '=') {
		arg = main_argv[i] + len + 1;
		return true;
	}
	return false;
}

static bool is_file_empty(string path)
{
	struct stat st;

	return !stat(path.c_str(), &st) && !st.st_size;
}

static bool update_files(string relative_path)
{
	string absolute_path = string(fiji_dir) + "/update" + relative_path;
	DIR *directory = opendir(absolute_path.c_str());
	if (!directory)
		return false;
	if (mkdir_p(string(fiji_dir) + relative_path)) {
		cerr << "Could not create directory: " << relative_path << endl;
		exit(1);
	}
	struct dirent *entry;
	while (NULL != (entry = readdir(directory))) {
		string filename(entry->d_name);
		if (filename == "." || filename == ".." ||
				update_files(relative_path + "/" + filename))
			continue;

		string source = absolute_path + "/" + filename;
		string target = string(fiji_dir) + relative_path
			+ "/" + filename;

		if (is_file_empty(source)) {
			if (unlink(source.c_str()))
				cerr << "Could not remove " << source << endl;
			if (unlink(target.c_str()))
				cerr << "Could not remove " << target << endl;
			continue;
		}

#ifdef WIN32
		if (file_exists(target.c_str()) && unlink(target.c_str())) {
			cerr << "Could not remove old version of " << target
				<< ".  Please remove manually!" << endl;
			exit(1);
		}
#endif
		if (rename(source.c_str(), target.c_str())) {
			cerr << "Could not move " << source << " to "
				<< target << ": " << strerror(errno) << endl;
			exit(1);
		}
	}
	closedir(directory);
	rmdir(absolute_path.c_str());
	return true;
}

static void update_files(void)
{
	update_files(string(""));
}

static void /* no-return */ usage(void)
{
	cerr << "Usage: " << main_argv[0] << " [<Java options>.. --] "
		"[<ImageJ options>..] [<files>..]" << endl
		<< endl
		<< "Java options are passed to the Java Runtime, ImageJ" << endl
		<< "options to ImageJ (or Jython, JRuby, ...)." << endl
		<< endl
		<< "In addition, the following options are supported by Fiji:"
			<< endl
		<< "General options:" << endl
		<< "--help, -h" << endl
		<< "\tshow this help" << endl
		<< "--dry-run" << endl
		<< "\tshow the command line, but do not run anything" << endl
		<< "--system" << endl
		<< "\tdo not try to run bundled Java" << endl
		<< "--java-home <path>" << endl
		<< "\tspecify JAVA_HOME explicitly" << endl
		<< "--print-java-home" << endl
		<< "\tprint Fiji's idea of JAVA_HOME" << endl
		<< "--print-fiji-dir" << endl
		<< "\tprint where Fiji thinks it is located" << endl
#ifdef WIN32
		<< "--console" << endl
		<< "\talways open an error console" << endl
#endif
		<< "--headless" << endl
		<< "\trun in text mode" << endl
		<< "--fiji-dir <path>" << endl
		<< "\tset the fiji directory to <path> (used to find" << endl
		<< "\t jars/, plugins/ and macros/)" << endl
		<< "--heap, --mem, --memory <amount>" << endl
		<< "\tset Java's heap size to <amount> (e.g. 512M)" << endl
		<< "--class-path, --classpath, -classpath, --cp, -cp <path>"
			<< endl
		<< "\tappend <path> to the class path" << endl
		<< "--jar-path, --jarpath, -jarpath <path>"
			<< endl
		<< "\tappend .jar files in <path> to the class path" << endl
		<< "--ext <path>" << endl
		<< "\tset Java's extension directory to <path>" << endl
		<< "--default-gc" << endl
		<< "\tdo not use advanced garbage collector settings by default"
			<< endl << "\t(-Xincgc -XX:PermSize=128m)" << endl
		<< endl
		<< "Options for ImageJ:" << endl
		<< "--allow-multiple" << endl
		<< "\tdo not reuse existing ImageJ instance" << endl
		<< "--plugins <dir>" << endl
		<< "\tuse <dir> to discover plugins" << endl
		<< "--run <plugin> [<arg>]" << endl
		<< "\trun <plugin> in ImageJ, optionally with arguments" << endl
		<< "--edit <file>" << endl
		<< "\tedit the given file in the script editor" << endl
		<< endl
		<< "Options to run programs other than ImageJ:" << endl
		<< "--jdb" << endl
		<< "\tstart in JDB, the Java debugger" << endl
		<< "--jython" << endl
		<< "\tstart Jython instead of ImageJ (this is the" << endl
		<< "\tdefault when called with a file ending in .py)" << endl
		<< "--jruby" << endl
		<< "\tstart JRuby instead of ImageJ (this is the" << endl
		<< "\tdefault when called with a file ending in .rb)" << endl
		<< "--clojure" << endl
		<< "\tstart Clojure instead of ImageJ (this is the "<< endl
		<< "\tdefault when called with a file ending in .clj)" << endl
		<< "--main-class <class name> (this is the" << endl
		<< "\tdefault when called with a file ending in .class)" << endl
		<< "--beanshell, --bsh" << endl
		<< "\tstart BeanShell instead of ImageJ (this is the "<< endl
		<< "\tdefault when called with a file ending in .bs or .bsh)"
		<< endl
		<< "--main-class <class name> (this is the" << endl
		<< "\tdefault when called with a file ending in .class)" << endl
		<< "\tstart the given class instead of ImageJ" << endl
		<< "--build" << endl
		<< "\tstart Fiji's build instead of ImageJ" << endl
		<< "--javac" << endl
		<< "\tstart JavaC, the Java Compiler, instead of ImageJ" << endl
		<< "--ant" << endl
		<< "\trun Apache Ant" << endl
		<< "--retrotranslator" << endl
		<< "\tuse Retrotranslator to support Java < 1.6" << endl
		<< endl;
	exit(1);
}

/* the maximal size of the heap on 32-bit systems, in megabyte */
#ifdef WIN32
#define MAX_32BIT_HEAP 1638
#else
#define MAX_32BIT_HEAP 1920
#endif

string make_memory_option(size_t memory_size)
{
	memory_size >>= 20;
	stringstream heap_size;
	heap_size << "-Xmx"<< memory_size << "m";
	return heap_size.str();
}

static void try_with_less_memory(size_t memory_size)
{
	/* Try again, with 25% less memory */
	if (memory_size < 0)
		return;
	memory_size >>= 20; // turn into megabytes
	size_t subtract = memory_size >> 2;
	if (!subtract)
		return;
	memory_size -= subtract;
	stringstream option;
	option << "--mem=" << memory_size << "m";
	char *memory_option = strdup(option.str().c_str());

	main_argc = main_argc_backup;
	main_argv = main_argv_backup;
	char **new_argv = (char **)malloc((3 + main_argc) * sizeof(char *));
	new_argv[0] = main_argv[0];

	int j = 1;
	new_argv[j++] = memory_option;

	// strip out --mem options
	bool found_dashdash = false;
	for (int i = 1; i < main_argc; i++) {
		if (!found_dashdash && !strcmp(main_argv_backup[i], "--"))
			found_dashdash = true;
		string dummy;
		if ((!found_dashdash || is_default_main_class(main_class)) &&
				(handle_one_option(i, "--mem", dummy) ||
				 handle_one_option(i, "--memory", dummy)))
			continue;
		new_argv[j++] = main_argv[i];
	}
	new_argv[j] = NULL;

	cerr << "Trying with a smaller heap: " << memory_option << endl;

#ifdef WIN32
	new_argv[0] = dos_path(new_argv[0]);
	for (int k = 0; k < j; k++)
		new_argv[k] = quote_win32(new_argv[k]);
#endif
	execve(new_argv[0], new_argv, NULL);

	stringstream error;

	error << "ERROR: failed to launch (errno=" << errno << ";"
		<< strerror(errno) << "):" << endl;
	for (int i = 0; i < j; i++)
		error << new_argv[i] << " ";
	error << endl;
#ifdef WIN32
	MessageBox(NULL, error.str().c_str(), "Error executing Fiji", MB_OK);
#else
	cerr << error.str();
#endif
	exit(1);
}

bool is_building(const char *target)
{
	if (main_argc < 3 ||
			(strcmp(main_argv[1], "--build") &&
			 strcmp(main_argv[1], "--fake")))
		return false;
	for (int i = 2; i < main_argc; i++)
		if (!strcmp(main_argv[i], target))
			return true;
	return false;
}

bool retrotranslator = false;

static int start_ij(void)
{
	JavaVM *vm;
	struct options options;
	JavaVMInitArgs args;
	JNIEnv *env;
	string class_path, ext_option, jvm_options, default_arguments, arg;
	stringstream plugin_path;
	int dashdash = 0;
	bool allow_multiple = false, skip_build_classpath = false;
	bool jdb = false, add_class_path_option = false, advanced_gc = true;

#ifdef WIN32
#define EXE_EXTENSION ".exe"
#else
#define EXE_EXTENSION
#endif
	if (file_exists(string(fiji_dir) + "/fiji" EXE_EXTENSION) &&
			file_is_newer(string(fiji_dir) + "/fiji.cxx",
				string(fiji_dir) + "/fiji" EXE_EXTENSION) &&
			!is_building("fiji"))
		cerr << "Warning: your Fiji executable is not up-to-date"
			<< endl;

	size_t memory_size = 0;

	memset(&options, 0, sizeof(options));

#ifdef MACOSX
	// When double-clicked => exactly 1 empty string argument
	if (main_argc == 2 && !*main_argv[1])
	{
		/*
		 * Reset main_argc so that ImageJ won't try to open
		 * that empty argument as a file (the root directory).
		 */
		main_argc=1;
		/*
		 * Additionally, change directory to the fiji dir to emulate
		 * the behavior of the regular ImageJ application which does
		 * not start up in the filesystem root.
		 */
		chdir(fiji_dir);
	}

	string value;
	if (!get_fiji_bundle_variable("heap", value) ||
			!get_fiji_bundle_variable("mem", value) ||
			!get_fiji_bundle_variable("memory", value))
		memory_size = parse_memory(value.c_str());
	if (!get_fiji_bundle_variable("system", value) &&
			atol(value.c_str()) > 0)
		options.use_system_jvm++;
	if (get_fiji_bundle_variable("ext", ext_option))
		ext_option = get_java_home() + "/Home/lib/ext:"
			"/Library/Java/Extensions:"
			"/System/Library/Java/Extensions:"
			"/System/Library/Frameworks/JavaVM.framework/"
				"Home/lib/ext";
	if (!get_fiji_bundle_variable("allowMultiple", value))
		allow_multiple = parse_bool(value);
	get_fiji_bundle_variable("JVMOptions", jvm_options);
	get_fiji_bundle_variable("DefaultArguments", default_arguments);
#else
	read_file_as_string(string(fiji_dir) + "/jvm.cfg", jvm_options);
#endif

	int count = 1;
	for (int i = 1; i < main_argc; i++)
		if (!strcmp(main_argv[i], "--") && !dashdash)
			dashdash = count;
		else if (dashdash && main_class &&
				!is_default_main_class(main_class))
			main_argv[count++] = main_argv[i];
		else if (!strcmp(main_argv[i], "--dry-run"))
			options.debug++;
		else if (handle_one_option(i, "--java-home", arg)) {
			absolute_java_home = arg;
			setenv_or_exit("JAVA_HOME", strdup(arg.c_str()), 1);
		}
		else if (!strcmp(main_argv[i], "--system"))
			options.use_system_jvm++;
#ifdef WIN32
		else if (!strcmp(main_argv[i], "--console"))
			open_win_console();
#endif
		else if (!strcmp(main_argv[i], "--jdb")) {
			add_class_path_option = true;
			jdb = true;
		}
		else if (!strcmp(main_argv[i], "--allow-multiple"))
			allow_multiple = true;
		else if (handle_one_option(i, "--plugins", arg))
			plugin_path << "-Dplugins.dir=" << arg;
		else if (handle_one_option(i, "--run", arg)) {
			replace(arg.begin(), arg.end(), '_', ' ');
			if (i + 1 < main_argc && main_argv[i + 1][0] != '-')
				arg += string("\", \"") + main_argv[++i];
			add_option(options, "-eval", 1);
			arg = string("run(\"") + arg + "\");";
			add_option(options, arg, 1);
		}
		else if (handle_one_option(i, "--edit", arg))
			for (;;) {
				add_option(options, "-eval", 1);
				arg = string("run(\"Script Editor\", \"")
					+ arg + "\");";
				add_option(options, arg, 1);
				if (i + 1 >= main_argc)
					break;
				arg = main_argv[++i];
			}
		else if (handle_one_option(i, "--heap", arg) ||
				handle_one_option(i, "--mem", arg) ||
				handle_one_option(i, "--memory", arg))
			memory_size = parse_memory(arg.c_str());
		else if (!strcmp(main_argv[i], "--headless")) {
			headless = 1;
			/* handle "--headless script.ijm" gracefully */
			if (i + 2 == main_argc && main_argv[i + 1][0] != '-')
				dashdash = count;
		}
		else if (!strcmp(main_argv[i], "--jython"))
			main_class = "org.python.util.jython";
		else if (!strcmp(main_argv[i], "--jruby"))
			main_class = "org.jruby.Main";
		else if (!strcmp(main_argv[i], "--clojure"))
			main_class = "clojure.lang.Repl";
		else if (!strcmp(main_argv[i], "--beanshell") ||
				!strcmp(main_argv[i], "--bsh"))
			main_class = "bsh.Interpreter";
		else if (handle_one_option(i, "--main-class", arg)) {
			class_path += "." PATH_SEP;
			main_class = strdup(arg.c_str());
		}
		else if (handle_one_option(i, "--jar", arg)) {
			class_path += arg + PATH_SEP;
			main_class = "fiji.JarLauncher";
			main_argv[count++] = strdup(arg.c_str());
		}
		else if (handle_one_option(i, "--class-path", arg) ||
				handle_one_option(i, "--classpath", arg) ||
				handle_one_option(i, "-classpath", arg) ||
				handle_one_option(i, "--cp", arg) ||
				handle_one_option(i, "-cp", arg))
			class_path += arg + PATH_SEP;
		else if (handle_one_option(i, "--jar-path", arg) ||
				handle_one_option(i, "--jarpath", arg) ||
				handle_one_option(i, "-jarpath", arg)) {
			string jars;
			build_classpath(jars, arg, 0);
			if (jars != "")
				class_path += jars + PATH_SEP;
		}
		else if (handle_one_option(i, "--ext", arg)) {
			if (ext_option != "")
				ext_option += PATH_SEP;
			ext_option += arg;
		}
		else if (!strcmp(main_argv[i], "--build") ||
				!strcmp(main_argv[i], "--fake")) {
#ifdef WIN32
			open_win_console();
#endif
			skip_build_classpath = true;
			headless = 1;
			string fake_jar = string(fiji_dir) + "/jars/fake.jar";
			string precompiled_fake_jar = string(fiji_dir)
				+ "/precompiled/fake.jar";
			if (run_precompiled || !file_exists(fake_jar) ||
					file_is_newer(precompiled_fake_jar,
						fake_jar))
				fake_jar = precompiled_fake_jar;
			if (file_is_newer(string(fiji_dir) + "/src-plugins/"
					"fake/fiji/build/Fake.java", fake_jar)
					&& !is_building("jars/fake.jar"))
				cerr << "Warning: jars/fake.jar is not up-to-date"
					<< endl;
			class_path += fake_jar + PATH_SEP;
			main_class = "fiji.build.Fake";
		}
		else if (!strcmp(main_argv[i], "--javac") ||
				!strcmp(main_argv[i], "--javap")) {
			add_class_path_option = true;
			headless = 1;
			class_path += fiji_dir;
			if (run_precompiled || !file_exists(string(fiji_dir)
						+ "/jars/javac.jar"))
				class_path += "/precompiled";
			else
				class_path += "/jars";
			class_path += string("/javac.jar" PATH_SEP)
				+ get_jre_home()
				+ "/../lib/tools.jar" PATH_SEP;
			if (!strcmp(main_argv[i], "--javac"))
				main_class = "com.sun.tools.javac.Main";
			else if (!strcmp(main_argv[i], "--javap"))
				main_class = "sun.tools.javap.Main";
			else
				cerr << main_argv[i] << "!\n";
		}
		else if (!strcmp(main_argv[i], "--ant")) {
			main_class = "org.apache.tools.ant.Main";
			class_path += get_jre_home()
				+ "/../lib/tools.jar" PATH_SEP;
		}
		else if (!strcmp(main_argv[i], "--retrotranslator") ||
				!strcmp(main_argv[i], "--retro"))
			retrotranslator = true;
		else if (handle_one_option(i, "--fiji-dir", arg))
			fiji_dir = strdup(arg.c_str());
		else if (!strcmp("--print-fiji-dir", main_argv[i])) {
			cout << fiji_dir << endl;
			exit(0);
		}
		else if (!strcmp("--print-java-home", main_argv[i])) {
			cout << get_java_home() << endl;
			exit(0);
		}
		else if (!strcmp("--default-gc", main_argv[i]))
			advanced_gc = false;
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
			false
#endif
			) {
		cerr << "No GUI detected.  Falling back to headless mode."
			<< endl;
		headless = 1;
	}

	if (ext_option != "") {
		ext_option = string("-Djava.ext.dirs=") + ext_option;
		add_option(options, ext_option, 0);
	}

	/* For Jython to work properly with .jar packages: */
	add_option(options, "-Dpython.cachedir.skip=false", 0);
	if (plugin_path.str() == "")
		plugin_path << "-Dplugins.dir=" << fiji_dir;
	add_option(options, plugin_path, 0);

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
		add_option(options, make_memory_option(memory_size).c_str(), 0);

	if (headless)
		add_option(options, "-Djava.awt.headless=true", 0);

	if (is_ipv6_broken())
		add_option(options, "-Djava.net.preferIPv4Stack=true", 0);

	if (advanced_gc) {
		add_option(options, "-Xincgc", 0);
		add_option(options, "-XX:PermSize=128m", 0);
	}

	if (!main_class) {
		const char *first = main_argv[1];
		int len = main_argc > 1 ? strlen(first) : 0;

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
			class_path += "." PATH_SEP;
			string dotted = first;
			replace(dotted.begin(), dotted.end(), '/', '.');
			dotted = dotted.substr(0, len - 6);
			main_class = strdup(dotted.c_str());
			main_argv++;
			main_argc--;
		}
		else
			main_class = default_main_class;
	}

	maybe_reexec_with_correct_lib_path();

	if (retrotranslator && build_classpath(class_path,
				string(fiji_dir) + "/retro", 0))
		return 1;

	/* set up class path */
	class_path = "-Djava.class.path=" + class_path;
	if (skip_build_classpath) {
		/* strip trailing ":" */
		int len = class_path.length();
		if (class_path[len - 1] == PATH_SEP[0])
			class_path = class_path.substr(0, len - 1);
	}
	else {
		if (headless)
			class_path += string(fiji_dir) + "/misc/headless.jar";

		if (is_default_main_class(main_class))
			update_files();
		else
			if (build_classpath(class_path, string(fiji_dir)
						+ "/plugins", 0))
				return 1;
		build_classpath(class_path, string(fiji_dir) + "/jars", 0);
	}
	add_option(options, class_path, 0);

	if (jvm_options != "")
		add_options(options, jvm_options, 0);
	if (default_arguments != "")
		add_options(options, default_arguments, 1);

	if (dashdash) {
		for (int i = 1; i < dashdash; i++)
			add_option(options, main_argv[i], 0);
		main_argv += dashdash - 1;
		main_argc -= dashdash - 1;
	}

	if (add_class_path_option) {
		add_option(options, "-classpath", 1);
		add_option(options, class_path.substr(18).c_str(), 1);
	}

	if (!strcmp(main_class, "org.apache.tools.ant.Main"))
		add_java_home_to_path();

	if (is_default_main_class(main_class)) {
		if (allow_multiple)
			add_option(options, "-port0", 1);
		else
			add_option(options, "-port7", 1);
		add_option(options, "-Dsun.java.command=Fiji", 0);

		update_files();
	}

	/* handle "--headless script.ijm" gracefully */
	if (headless && is_default_main_class(main_class)) {
		if (main_argc < 2) {
			cerr << "--headless without a parameter?" << endl;
			if (!options.debug)
				exit(1);
		}
		if (main_argc > 1 && *main_argv[1] != '-')
			add_option(options, "-batch", 1);
	}

	if (jdb) {
		add_option(options, main_class, 1);
		main_class = "com.sun.tools.example.debug.tty.TTY";
	}

	if (retrotranslator) {
		add_option(options, "-advanced", 1);
		add_option(options, main_class, 1);
		main_class =
			"net.sf.retrotranslator.transformer.JITRetrotranslator";
	}

	for (int i = 1; i < main_argc; i++)
		add_option(options, main_argv[i], 1);

	const char *properties[] = {
		"fiji.dir", fiji_dir,
		"fiji.defaultLibPath", JAVA_LIB_PATH,
		"fiji.executable", main_argv0,
		NULL
	};

	if (options.debug) {
		for (int i = 0; properties[i]; i += 2) {
			stringstream property;
			property << "-D" << properties[i]
				<< "=" << properties[i + 1];
			add_option(options, property, 0);
		}

		show_commandline(options);
		exit(0);
	}

	memset(&args, 0, sizeof(args));
	/* JNI_VERSION_1_4 is used on Mac OS X to indicate 1.4.x and later */
	args.version = JNI_VERSION_1_4;
	args.options = prepare_java_options(options.java_options);
	args.nOptions = options.java_options.nr;
	args.ignoreUnrecognized = JNI_FALSE;

	if (options.use_system_jvm)
		env = NULL;
	else {
		int result = create_java_vm(&vm, (void **)&env, &args);
		if (result == JNI_ENOMEM) {
			try_with_less_memory(memory_size);
			cerr << "Out of memory!" << endl;
		}
		if (result) {
			if (result != 2)
				cerr << "Warning: falling back to System JVM"
					<< endl;
			env = NULL;
		} else
			prepend_string(options.java_options,
				("-Djava.home=" + get_java_home()).c_str());
	}

	if (env) {
		jclass instance;
		jmethodID method;
		jobjectArray args;

		for (int i = 0; properties[i]; i += 2)
			set_property(env, properties[i], properties[i + 1]);

		string slashed(main_class);
		replace(slashed.begin(), slashed.end(), '.', '/');
		if (!(instance = env->FindClass(slashed.c_str()))) {
			env->ExceptionDescribe();
			cerr << "Could not find " << main_class << endl;
			exit(1);
		} else if (!(method = env->GetStaticMethodID(instance,
				"main", "([Ljava/lang/String;)V"))) {
			env->ExceptionDescribe();
			cerr << "Could not find main method" << endl;
			exit(1);
		}

		args = prepare_ij_options(env, options.ij_options);
		env->CallStaticVoidMethodA(instance,
				method, (jvalue *)&args);
		if (vm->DetachCurrentThread())
			cerr << "Could not detach current thread"
				<< endl;
		/* This does not return until ImageJ exits */
		vm->DestroyJavaVM();
	} else {
		/* fall back to system-wide Java */
#ifdef MACOSX
		/*
		 * On MacOSX, one must (stupidly) fork() before exec() to
		 * clean up some pthread state somehow, otherwise the exec()
		 * will fail with "Operation not supported".
		 */
		if (fork())
			exit(0);

		add_option(options, "-Xdock:name=Fiji", 0);
		string icon_option = "-Xdock:icon=";
		append_icon_path(icon_option);
		add_option(options, icon_option, 0);
#endif

		for (int i = 0; properties[i]; i += 2) {
			stringstream property;
			property << "-D" << properties[i]
				<< "=" << properties[i + 1];
			add_option(options, property, 0);
		}

		/* fall back to system-wide Java */
		add_option(options, main_class, 0);
		append_string_array(options.java_options, options.ij_options);
		append_string(options.java_options, NULL);
		prepend_string(options.java_options, "java");

		string java_binary("java");
		char * java_home_env = getenv("JAVA_HOME");
		if( java_home_env && strlen(java_home_env) > 0 ) {
			int n = strlen(java_home_env);
			cerr << "Found that JAVA_HOME was: '" <<
				java_home_env << "'" << endl;
			java_binary = java_home_env;
			if( java_home_env[n-1] != '/' ) {
				java_binary += "/";
			}
			java_binary += "bin/java";
		}
		options.java_options.list[0] = (char *)java_binary.c_str();
#ifdef WIN32
		if (console_opened)
			sleep(5); // sleep 5 seconds

		FreeConsole(); // java.exe cannot reuse the console anyway
		for (int i = 0; i < options.java_options.nr - 1; i++)
			options.java_options.list[i] =
				quote_win32(options.java_options.list[i]);
#endif
		if (execvp(java_binary.c_str(), options.java_options.list))
			cerr << "Could not launch system-wide Java ("
				<< strerror(errno) << ")" << endl;
#ifdef WIN32
		char message[16384];
		int off = sprintf(message, "Error: '%s' while executing\n\n",
				strerror(errno));
		for (int i = 0; options.java_options.list[i]; i++)
			off += sprintf(message + off, "'%s'\n",
					options.java_options.list[i]);
		MessageBox(NULL, message, "Error", MB_OK);
#endif
		exit(1);
	}
	return 0;
}

#ifdef MACOSX
static void append_icon_path(string &str)
{
	str += fiji_dir;
	/*
	 * Check if we're launched from within an Application bundle or
	 * command line.  If from a bundle, Fiji.app should be in the path.
	 */
	if (!suffixcmp(fiji_dir, strlen(fiji_dir), "Fiji.app"))
		str += "/Contents/Resources/Fiji.icns";
	else
		str += "/images/Fiji.icns";
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
				JavaVMBundleURL, CFSTR("Versions"), true);
	CFRelease(JavaVMBundleURL);
	if (!JavaVMBundlerVersionsDirURL)
		return;

	/* Append to the path the target JVM's Version. */
	CFURLRef TargetJavaVM = NULL;
	CFStringRef targetJVM; // Minimum Java5

	// try 1.6 only with 64-bit
	if (is_intel() && sizeof(void *) > 4) {
		targetJVM = CFSTR("1.6");
		TargetJavaVM =
		CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,
				JavaVMBundlerVersionsDirURL, targetJVM, true);
	}

	if (!TargetJavaVM) {
		targetJVM = CFSTR("1.5");
		TargetJavaVM =
		CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,
				JavaVMBundlerVersionsDirURL, targetJVM, true);
	}

	CFRelease(JavaVMBundlerVersionsDirURL);
	if (!TargetJavaVM)
		return;

	UInt8 pathToTargetJVM[PATH_MAX] = "";
	Boolean result = CFURLGetFileSystemRepresentation(TargetJavaVM, true,
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

static int get_fiji_bundle_variable(const char *key, string &value)
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

	value = CFStringGetCStringPtr(propertyString,
			kCFStringEncodingMacRoman);

	return 0;
}

/* MacOSX needs to run Java in a new thread, AppKit in the main thread. */

static void dummy_call_back(void *info) {}

static void *start_ij_aux(void *dummy)
{
	exit(start_ij());
}

static int start_ij_macosx(void)
{
	/* set the Application's name */
	stringstream name;
	name << "APP_NAME_" << (long)getpid();
	setenv(name.str().c_str(), "Fiji", 1);

	/* set the Dock icon */
	stringstream icon;
	icon << "APP_ICON_" << (long)getpid();;
	string icon_path;
	append_icon_path(icon_path);
	setenv(icon.str().c_str(), icon_path.c_str(), 1);

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
			retrotranslator = true;
		match = "-macosx";
		replace = "-tiger";
		if (sizeof(void *) < 8)
			return 0; /* already 32-bit, everything's fine */
	}

	int offset = strlen(argv[0]) - strlen(match);
	if (offset < 0 || strcmp(argv[0] + offset, match))
		return 0; /* suffix not found, no replacement */

	if (strlen(replace) > strlen(match)) {
		char *buffer = (char *)malloc(offset + strlen(replace) + 1);
		if (!buffer) {
			cerr << "Could not allocate new argv[0]" << endl;
			exit(1);
		}
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

static bool is_dir_empty(string path)
{
	DIR *dir = opendir(path.c_str());
	if (!dir)
		return false;

	struct dirent *entry;
	while (NULL != (entry = readdir(dir)))
		if (entry->d_name[0] != '.') {
			closedir(dir);
			return false;
		}

	closedir(dir);
	return true;
}

static string get_newest_subdir(string relative_path)
{
	string path = string(fiji_dir) + "/" + relative_path;
	string result = "";
	DIR *dir = opendir(path.c_str());
	if (!dir)
		return result;
	long mtime = 0;
	struct dirent *entry;
	while (NULL != (entry = readdir(dir))) {
		string filename(entry->d_name);
		if (filename == "." || filename == ".." || filename == ".git")
			continue;
		struct stat st;
		if (stat((path + "/" + filename).c_str(), &st))
			continue;
		if (!S_ISDIR(st.st_mode))
			continue;
		if (is_dir_empty(path + "/" + filename))
			continue;
		if (mtime < st.st_mtime) {
			mtime = st.st_mtime;
			result = relative_path + "/" + filename;
		}
	}
	closedir(dir);
	return result;
}

static void adjust_java_home_if_necessary(void)
{
#ifdef MACOSX
	/* On MacOSX, we use the system Java anyway. */
	return;
#endif
	if (file_exists(string(fiji_dir) + "/" + relative_java_home
			+ "/" + library_path))
		return;
	string platform_subdir = get_newest_subdir(string("java"));
	if (platform_subdir == "")
		return;
	string jdk_subdir = get_newest_subdir(platform_subdir);
	if (jdk_subdir == "")
		return;
	jdk_subdir += "/jre";
	if (dir_exists(string(fiji_dir) + "/" + jdk_subdir))
		relative_java_home = strdup(jdk_subdir.c_str());
}

int main(int argc, char **argv, char **e)
{
#if defined(MACOSX)
	launch_32bit_on_tiger(argc, argv);
#elif defined(WIN32)
#ifdef WIN64
	/* work around MinGW64 breakage */
	argc = __argc;
	argv = __argv;
	argv[0] = _pgmptr;
#endif
	int len = strlen(argv[0]);
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
	int size = (argc + 1) * sizeof(char *);
	main_argv_backup = (char **)malloc(size);
	memcpy(main_argv_backup, main_argv, size);
	main_argc_backup = argc;

	return start_ij();
}
