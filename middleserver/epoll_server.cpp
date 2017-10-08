#include "epoll_server.h"
#include "context.h"
#include "string_utls.h"

bool epoll_server::is_stop = false;

void epoll_server::processs_map(int sockfd){

    //处理缓存
    context::map_mutex_locker.lock();
    hash_map<int,string>::iterator iter_r = context::g_relation.find(sockfd);
    if (context::g_relation.end() != iter_r){

        string key = iter_r->second;
        hash_map<string,cliusr>::iterator iter_c = context::g_cliusrs.find(key);
        if (context::g_cliusrs.end() != iter_c){

            context::g_cliusrs.erase(iter_c);
        }

        context::g_relation.erase(iter_r);
    }
    context::map_mutex_locker.unlock();
    //
    cout << "缓存map size: " << context::g_cliusrs.size() << endl;
    cout << "客户端关闭：socket fd：" << sockfd << endl;
}

bool epoll_server::init()   //EpollServer的初始化
{
    cout << "sever being start up, waitting..." << endl;

    bzero(&bind_addr, sizeof(bind_addr));
    bind_addr.sin_family = AF_INET;
    bind_addr.sin_port = htons(listen_port);
    bind_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    //创建Socket
    listen_sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if(listen_sockfd < 0)
    {
        cout << "epoll server socket init error" << endl;
        return false;
    }
    int ret = bind(listen_sockfd, (struct sockaddr *)&bind_addr, sizeof(bind_addr));
    if(ret < 0)
    {
        cout << "epoll server bind init error" << endl;
        return false;
    }
    ret = listen(listen_sockfd, 10);
    if(ret < 0)
    {
        cout << "epoll server listen init error" << endl;
        return false;
    }
    //create Epoll
    epollfd = epoll_create1(0);
    if(epollfd < 0)
    {
        cout << "epoll server epoll_create init error" << endl;
        return false;
    }
    pool = new thread_pool<base_task>(thread_num);  //创建线程池
    return true;
}

void epoll_server::epoll()
{
    pool->start();   //线程池启动

    addfd(epollfd, listen_sockfd, false);

    while(!is_stop)
    {

        int ret = epoll_wait(epollfd, events, MAX_EVENT, -1);   //调用epoll_wait
        if (ret < 0)                                             //出错处理
        {
            cout << "epoll_wait error:" << errno << endl;
            continue;
        }
        for(int i = 0; i < ret; ++i)
        {
            int fd = events[i].data.fd;
            if (fd == listen_sockfd)                                    //新的连接到来
            {
                struct sockaddr_in clientAddr;
                socklen_t len = sizeof(clientAddr);
                int confd = accept(listen_sockfd, (struct sockaddr *)&clientAddr, &len);
                epoll_server::addfd(epollfd, confd, false);
                cout << "have client connect come in" << endl;
                /*
                int confd;
                while((confd = accept(listen_sockfd, (struct sockaddr *)&clientAddr, &len))>0){
                    epoll_server::addfd(epollfd, confd, false);
                }
                if (confd == -1) {

                    if (
                            errno != EAGAIN &&
                            errno != ECONNABORTED &&
                            errno != EPROTO &&
                            errno != EINTR)
                        perror("accept");
                }
                continue;
                */
            }
            else if(events[i].events & EPOLLIN)                 //某个fd上有数据可读
            {

                string  buffer;
                uint    len;
                uint    isCommand = 0;

                char head[4];
                if (!readData(head,4,fd)){
                    read_status = -1;
                    cout << "读包头失败:" << head << endl;
                    goto READERROR;
                }
                len = string_utls::bytes_to_int_big(head);

                cout << len << endl;
                if (len > 0 && len < 80960){
                    isCommand  = 1;
                }else{
                    cout << "长度超出范围0~80k" << endl;
                }

                if (isCommand){
                    if (!readData(buffer, len, fd)){
                        read_status = -1;
                    }else{
                        base_task *task = new tcp_task(buffer, fd , len);
                        pool->append_task(task);
                    }
                }

                READERROR:
                if (-1 == read_status){

                    read_status = 0;

                    struct epoll_event ev;
                    ev.events = EPOLLIN;
                    ev.data.fd = fd;

                    epoll_ctl(epollfd, EPOLL_CTL_DEL, fd, &ev);
                    shutdown(fd, SHUT_RDWR);
                    processs_map(fd);
                }

            }
            else if ((events[i].events & EPOLLERR) ||
                     (events[i].events & EPOLLHUP) ||
                     (!(events[i].events & EPOLLIN))){

                close (events[i].data.fd);
                processs_map(fd);
            }
            else
            {
                cout << "something else had happened" << endl;
            }
        }
    }

    close(listen_sockfd);//结束。

    pool->stop();
}

