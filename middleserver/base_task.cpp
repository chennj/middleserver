#include "base_task.h"
#include "context.h"
#include "string_utls.h"
#include <list>

bool tcp_task::send_data(const string &buffer, int fd){

    return send_data(buffer.c_str(), buffer.size(), fd);
}

bool tcp_task::send_data(const char * buffer, uint size, int fd){

    uint    sendLen = 0;
    int     tmpLen = 0;

    if (NULL == buffer){
        return false;
    }

    while (sendLen < size){
        tmpLen = send(fd, buffer+sendLen, size-sendLen, MSG_NOSIGNAL);
        if (-1 == tmpLen){
            return false;
        }
        sendLen += tmpLen;
    }

    return true;

}

string tcp_task::get_xml(const cliusr & cu){

    string xml_l;
    xml_l = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    xml_l+= "<package><body>";
    xml_l+= "<type>"+cu.type+"</type>";
    xml_l+= "<content>"+cu.content+"</content>";
    xml_l+= "<groudid>"+cu.selfgroupid+"</groudid>";
    xml_l+= "<userno>"+cu.selfuserno+"</userno>";
    xml_l+= "</body></package>\n";

    /*
    uint len = xml_l.size();
    stringstream ss;
    string len_s;
    ss<<len;
    ss>>len_s;
    char c[4] = {'0','0','0','0'};
    uint sl = len_s.size();
    for (uint i=0; i<sl; i++){
        c[4-sl+i] = len_s[i];
    }
    string xml_h = c;
    string xml = xml_h + xml_l;

    return xml;
    */
    return xml_l;
}

string tcp_task::assemble_send_msg(const string & str){

    char head[4];
    string_utls::int_to_bytes_big(str.size(), head);
    string head_s;
    head_s.append(head, 4);

    return head_s + str;
}

int tcp_task::parse_body(string & xml, cliusr & cu){

    string type;
    string content;
    string parentid;
    string selfgroupid;
    string selfuserno;
    string peergroupid;
    string peeruserno;

    string_utls::simple_regex_match(xml.c_str(),"<type>(.*)<\\/type>",type);
    cout << "type=" << type << endl;

    string_utls::simple_regex_match(xml.c_str(),"<content>(.*)<\\/content>",content);
    cout << "content=" << content << endl;

    string_utls::simple_regex_match(xml.c_str(),"<parentid>(.*)<\\/parentid>",parentid);
    cout << "parentid=" << parentid << endl;

    string_utls::simple_regex_match(xml.c_str(),"<selfgroupid>(.*)<\\/selfgroupid>",selfgroupid);
    cout << "selfgroupid=" << selfgroupid << endl;

    string_utls::simple_regex_match(xml.c_str(),"<selfuserno>(.*)<\\/selfuserno>",selfuserno);
    cout << "selfuserno=" << selfuserno << endl;

    string_utls::simple_regex_match(xml.c_str(),"<peergroupid>(.*)<\\/peergroupid>",peergroupid);
    cout << "peergroupid=" << peergroupid << endl;

    string_utls::simple_regex_match(xml.c_str(),"<peeruserno>(.*)<\\/peeruserno>",peeruserno);
    cout << "peeruserno=" << peeruserno << endl;

    if (!type.empty()){

        cu.type         = type;
        cu.content      = content;
        cu.parentid     = parentid;
        cu.selfgroupid  = selfgroupid;
        cu.selfuserno   = selfuserno;
        cu.peergroupid  = peergroupid;
        cu.peeruserno   = peeruserno;
    }

    return 0;
}

void tcp_task::doit(){

    //cout << "收到的原始信息：" << order << endl;
    //cout << "socket fd:" << sockfd << endl;
    cout << "map size:" << context::g_cliusrs.size() << endl;

    //分析xml字符串
    cliusr cu;
    parse_body(order, cu);

    //处理通讯
    if (!(cu.type.empty())){

        if ("1000" == cu.type){     //第三方发往手机的推送信息

            cout << "推送消息" << endl;

            string snd_msg = assemble_send_msg(get_xml(cu));

            std::list<int> list;
            context::map_mutex_locker.lock();
            hash_map<string, cliusr>::iterator it = context::g_cliusrs.begin();
            while(context::g_cliusrs.end() != it){

                int fd = it++->second.self_sockfd;
                list.push_back(fd);
            }
            context::map_mutex_locker.unlock();

            if (list.size() > 0){

                std::list<int>::iterator it = list.begin();
                while(list.end() != it){

                    send_data(snd_msg, (*it));
                    it++;
                }
            }

            //发回：表示推送任务结束
            send_data(snd_msg, sockfd);

        }//end 1000

        if ("0001" == cu.type){     //聊天类型

            cout << "聊天消息" << endl;

            string key = cu.peeruserno;

            if( (!key.empty()) && ("none" != key)){

                bool found = false;
                int target_sockfd;
                context::map_mutex_locker.lock();
                hash_map<string, cliusr>::iterator iter = context::g_cliusrs.find(key);
                if (context::g_cliusrs.end() != iter){
                    found = true;
                    target_sockfd = iter->second.self_sockfd;
                }
                context::map_mutex_locker.unlock();

                if (found){

                    string snd_msg = assemble_send_msg(get_xml(cu));
                    if (!send_data(snd_msg, target_sockfd)){


                        cu.type = "9999";
                        cu.content = "发送失败";

                        string snd_err_s = assemble_send_msg(get_xml(cu));

                        send_data(snd_err_s, sockfd);
                    }else{

                        cu.type = "8888";
                        cu.content = "发送成功";

                        string snd_success_s = assemble_send_msg(get_xml(cu));

                        send_data(snd_success_s, sockfd);
                    }
                }
            }
        }//end 0001

        if ("0002" == cu.type){     //心跳类型

            string key = cu.selfuserno;
            string content="";

            context::map_mutex_locker.lock();
            hash_map<string, cliusr>::iterator iter = context::g_cliusrs.find(key);
            if (context::g_cliusrs.end() == iter){
                //第一次插入缓存
                cu.self_sockfd = sockfd;
                context::g_relation.insert(hash_map<int,string>::value_type(sockfd,key));
                context::g_cliusrs.insert(hash_map<string,cliusr>::value_type(key,cu));
            }else{
                //如果有变化更改socket fd
                int prev_sockfd = iter->second.self_sockfd;
                hash_map<int, string>::iterator iter_key = context::g_relation.find(prev_sockfd);
                if ( (context::g_relation.end() != iter_key)
                     && (prev_sockfd != sockfd)){
                   context::g_relation.erase(iter_key);
                   context::g_relation.insert(hash_map<int,string>::value_type(sockfd,key));
                   iter->second.self_sockfd = sockfd;
                }
            }
            //遍历找出所有的用户发回,除了自己
            if (context::g_cliusrs.size()>0){
                for(iter = context::g_cliusrs.begin(); iter != context::g_cliusrs.end(); iter++){
                    if (key != iter->first)
                        content += iter->second.selfgroupid + ":" + iter->second.selfuserno + "  ";
                }
            }
            context::map_mutex_locker.unlock();

            if (!content.empty()){
                //发送已经上线的成员
                cu.type = "0000";
                cu.content = content;
                send_data(assemble_send_msg(get_xml(cu)), sockfd);
            }
            cout << "心跳：" << key << endl;

        }//end 0002

    }//end 处理通讯结束

    return;
}

