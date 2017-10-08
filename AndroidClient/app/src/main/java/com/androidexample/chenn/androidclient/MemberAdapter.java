package com.androidexample.chenn.androidclient;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by chenn on 2017/9/20.
 */

public class MemberAdapter extends BaseAdapter {

    private Context context;
    private int selectedPosition = -1;

    private List<MemberBean> memberBeanList = null;
    private LayoutInflater inflater = null;

    public MemberAdapter(Context context, List<MemberBean> memberBeanList){

        this.context = context;
        this.memberBeanList = memberBeanList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return memberBeanList.size();
    }

    @Override
    public Object getItem(int i) {
        return memberBeanList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ViewHolder viewHolder;

        if (view == null){

            view = inflater.inflate(R.layout.member, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.layout = (LinearLayout)view.findViewById(R.id.layout_member);
            viewHolder.tx_id = (TextView)view.findViewById(R.id.tx_item_id);
            viewHolder.tx_no = (TextView)view.findViewById(R.id.tx_item_no);
            view.setTag(viewHolder);
        }else{

            viewHolder = (ViewHolder)view.getTag();
        }

        MemberBean memberBean = memberBeanList.get(i);
        viewHolder.tx_id.setText(memberBean.getId());
        viewHolder.tx_no.setText(memberBean.getNo());

        /**
         * 设置选中效果
         */
        if (selectedPosition == i){

            viewHolder.layout.setBackgroundColor(Color.RED);
        }else{

            viewHolder.layout.setBackgroundColor(0xC9C7CD);
        }

        return view;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    static class ViewHolder{

        LinearLayout layout;
        TextView tx_id;
        TextView tx_no;
    }

}
