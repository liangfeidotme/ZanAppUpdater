package com.youzan.genesis.utils;

import android.content.Context;
import android.widget.Toast;

import com.youzan.genesis.R;

/**
 * Created by Francis on 15/10/28.
 */
public class ToastUtil {

    private static Toast toast;

    public static void show(Context context, int res) {
        if(null == context){
            return;
        }

        toast = Toast.makeText(context, res, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void show(Context context, String res) {
        if(null == context){
            return;
        }

        toast = Toast.makeText(context, res, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showLong(Context context, int res) {
        if(null == context){
            return;
        }

        toast = Toast.makeText(context, res, Toast.LENGTH_LONG);
        toast.show();
    }

    public static void showLong(Context context, String res) {
        if(null == context){
            return;
        }

        toast = Toast.makeText(context, res, Toast.LENGTH_LONG);
        toast.show();
    }

    public static void getDataFail(Context context){
        if(null == context){
            return;
        }

        toast = Toast.makeText(context, R.string.get_data_fail, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void requestDataFail(Context context){
        if(null == context){
            return;
        }

        toast = Toast.makeText(context, R.string.request_data_fail, Toast.LENGTH_SHORT);
        toast.show();
    }

}
