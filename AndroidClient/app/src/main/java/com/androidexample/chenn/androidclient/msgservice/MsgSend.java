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
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by chenn on 2017/9/20.
 */

public class MsgSend {

    private Handler handler;

    private Socket client;

    private OutputStream outStr = null;

    private InputStream inStr = null;

    private byte[] sndBytes;

    public MsgSend(Handler handler, byte[] sndBytes){

        this.handler = handler;
        this.sndBytes = sndBytes;
    }

    private void send_err(String err){

        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("content", err);
        msg.what = Const.MSG_MAIN_ERR;
        msg.setData(b);
        handler.sendMessage(msg);
    }

    private void processReInfo(String info){

        String type = "";
        if (null == info){
            //接收超时
            type = "9999";
        }else {
            try {
                info = info.replaceAll("[\u0000-\u001f]", "");
                info = info.replaceAll("\n", "");
                StringReader sr = new StringReader(info);
                InputSource is = new InputSource(sr);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(is);
                type = doc.getElementsByTagName("type").item(0).getFirstChild().getNodeValue();
                type = type.trim();
            } catch (SAXException e) {
                Log.i(Const.TAG,"sendCommand error:" + e.getMessage());
                send_err("sendCommand error:" + e.getMessage());
            } catch (ParserConfigurationException e) {
                Log.i(Const.TAG,"sendCommand error:" + e.getMessage());
                send_err("sendCommand error:" + e.getMessage());
            } catch (IOException e) {
                Log.i(Const.TAG,"sendCommand error:" + e.getMessage());
                send_err("sendCommand error:" + e.getMessage());
            }
        }
        if ("9999".equals(type)){

            //处理传送失败
            Log.i(Const.TAG,"传送失败");
            send_err("传送失败");
        }else{
            Log.i(Const.TAG,"传送成功");
            send_err("传送成功");
        }
    }

    public void connect() throws IOException {

        client = new Socket(Const.SERVER_IP, Const.SERVER_PORT);
        outStr = client.getOutputStream();
        inStr = client.getInputStream();
    }

    private class AnswerThread implements Runnable{

        @Override
        public void run() {

            try{
                //发送报文
                outStr.write(sndBytes);
                outStr.flush();

                Log.i(Const.TAG,"msgsend:"+String.valueOf(sndBytes));

                client.setSoTimeout(3000);

                byte[] head = new byte[4];
                //读四位包头
                int number = inStr.read(head);
                if (-1 < number){
                    //将四字节head转换为int类型
                    ByteBuffer bb = ByteBuffer.wrap(head);
                    int recvSize = bb.order(ByteOrder.BIG_ENDIAN).getInt();
                    byte[] info = new byte[recvSize];
                    number = inStr.read(info);

                    Log.i(Const.TAG,"msgsend return size:"+recvSize);

                    if (-1 < number){
                        //转换网络字节为string
                        String xmlinfo = Const.get_str_from_sockebytes(info);
                        Log.i(Const.TAG,"msgsend return info:"+xmlinfo);
                        processReInfo(xmlinfo);
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start(){

        try {
            connect();
            Thread tAnswer = new Thread(new AnswerThread());
            tAnswer.start();
        } catch (Exception e) {
            Log.i(Const.TAG,"发送线程启动错误" + e);
            send_err("发送线程启动错误"+e);
        }
    }

}
