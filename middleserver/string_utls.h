#ifndef STRING_UTLS_H
#define STRING_UTLS_H

#include <string>

class string_utls{

public:
    string_utls(){}
    ~string_utls(){}

public:
    static void int_to_bytes_big(int i, char * bytes);
    static int  bytes_to_int_big(const char * bytes);
    static void int_to_bytes_little(int i, char * bytes);
    static int  bytes_to_int_little(const char * bytes);

    static void simple_regex_match(const char* str, const char * pattern, std::string & result);
};

#endif // STRING_UTLS_H

