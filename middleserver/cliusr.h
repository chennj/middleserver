#ifndef CLIUSR_H
#define CLIUSR_H

#include <iostream>
#include <sstream>
#include <string>

class cliusr{

public:
    cliusr(){}
    ~cliusr(){}

public:

    std::string type;
    std::string parentid;
    std::string selfgroupid;
    std::string selfuserno;
    std::string peergroupid;
    std::string peeruserno;
    std::string content;

    int self_sockfd;
    int peer_sockfd;
    int svr_sockfd;
};

#endif // CLIUSR_H

