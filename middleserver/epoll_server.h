#ifndef EPOLL_SERVER_H
#define EPOLL_SERVER_H

#include "thread_pool.h"
#include "base_task.h"

#include <sstream>
#include <iostream>
#define MAX_EVENT 1000

using namespace std;

class epoll_server{

private:

    int read_status;                //读状态
    static bool is_stop;            //是否停止epoll_wait的标志
    int thread_num;                	//线程数目
    int listen_sockfd;              //监听的fd
    int listen_port;               	//端口
    int epollfd;                    //Epoll的fd
    thread_pool<base_task> *pool;   //线程池的指针
    epoll_event events[MAX_EVENT];  //epoll的events数组
    struct sockaddr_in bind_addr;   //绑定的sockaddr

protected:

    int readData(char * bytes, int size, int sockfd){

        int sendLen = 0;
        int tmpLen = 0;

        while(sendLen < size){

            tmpLen = recv(sockfd, bytes+sendLen, size-sendLen, 0);
            if (0 < tmpLen){
                sendLen += tmpLen;
                continue;
            }
            if (0 == tmpLen){
                cout << "error number: " << errno << endl;
                return 0;
            }else{
                if (errno == EINTR || errno == EAGAIN || errno == EWOULDBLOCK){
                    continue;
                }
                cout << "error number: " << errno << endl;
                return 0;
            }
        }
        return 1;
    }

    int readData(string & buffer, int size, int sockfd){

        char tmp[size] = {0};
        int tmplen = 0;
        int sendlen = 0;

        while(sendlen < size){
            tmplen = recv(sockfd, tmp+sendlen, size-sendlen, 0);
            if (0 < tmplen){
                buffer.append(tmp+sendlen,tmplen);
                sendlen += tmplen;
                continue;
            }
            if (0 == tmplen){

                cout << "error number: " << errno << endl;
                return 0;
            }else{
                if (errno == EINTR || errno == EAGAIN || errno == EWOULDBLOCK){
                    continue;
                }
                cout << "error number: " << errno << endl;
                return 0;
            }
        }

        return 1;
    }

    uint readData(string &buffer, int sockfd){

        char    tmp[2] = {0};
        int     len = 0;

        len = recv(sockfd, tmp, sizeof(tmp), 0);
        if (0 < len){
            buffer.append(tmp,len);
            return 1;
        }
        if (0 == len){
            read_status = -1;
            return 0;
        }else{
            if (errno == EWOULDBLOCK || errno == EAGAIN){
                printf("read error! [EWOULDBLOCK EAGAIN]\n");
                return 1;
            }
            if (errno == EINTR){
                printf("read error! [EINTR]");
                return 1;
            }
            read_status = -1;
            return 0;
        }

    }

    uint readSize(string &buffer, string &str, ulong size){

        str.clear();

        if (buffer.size() < size){
            return 0;
        }

        str.assign(buffer, 0, size);
        buffer.erase(0, size);
        return 1;
    }

    uint readLine(string & buffer, string & line){

        ulong linePos = buffer.find('\n');
        ulong lineLen = linePos + 1;

        line.clear();

        if (string::npos == linePos){
            return 0;
        }
        if (0 < linePos && '\r' == buffer[linePos - 1]){
            linePos--;
        }
        line.assign(buffer, 0, linePos);
        buffer.erase(0, lineLen);
        return 1;
    }

public:
    epoll_server():thread_num(20),listen_port(9999),pool(NULL),read_status(0){}

    epoll_server(int port, int threadnum) :
        thread_num(threadnum) ,
        listen_port(port),
        pool(NULL),
        read_status(0)
    {
    }

    ~epoll_server()
    {
        delete pool;
    }

    //将fd设置称非阻塞
    static int setnonblocking(int fd)
    {
        int old_option = fcntl(fd, F_GETFL);
        int new_option = old_option | O_NONBLOCK;
        fcntl(fd, F_SETFL, new_option);
        return old_option;
    }

    static void addfd(int epollfd, int sockfd, bool oneshot)  //向Epoll中添加fd
    {
        //oneshot表示是否设置称同一时刻，只能有一个线程访问fd，数据的读取都在主线程中，所以调用都设置成false
        epoll_event event;
        event.data.fd = sockfd;
        event.events = EPOLLIN | EPOLLET;
        if(oneshot)
        {
            event.events |= EPOLLONESHOT;
        }
        epoll_ctl(epollfd, EPOLL_CTL_ADD, sockfd, &event); //添加fd
        epoll_server::setnonblocking(sockfd);
    }

    static void processs_map(int sockfd);

    bool init();

    void epoll();

};

#endif // EPOLL_SERVER_H

