MAKEFILE= Makefile

SOURCES += main.cpp \
    context.cpp \
    string_utls.cpp \
    base_task.cpp \
	thread_pool.cpp \
    epoll_server.cpp
	
INCLUDE = -I./
CXX		= g++
LIBS	= -lpthread
FLAGS	= -Wall -g

.DAZED: middleserver

middleserver:
	$(CXX) -o middleserver $(SOURCES) $(INCLUDE) $(LIBS) $(FLAGS)
	
clean:
	rm -rf middleserver