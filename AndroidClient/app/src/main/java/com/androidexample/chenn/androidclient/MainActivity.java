package com.androidexample.chenn.androidclient;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.androidexample.chenn.androidclient.msgservice.Const;
import com.androidexample.chenn.androidclient.msgservice.MessageThread;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText ed_id;
    private EditText ed_no;
    private EditText ed_ip;
    private TextView tx_info;
    private ListView lv_member;
    private TextView tx_member;
    private TextView tx_err;
    private EditText ed_snd;
    private TextView tx_clear;

    private static MessageThread messageThread;

    private Handler handler;

    private MemberAdapter memberAdapter;

    private List<MemberBean> memberBeanList;

    private String toid;
    private String tono;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//
        setContentView(R.layout.activity_main);

        handler = new MyHandler(this);

        start_message_thread();

        init();
    }

    /**
     * 向消息线程发送消息
     * @param what
     * @param bundle
     */
    private void send_msg(int what, Bundle bundle){

        if ((null == messageThread) || (!messageThread.isAlive())){

            show_content("消息线程未能成功启动");
            return;
        }

        Message msg = new Message();
        if (null != bundle) {
            msg.setData(bundle);
        }
        msg.what = what;
        messageThread.getHandler().sendMessage(msg);
    }

    /**
     * 启动后台消息线程
     */
    private void start_message_thread(){

        if (null == messageThread || (!messageThread.isAlive())) {

            messageThread = new MessageThread(handler);
            messageThread.start();
        }
    }

    /**
     * 初始化
     */
    private void init(){

        toid = "*";
        tono = "*";

        ed_id       = (EditText) findViewById(R.id.ed_gongsiid);
        ed_no       = (EditText) findViewById(R.id.ed_weixinhao);
        ed_ip       = (EditText) findViewById(R.id.ed_address);
        tx_info     = (TextView) findViewById(R.id.tx_content);
        lv_member   = (ListView) findViewById(R.id.lv_member);
        tx_member   = (TextView) findViewById(R.id.tx_member);
        tx_err      = (TextView) findViewById(R.id.tx_err);
        ed_snd      = (EditText) findViewById(R.id.ed_snd);
        tx_clear    = (TextView) findViewById(R.id.tx_clear);

        Button bt_open = (Button)findViewById(R.id.bt_ok);
        bt_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                tx_info.setText("");

                if (!check_param()){
                    show_err("组id或者用户id不能为空或者服务器地址不能为空");
                    return;
                }

                Const.CURRENT_ID    = ed_id.getText().toString().trim();
                Const.CURRENT_NO    = ed_no.getText().toString().trim();
                Const.SERVER_IP     = ed_ip.getText().toString().trim();

                Bundle b = new Bundle();
                String to = tx_member.getText().toString().trim();

                b.putString("id", ed_id.getText().toString().trim());
                b.putString("no", ed_no.getText().toString().trim());
                b.putString("ip", ed_ip.getText().toString().trim());

                send_msg(Const.MSG_CMD_OPEN,b);
            }
        });

        Button bt_reset = (Button)findViewById(R.id.bt_cancel);
        bt_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!check_param()){
                    show_err("组id或者用户id不能为空或者服务器地址不能为空");
                    return;
                }

                Const.CURRENT_ID    = ed_id.getText().toString().trim();
                Const.CURRENT_NO    = ed_no.getText().toString().trim();
                Const.SERVER_IP     = ed_ip.getText().toString().trim();

                send_msg(Const.MSG_CMD_RESET,null);
            }
        });

        Button bt_snd = (Button)findViewById(R.id.bt_snd);
        bt_snd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!check_param()){
                    show_err("组id或者用户id不能为空");
                    return;
                }

                String content = ed_snd.getText().toString().trim();

                if (content.isEmpty()){
                    show_err("发送内容不能为空!");
                }
                Bundle b = new Bundle();
                b.putString("id", toid);
                b.putString("no", tono);
                b.putString("content",ed_snd.getText().toString().trim());

                send_msg(Const.MSG_CMD_SEND, b);

                show_content(tono + ":\n" + ed_snd.getText().toString().trim());
                ed_snd.setText("");
            }
        });

        lv_member.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                memberAdapter.setSelectedPosition(position);
                memberAdapter.notifyDataSetInvalidated();
                MemberBean memberBean = (MemberBean)parent.getItemAtPosition(position);

                toid = memberBean.getId().trim();
                tono = memberBean.getNo().trim();

                tx_member.setText(toid+":"+tono);
            }
        });

        tx_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tx_member.setText("");
            }
        });
    }

    private void show_err(String content){

        tx_err.append(content);
        tx_err.append("\n");
    }

    /**
     * 显示信息
     * @param content
     */
    private void show_content(String content){

        tx_info.append(content);
        tx_info.append("\n");
    }

    /**
     * 检查参数
     * @return
     */
    private boolean check_param(){

        String id = ed_id.getText().toString().trim();
        String no = ed_no.getText().toString().trim();
        String ip = ed_ip.getText().toString().trim();

        if (id.isEmpty() || no.isEmpty() || ip.isEmpty()){
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        try {
            if (null != messageThread && messageThread.isAlive()) {
                send_msg(Const.MSG_MAIN_EXIT, null);
                messageThread.join(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void set_member_list(String content){

        if (content.isEmpty()){
            return;
        }

        if (null != memberBeanList && memberBeanList.size()>0)
            memberBeanList.clear();
        else
            memberBeanList = new ArrayList<>();

        String[] arr = content.split("\\s+");
        for (String s : arr) {
            String[] pair = s.split("\\:");
            if (2 == pair.length) {
                String id = pair[0];
                String no = pair[1];

                memberBeanList.add(new MemberBean(id,no));
            }
        }

        if (memberBeanList.size() == 0)return;

        memberAdapter = new MemberAdapter(this, memberBeanList);
        memberAdapter.setSelectedPosition(0);
        lv_member.setAdapter(memberAdapter);
    }

    /**
     * 消息处理
     */
    private static class MyHandler extends Handler{

        private WeakReference<MainActivity> activity;

        private MyHandler(){}

        MyHandler(Context context){

            activity = new WeakReference<MainActivity>((MainActivity)context);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){

                case Const.MSG_MAIN_RECEIVE:{
                    Bundle b = msg.getData();
                    String content = b.getString("content");
                    activity.get().show_content(content);
                    break;
                }

                case Const.MSG_MAIN_SETMEMBER:{
                    Bundle b = msg.getData();
                    String content = b.getString("content");
                    activity.get().set_member_list(content);
                    break;
                }

                case Const.MSG_MAIN_ERR:{

                    Bundle b = msg.getData();
                    String content = b.getString("content");
                    activity.get().show_err(content);
                    break;
                }

                default:
                    break;
            }
        }

    }
}
