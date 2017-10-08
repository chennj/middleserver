#include "string_utls.h"
#include <iostream>
#include <regex.h>
#include <memory.h>

int string_utls::bytes_to_int_big(const char *bytes){

    int addr =0;
    addr |= (bytes[3] & 0xff);
    addr |= ((bytes[2] << 8)& 0xff00);
    addr |= ((bytes[1] << 16)&0xff0000);
    addr |= ((bytes[0] << 24)&0xff000000);
    return addr;
}

void string_utls::int_to_bytes_big(int i, char *bytes){

    bytes[3] = i & 0xff;
    bytes[2] = (i >> 8) & 0xff;
    bytes[1] = (i >> 16) & 0xff;
    bytes[0] = (i >> 24) & 0xff;
}

int string_utls::bytes_to_int_little(const char *bytes){

    int addr = 0;
    addr |= (bytes[0] & 0xff);
    addr |= ((bytes[1] << 8) & 0xff00);
    addr |= ((bytes[2] << 16)& 0xff0000);
    addr |= ((bytes[3] << 24)& 0xff000000);
    return addr;
}

void string_utls::int_to_bytes_little(int i, char *bytes){

    bytes[0] = i & 0xff;
    bytes[1] = (i >> 8) & 0xff;
    bytes[2] = (i >> 16) & 0xff;
    bytes[3] = (i >> 24) & 0xff;
}

void string_utls::simple_regex_match(const char *str, const char* pattern, std::string & result){

    regex_t reg;
    regmatch_t match[10];

    result.clear();

    int ret = 0;
    ret = regcomp(&reg, pattern, REG_EXTENDED | REG_NEWLINE);

    if(ret != 0){
        std::cout << "regex error" << std::endl;
    }else{
        ret = regexec(&reg, str, 10, match, 0);
        if(ret != REG_NOMATCH){
            int len = match[1].rm_eo - match[1].rm_so;
            char buf[1024] = {0};
            //memcpy(buf, str + match[1].rm_so, len);
            //printf("final buf %s\n", buf);
            result.append(str + match[1].rm_so, len);
        }
    }
    regfree(&reg);
}
