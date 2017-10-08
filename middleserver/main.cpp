#include "epoll_server.h"

using namespace std;

int main(int argc, char const *argv[])
{
    int port;
    int threadnum;
    
    if(argc == 3)
    {
        port = atoi(argv[1]);
        if(port == 0)
        {
            port = 9999;
        }
        threadnum = atoi(argv[2]);
        if(threadnum == 0)
        {
            threadnum=10;
        }
    }else if (argc == 2){
        port = atoi(argv[1]);
        if(port == 0)
        {
            port = 9999;
        }
        threadnum=10;
    }else {
        port = 9999;
        threadnum=10;
    }

    epoll_server *epoll = new epoll_server(port, threadnum);

    if (!epoll->init()){

        cout << "server start up failed" << endl;
        return 0;
    }

    epoll->epoll();

    return 0;
}

