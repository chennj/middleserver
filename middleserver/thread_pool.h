#ifndef THREAD_POOL_H
#define THREAD_POOL_H

#include "locker.h"
#include <queue>
#include <stdio.h>
#include <exception>
#include <errno.h>
#include <iostream>

using namespace std;

template <class T>
class thread_pool{

private:
    uint            thread_number;              //线程池的线程数
    pthread_t *     all_threads;                //线程数组
    std::queue<T*>  task_queue;                 //任务队列
    mutex_locker    queue_mutex_locker;         //互斥锁
    cond_locker     queue_cond_locker;          //条件变量
    bool            is_stop;                    //是否结束线程

private:
    static void *   worker(void * args);        //线程运行时的函数，执行run();
    void            run();                      //线程执行函数
    T *             get_task();                 //获取任务

public:
    thread_pool(uint thread_num = 20);
    ~thread_pool();

    bool            append_task(T * task_queue);//添加任务
    void            start();                    //线程池开启
    void            stop();                     //线程池关闭
};

template <class T>
thread_pool<T>::thread_pool(uint thread_num):
    thread_number(thread_num),
    is_stop(false),
    all_threads(NULL)
{
    if (0 >= thread_num){

        cout << "threadpool can't init because thread_number = 0" << endl;
        thread_number = 20;
    }

    all_threads = new pthread_t[thread_number];
    if (NULL == all_threads){

        cout << "can't init threadpool because thread array can't new" << endl;
        throw std::exception();
    }
}

template <class T>
thread_pool<T>::~thread_pool(){

    delete [] all_threads;
    stop();
}

template <class T>
void thread_pool<T>::start(){

    for (uint i=0; i<thread_number; ++i){

        if (pthread_create(all_threads+i, NULL, worker, this) != 0){

            //创建线程失败，清除成功申请的资源并抛出异常
            delete [] all_threads;
            throw std::exception();
        }

        if (pthread_detach(all_threads[i])){

            //将线程设置为脱离线程，失败则清除成功申请的资源并抛出异常
            delete[] all_threads;
            throw std::exception();
        }
    }
}

template <class T>
void thread_pool<T>::stop(){

    is_stop = true;
    queue_cond_locker.broadcast();
}

template <class T>
bool thread_pool<T>::append_task(T *task){

    queue_mutex_locker.lock();

    bool need_signal = task_queue.empty();

    task_queue.push(task);

    queue_mutex_locker.unlock();

    if (need_signal)
        queue_cond_locker.sigal();

    return true;
}

template <class T>
void * thread_pool<T>::worker(void *args){

    thread_pool * pool = (thread_pool*)args;
    pool->run();
    return pool;
}

template <class T>
T* thread_pool<T>::get_task(){

    T * task = NULL;

    queue_mutex_locker.lock();
    if (!task_queue.empty()){

        task = task_queue.front();
        task_queue.pop();
    }
    queue_mutex_locker.unlock();

    return task;
}

template <class T>
void thread_pool<T>::run(){

    while (!is_stop){

        T * task = get_task();
        if (NULL == task){

            queue_cond_locker.wait();
        }else{

            task->doit();
        }
    }

    //debug
    cout << "exit" << (ulong)pthread_self() << endl;
}

#endif // THREAD_POOL_H

