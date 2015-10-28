package com.youzan.genesis.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Francis on 15/10/28.
 */
public class DateUtil {

    private static final String DATE_FORMAT_NOTIFICATION_TIME = "HH:mm";

    public static String getCurrentTimeForNotification() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOTIFICATION_TIME);

        return simpleDateFormat.format(new Date());
    }

}
