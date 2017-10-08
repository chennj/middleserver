package com.androidexample.chenn.androidclient.msgservice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.UnsupportedEncodingException;

/**
 * Created by chenn on 2017/9/20.
 */

public class MessageThread extends Thread {


    private Handler handler;
    private Handler main_activity_handler;
    private static MsgReceive receive = null;

    public MessageThread(Handler handler){

        main_activity_handler = handler;
    }

    public Handler getHandler(){
        return handler;
    }

    @Override
    public void run() {

        Looper.prepare();
        handler = new MessageHandler(main_activity_handler);
        Looper.loop();
    }


    private static class MessageHandler extends Handler{

        private Handler handler;

        private MessageHandler(){}

        MessageHandler(Handler handler){

            this.handler = handler;

        }

        private void send_msg_to_main(int what, Bundle bundle){

            Message msg = new Message();
            if (null != bundle) {
                msg.setData(bundle);
            }
            msg.what = what;
            handler.sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {

            if (!Const.isReachable(Const.SERVER_IP)){
                Bundle snd_b = new Bundle();
                snd_b.putString("content", "目标地址不可达");
                send_msg_to_main(Const.MSG_MAIN_ERR,snd_b);
                return;
            }

            switch (msg.what){

                case Const.MSG_CMD_OPEN:{

                    byte[] snd_pulse;
                    try {
                        //0002表示MsgReceive启动后向服务器发送心跳连接
                        snd_pulse = Const.assembleSendXml("0002", "none", "none", "pulse");
                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();
                        Bundle snd_b = new Bundle();
                        snd_b.putString("content", e.getMessage());
                        send_msg_to_main(Const.MSG_MAIN_ERR, snd_b);
                        return;
                    }

                    if (null == receive)receive = new MsgReceive(handler, snd_pulse);
                    receive.start();

                    break;
                }

                case Const.MSG_CMD_RESET:{

                    if (null == receive){
                        return;
                    }

                    byte[] snd_pulse;
                    try {
                        //0002表示MsgReceive启动后向服务器发送心跳连接
                        snd_pulse = Const.assembleSendXml("0002", "none", "none", "pulse");
                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();
                        Bundle snd_b = new Bundle();
                        snd_b.putString("content", e.getMessage());
                        send_msg_to_main(Const.MSG_MAIN_ERR, snd_b);
                        return;
                    }

                    receive.reStart(snd_pulse);
                    break;
                }

                case Const.MSG_CMD_SEND:{

                    Bundle b = msg.getData();
                    String toid = b.getString("id");
                    String tono = b.getString("no");
                    String content = b.getString("content");

                    byte[] snd_bytes;

                    try {
                        snd_bytes = Const.assembleSendXml("0001", toid, tono, content);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        Bundle snd_b = new Bundle();
                        snd_b.putString("content", e.getMessage());
                        send_msg_to_main(Const.MSG_MAIN_ERR, snd_b);
                        return;
                    }

                    new MsgSend(handler, snd_bytes).start();
                    break;
                }

                case Const.MSG_MAIN_RECEIVE:{

                    //获得来之MsgReceive的信息
                    Bundle b = msg.getData();
                    String content = b.getString("content");

                    //向主线程发送信息
                    Bundle snd_b = new Bundle();
                    snd_b.putString("content", content);
                    send_msg_to_main(Const.MSG_MAIN_RECEIVE, snd_b);

                    break;
                }

                case Const.MSG_MAIN_SETMEMBER:{

                    Bundle b = msg.getData();
                    String content = b.getString("content");

                    Bundle snd_b = new Bundle();
                    snd_b.putString("content", content);
                    send_msg_to_main(Const.MSG_MAIN_SETMEMBER, snd_b);

                    break;
                }
                case Const.MSG_MAIN_EXIT:{

                    if (null == receive)return;
                    receive.stop();
                    Looper.myLooper().quit();
                    break;
                }

            }//end switch(msg.what)
        }
    }//end MessageHandler
}
