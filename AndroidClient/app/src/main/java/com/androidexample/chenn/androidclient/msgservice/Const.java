package com.androidexample.chenn.androidclient.msgservice;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chenn on 2017/9/20.
 */

public class Const {

    public final static String TAG = "chenn-->";

    public static volatile String SERVER_IP     = "192.168.0.122";
    public static volatile int SERVER_PORT      = 9999;

    public static volatile String CURRENT_ID    = "none";
    public static volatile String CURRENT_NO    = "none";

    public final static int MSG_MAIN_RECEIVE    = 100;
    public final static int MSG_CMD_OPEN        = 101;
    public final static int MSG_CMD_RESET       = 102;
    public final static int MSG_MAIN_EXIT       = 103;
    public final static int MSG_MAIN_SETMEMBER  = 104;  //初始化上线的成员
    public final static int MSG_CMD_SEND        = 105;
    public final static int MSG_MAIN_ERR        = 106;

    /**
     * 网络byte类型的字节不能转成string，需要进行转换
     * @param bytes
     * @return
     */
    public static String get_str_from_sockebytes(byte[] bytes){

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        InputStreamReader isr = new InputStreamReader(bis);
        BufferedReader br = new BufferedReader(isr);

        String line = ""; String result = "";
        try {
            while ((line = br.readLine()) != null){

                result += line;
            }
        } catch (IOException e) {

            Log.i("cnj->read bytes:",e.getMessage());
        }

        return result;
    }

    /**
     * 合并两个byte数组
     * @param byte_1
     * @param byte_2
     * @return
     */
    public static byte[] bytes_megrer(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }


    static String parse(String s, String regx, String dep){

        Pattern pattern = Pattern.compile(regx,Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(s);

        String result="";

        while(matcher.find()){

            int groupcount = matcher.groupCount();
            for (int i=1; i<=groupcount; i++){

                result += matcher.group(i)+((dep==null)?"":dep);
            }
        }

        return result;
    }

    public static byte[] assembleSendXml(String type, String toid, String tono, String content) throws UnsupportedEncodingException {

        String xmlmsg = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
                "<package><body>"+
                "<type>"+type+"</type>"+
                "<content>"+content+"</content>"+
                "<parentid>0</parentid>"+
                "<selfgroupid>"+CURRENT_ID+"</selfgroupid>"+
                "<selfuserno>"+CURRENT_NO+"</selfuserno>"+
                "<peergroupid>"+toid+"</peergroupid>"+
                "<peeruserno>"+tono+"</peeruserno>"+
                "</body></package>";

        //将int类型转为网络大字节序的4个byte
        int xmlmsg_l = xmlmsg.getBytes("utf-8").length;
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.asIntBuffer().put(xmlmsg_l);
        byte[] head = bb.array();

        //将发送信息转为byte[]
        return bytes_megrer(head, xmlmsg.getBytes());
    }

    public static boolean isReachable(String remoteAddr){

        boolean isReachable = false;
        try{
            InetAddress address = InetAddress.getByName(remoteAddr);
            isReachable = address.isReachable(2000);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isReachable;
    }
}
