#ifndef LOCKER_H
#define LOCKER_H

#include <pthread.h>
#include <stdio.h>
#include <semaphore.h>

/**
 * @brief The sem_locker class
 * 信号量
 */
class sem_locker{

private:
    sem_t m_sem;

public:
    //初始化信号量
    sem_locker(){

        if (sem_init(&m_sem, 0, 0) != 0){

            printf("sem init error\n");
        }
    }

    //销毁信号量
    ~sem_locker(){

        sem_destroy(&m_sem);
    }

    //等待信号量
    bool wait(){

        return sem_wait(&m_sem) == 0;
    }

    //添加信号量
    bool add(){

        return sem_post(&m_sem);
    }
};

/**
 * @brief The mutex_locker class
 * 互斥量
 */
class mutex_locker{

private:
    pthread_mutex_t m_mutex;

public:
    mutex_locker(){

        if (pthread_mutex_init(&m_mutex, NULL) != 0)
            printf("mutex init error");
    }

    ~mutex_locker(){

        pthread_mutex_destroy(&m_mutex);
    }

    bool lock(){

        return pthread_mutex_lock(&m_mutex) == 0;
    }

    bool unlock(){

        return pthread_mutex_unlock(&m_mutex) == 0;
    }
};

/**
 * @brief The cond_locker class
 * 条件变量
 */
class cond_locker{

private:
    pthread_mutex_t m_mutex;
    pthread_cond_t m_cond;

public:
    cond_locker(){

        if (pthread_mutex_init(&m_mutex, NULL) != 0)
            printf("cond_locker mutex init error");
        if (pthread_cond_init(&m_cond, NULL) != 0){

            pthread_mutex_destroy(&m_mutex);
            printf("cond_locker cond init error");
        }
    }

    ~cond_locker(){

        pthread_mutex_destroy(&m_mutex);
        pthread_cond_destroy(&m_cond);
    }

    bool wait(){

        int ans = 0;
        pthread_mutex_lock(&m_mutex);
        ans = pthread_cond_wait(&m_cond, &m_mutex);
        pthread_mutex_unlock(&m_mutex);
        return ans;
    }

    //唤醒等待条件变量的线程
    bool sigal(){

        return pthread_cond_signal(&m_cond) == 0;
    }

    //唤醒所有等待条件变量的线程
    bool broadcast(){

        return pthread_cond_broadcast(&m_cond) == 0;
    }
};

#endif // LOCKER_H

