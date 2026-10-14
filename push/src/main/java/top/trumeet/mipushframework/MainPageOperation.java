package top.trumeet.mipushframework;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class MainPageOperation {
    private final Context context;

    public MainPageOperation(Context context) {
        this.context = context;
    }

    public void gotoGitHubReleasePage() {
        context.startActivity(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://github.com/huaxianyan/PixelMiPushFramework/releases")));
    }
}
