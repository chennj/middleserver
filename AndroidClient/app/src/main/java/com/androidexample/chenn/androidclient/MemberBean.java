package com.androidexample.chenn.androidclient;

import java.io.Serializable;

/**
 * Created by chenn on 2017/9/20.
 */

public class MemberBean implements Serializable {

    private String id = "";
    private String no = "";

    public MemberBean(String id, String no){

        this.id = id;
        this.no = no;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }
}
