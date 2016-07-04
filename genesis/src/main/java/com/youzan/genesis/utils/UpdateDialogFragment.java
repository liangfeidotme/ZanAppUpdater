package com.youzan.genesis.utils;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.youzan.genesis.R;
import com.youzan.mobile.zanpermissions.AfterPermissionGranted;
import com.youzan.mobile.zanpermissions.PermissionCallbacks;
import com.youzan.mobile.zanpermissions.ZanPermissions;

import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class UpdateDialogFragment extends DialogFragment
        implements PermissionCallbacks, View.OnClickListener {

    private static final String ARGS_CANCELABLE = "ARGS_CANCELABLE";
    private static final String ARGS_TITLE = "ARGS_TITLE";
    private static final String ARGS_CONTENT = "ARGS_CONTENT";

    private static final int RC_WRITE_EXTERNAL_STORAGE = 0x33;

    private boolean mCancelable = true;
    private String mTitle = "";
    private String mContent = "";
    private IPositiveClickListener mPositiveClickListener;
    private TextView mPositiveTextView;

    public static UpdateDialogFragment newInstance(String title,
                                                   String content,
                                                   boolean cancelable) {
        Bundle bundle = new Bundle();
        bundle.putString(ARGS_TITLE, title);
        bundle.putString(ARGS_CONTENT, content);
        bundle.putBoolean(ARGS_CANCELABLE, cancelable);
        UpdateDialogFragment fragment = new UpdateDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mCancelable = bundle.getBoolean(ARGS_CANCELABLE);
            mTitle = bundle.getString(ARGS_TITLE);
            mContent = bundle.getString(ARGS_CONTENT);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = View.inflate(getActivity(), R.layout.fragment_update_dialog, null);

        mPositiveTextView = (TextView) v.findViewById(R.id.update_choose_positive);
        mPositiveTextView.setOnClickListener(this);

        TextView titleTextView = (TextView) v.findViewById(R.id.update_title);
        titleTextView.setText(mTitle);

        TextView contentTextView = (TextView) v.findViewById(R.id.update_content);
        contentTextView.setText(mContent);

        TextView cancelTextView = (TextView) v.findViewById(R.id.update_choose_negative);

        Dialog dialog = DialogUtil.buildDialog(getActivity(), v);
        if (mCancelable) {
            cancelTextView.setVisibility(View.VISIBLE);
            cancelTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        } else {
            cancelTextView.setVisibility(View.GONE);
            dialog.setCancelable(false);
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }
        return dialog;
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        ZanPermissions.checkDeniedPermissionsNeverAskAgain(this,
                getString(R.string.permission_write_warning),
                R.string.permission_setting, R.string.cancel, perms, null);
    }


    @Override
    public void onClick(View v) {
        if (v == mPositiveTextView) {
            callWrite();
        }
    }

    @AfterPermissionGranted(RC_WRITE_EXTERNAL_STORAGE)
    private void callWrite() {
        String[] perms = {WRITE_EXTERNAL_STORAGE};
        if (ZanPermissions.hasPermissions(getActivity(), perms)) {
            if (mPositiveClickListener != null) {
                mPositiveClickListener.positiveClick();
                dismiss();
            }
        } else {
            ZanPermissions.requestPermissions(this, RC_WRITE_EXTERNAL_STORAGE, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (RC_WRITE_EXTERNAL_STORAGE == requestCode) {
            ZanPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }

    public void setPositiveClickListener(IPositiveClickListener positiveClickListener) {
        mPositiveClickListener = positiveClickListener;
    }

    public interface IPositiveClickListener {
        void positiveClick();
    }

}
