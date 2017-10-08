package com.androidexample.chenn.androidclient.msgservice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by chenn on 2017/9/20.
 */

public final class MsgReceive {

    private Socket client;

    private OutputStream outStr = null;

    public boolean runfalg = true;

    private InputStream inStr = null;

    private Thread tRecv = null;
    private Thread tKeep = null;

    private Handler handler;
    private byte[] snd_bytes;


    public MsgReceive(Handler handler, byte[] snd_pulse){

        this.handler = handler;
        this.snd_bytes = snd_pulse;
    }

    private void send_content(int what, Bundle bundle){

        Message msg = new Message();
        if (null != bundle) {
            msg.setData(bundle);
        }
        msg.what = what;
        handler.sendMessage(msg);
    }

    private void send_err(String err){

        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("content", err);
        msg.what = Const.MSG_MAIN_ERR;
        msg.setData(b);
        handler.sendMessage(msg);
    }

    public void stop(){

        runfalg = false;
    }

    public void reStart(byte[] snd_pulse){

        this.snd_bytes = snd_pulse;

        //重启命令接收线程
        runfalg = false;

        try{
            if (null != tKeep && tKeep.isAlive())tKeep.join();
        } catch (InterruptedException e) {
            Log.i(Const.TAG,"tKeep close:"+e.getMessage());
            send_err("tKeep close:"+e.getMessage());
            disconnect();
        } finally {
            tKeep = null;
        }

        try {
            if (null != tRecv && tRecv.isAlive())tRecv.join(1000);
        } catch (InterruptedException e) {
            Log.i(Const.TAG,"tRecv close:"+e.getMessage());
            send_err("tRecv close:"+e.getMessage());
        }finally {
            tRecv = null;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i(Const.TAG,"连接重置");
        send_err("连接重置");

        start();
    }

    private void processCommand(String info) {

        String type;
        String content;
        try {
            info = info.replaceAll("[\u0000-\u001f]", "");
            info = info.replaceAll("\n","");
            StringReader sr = new StringReader(info);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            type = doc.getElementsByTagName("type").item(0).getFirstChild().getNodeValue();
            if ("1000".equals(type)){
                //接受消息推送
                content = doc.getElementsByTagName("content").item(0).getFirstChild().getNodeValue();
                content = content.trim();
                content = "推送消息：\n" + content;

                Bundle b = new Bundle();
                b.putString("content", content);
                send_content(Const.MSG_MAIN_RECEIVE, b);

            }else if ("0000".equals(type)){
                //初始化上线成员
                content = doc.getElementsByTagName("content").item(0).getFirstChild().getNodeValue();
                content = content.trim();

                Bundle b = new Bundle();
                b.putString("content", content);
                send_content(Const.MSG_MAIN_SETMEMBER,b);

            }else if ("0001".equals(type)){
                //接受聊天消息
                content = doc.getElementsByTagName("content").item(0).getFirstChild().getNodeValue();
                content = content.trim();
                String userno = doc.getElementsByTagName("userno").item(0).getFirstChild().getNodeValue();
                content = userno + "\n" + content;

                Bundle b = new Bundle();
                b.putString("content", content);
                send_content(Const.MSG_MAIN_RECEIVE,b);

            }else if ("0002".equals(type)){
                //其他
                //...
            }
        } catch (SAXException e) {
            Log.i(Const.TAG,"sendCommand error:"+e.getMessage());
            send_err("sendCommand error:"+e.getMessage());
        } catch (ParserConfigurationException e) {
            Log.i(Const.TAG,"sendCommand error:"+e.getMessage());
            send_err("sendCommand error:"+e.getMessage());
        } catch (IOException e) {
            Log.i(Const.TAG,"sendCommand error:"+e.getMessage());
            send_err("sendCommand error:"+e.getMessage());
        }

    }

    public void connect() throws IOException {

        client = new Socket(Const.SERVER_IP, Const.SERVER_PORT);
        outStr = client.getOutputStream();
        inStr = client.getInputStream();

        tKeep = new Thread(new KeepThread());
        tKeep.start();

        tRecv = new Thread(new RecvThread());
        tRecv.start();
    }

    public void disconnect() {
        try {
            if (null != client) {client.close();client=null;}
            if (null != inStr) {inStr.close();inStr=null;}
            if (null != outStr) {outStr.close();outStr=null;}
        } catch (IOException e) {

            Log.i(Const.TAG,"关闭接收线程异常:"+e.getMessage());
            send_err("关闭接收线程异常:"+e.getMessage());
        }

    }
    private class KeepThread implements Runnable {
        public void run() {

            Log.i(Const.TAG,"启动心跳线程");
            send_err("启动心跳线程");
            try{

                //第一次发送
                outStr.write(snd_bytes);

                while(runfalg){

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    outStr.write(snd_bytes);
                }

            } catch (UnsupportedEncodingException e) {

                Log.i(Const.TAG,"心跳异常:"+e.getMessage());
                send_err("心跳异常:"+e.getMessage());
            } catch (IOException e) {

                Log.i(Const.TAG,"心跳异常:"+e.getMessage());
                send_err("心跳异常:"+e.getMessage());
            } finally {

                disconnect();
            }

            Log.i(Const.TAG,"结束心跳线程");
            send_err("结束心跳线程");

        }
    }

    private class RecvThread implements Runnable {
        public void run() {
            Log.i(Const.TAG,"启动接收线程");
            send_err("启动接收线程");
            try {

                while (runfalg){

                    byte[] head = new byte[4];
                    //读四位包头
                    int number = inStr.read(head);
                    if (-1 < number) {

                        //将四字节head转换为int类型
                        ByteBuffer bb = ByteBuffer.wrap(head);
                        int recvSize = bb.order(ByteOrder.BIG_ENDIAN).getInt();
                        byte[] info = new byte[recvSize];
                        number = inStr.read(info, 0, recvSize);

                        Log.i(Const.TAG,"receive head:"+recvSize);
                        send_err("receive head:"+recvSize);

                        if (-1 < number){

                            //转换网络字节为string
                            String xmlinfo = Const.get_str_from_sockebytes(info);
                            Log.i(Const.TAG,"receive body:"+xmlinfo);
                            send_err("receive body:"+xmlinfo);
                            processCommand(xmlinfo);
                        }
                    }

                }
            } catch (IOException e) {
                Log.i(Const.TAG,"接收线程"+e.getMessage());
                send_err("接收线程"+e.getMessage());
            } catch (NullPointerException e){
                Log.i(Const.TAG,"接收线程"+e.getMessage());
                send_err("接收线程"+e.getMessage());
            }

            Log.i(Const.TAG,"结束接收线程");
            send_err("结束接收线程");

        }
    }

    public void start(){

        try {
            if (tRecv == null || (!tRecv.isAlive())) {
                runfalg = true;
                connect();
            }
        } catch (IOException e) {
            Log.i(Const.TAG,"start MsgReceive failed:" + e);
            send_err("接收线程启动失败"+e.getMessage());
        }

    }

}
