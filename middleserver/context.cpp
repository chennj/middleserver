#include "context.h"

hash_map<int, string> context::g_relation = hash_map<int, string>();
hash_map<string, cliusr> context::g_cliusrs = hash_map<string, cliusr>();
mutex_locker context::map_mutex_locker = mutex_locker();

