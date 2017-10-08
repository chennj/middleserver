#ifndef BASE_TASK_H
#define BASE_TASK_H

#include <sys/socket.h>
#include <sys/types.h>
#include <stdio.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <sys/epoll.h>
#include <assert.h>
#include <netinet/tcp.h>

#include "cliusr.h"

#define INVALIDFD   ((int)-1)

using namespace std;

class base_task{

public:
    virtual void doit()=0;
};

class tcp_task : public base_task{

private:
    int     sockfd;
    string  order;
    ulong   size;

public:
    tcp_task(string & str, int fd, ulong len):
        sockfd(fd),
        size(len)
    {
        order.assign(str, 0, size);
    }

    bool alive(){

        return INVALIDFD==sockfd;
    }

    int parse_body(string & xml, cliusr & cu);

    void doit();

    bool send_data(const string &buffer, int fd);

    bool send_data(const char * buffer, uint size, int fd);

    string get_xml(const cliusr & cu);

    string assemble_send_msg(const string & str);
};

#endif // BASE_TASK_H

