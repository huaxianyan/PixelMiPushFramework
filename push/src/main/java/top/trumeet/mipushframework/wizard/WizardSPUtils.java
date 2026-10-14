package top.trumeet.mipushframework.wizard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;

import com.xiaomi.xmsf.push.control.PushControllerUtils;

import top.trumeet.common.Constants;
import top.trumeet.mipushframework.main.MainPage;

/**
 * Created by Trumeet on 2017/8/24.
 * A util store Wizard info to SP
 * @author Trumeet
 */

final class WizardSPUtils {
    private static SharedPreferences getSp (Context context) {
        return context.getApplicationContext().getSharedPreferences(Constants.WIZARD_SP_NAME,
                Context.MODE_PRIVATE);
    }

    static boolean shouldShowWizard (Context context) {
        return !RequestPermissionPageKt.hasNotificationPermission(context)
                || getSp(context).getBoolean(Constants.KEY_SHOW_WIZARD, true);
    }

    static void setShouldShowWizard (boolean value, Context context) {
        getSp (context)
                .edit()
                .putBoolean(Constants.KEY_SHOW_WIZARD, value)
                .apply();
    }

    static void finishWizard (Activity context) {
        setShouldShowWizard(false, context);
        PushControllerUtils.setServiceEnable(true, context.getApplicationContext());
        ActivityCompat.finishAffinity(context);
        context.startActivity(new Intent(context,
                MainPage.class));
    }
}
