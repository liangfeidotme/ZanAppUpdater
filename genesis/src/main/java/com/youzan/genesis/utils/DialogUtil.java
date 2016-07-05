package com.youzan.genesis.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.youzan.genesis.R;

/**
 * Created by Francis on 15/10/28.
 */
public class DialogUtil {

    private static final int POSITIVE_COLOR = R.color.metarial_dialog_positive;
    private static final int NEGATIVE_COLOR = R.color.metarial_dialog_negative;

    private static AlertDialog alertDialog;

    public static void showDialog(Context context, int message, int positiveMessage,
                                  final OnClickListener positiveClickListener, boolean
                                          cancelable) {
        dismissDialog();
        if (null != alertDialog && alertDialog.isShowing()
                || (context instanceof Activity) && ((Activity) context).isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(cancelable)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(positiveMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (null != positiveClickListener) {
                            positiveClickListener.onClick();
                        }
                    }
                })
                .setMessage(message);
        alertDialog = builder.create();
        setDialogColor(context);
        alertDialog.show();
    }

    public static void dismissDialog() {
        if (null != alertDialog && alertDialog.isShowing() && null != alertDialog.getWindow()) {
            try {
                alertDialog.dismiss();
            } catch (IllegalArgumentException e) {
                Log.d("FFFF", e.toString());
            }
        }
    }

    public static Dialog buildDialog(Context context, View view, String title,
                                     String positiveMessage, String negativeMessage,
                                     final OnClickListener positiveClickListener,
                                     final OnClickListener negativeClickListener,
                                     boolean cancelable) {
        alertDialog = new android.support.v7.app.AlertDialog.Builder(context)
                .setView(view)
                .setTitle(title)
                .setPositiveButton(positiveMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (null != positiveClickListener) {
                            positiveClickListener.onClick();
                        }
                    }
                })
                .setNegativeButton(negativeMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (null != negativeClickListener) {
                            negativeClickListener.onClick();
                        }
                    }
                })
                .create();
        alertDialog.setCancelable(cancelable);
        alertDialog.setCanceledOnTouchOutside(cancelable);

        return alertDialog;
    }

    public static Dialog buildDialog(Context context, View view) {
        return buildDialog(context, view, null, null, null, null, null, false);
    }

    /**
     * 修改按钮颜色
     *
     * @param context
     */
    private static void setDialogColor(final Context context) {
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(context.getResources().getColor(NEGATIVE_COLOR));
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(context.getResources().getColor(POSITIVE_COLOR));
            }
        });

    }

    public interface OnClickListener {
        void onClick();
    }
}
