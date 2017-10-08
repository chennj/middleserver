#ifndef CONTEXT_H
#define CONTEXT_H

#include "cliusr.h"
#include "locker.h"
#include <hash_map>

namespace __gnu_cxx {
    template<>
    struct hash<std::string>
    {
        hash<char*> h;
        size_t operator()(const std::string &s) const
        {
            return h(s.c_str());
        }
    };
}
using namespace std;
using namespace __gnu_cxx;

class context{

public:

    /**
     * @brief g_relation
     * 关联g_cliusrs
     * int key = socket fd
     * string = g_cliusrs.begin()->first
     */
    static hash_map<int, string> g_relation;

    /**
     * @brief g_cliusrs
     * string key = cliusr->userno
     */
    static hash_map<string, cliusr> g_cliusrs;

    static mutex_locker map_mutex_locker;
};


#endif // CONTEXT_H

