package org.wordpress.android.ui.caredear;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import org.wordpress.caredear.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListAbstract;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListWPOrg;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.ApiHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CdSignInActivity extends Activity {

    private String mUsername = "caredear-m2c";
    private String mPassword = "caredear-m2c";
    private String mHttpUsername = "";
    private String mHttpPassword = "";
    private String mURL = "http://wp.caredear.com/wordpress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signIn();
    }


    private void signIn() {
        signInAndFetchBlogListWPOrg();
    }

    private void signInAndFetchBlogListWPOrg() {
        FetchBlogListWPOrg fetchBlogListWPOrg = new FetchBlogListWPOrg(mUsername, mPassword, mURL);
        if (mHttpUsername != null && mHttpPassword != null) {
            fetchBlogListWPOrg.setHttpCredentials(mHttpUsername, mHttpPassword);
        }
        fetchBlogListWPOrg.execute(mFetchBlogListCallback);
    }

    private FetchBlogListAbstract.Callback mFetchBlogListCallback = new FetchBlogListAbstract.Callback() {
        @Override
        public void onSuccess(final List<Map<String, Object>> userBlogList) {
            if (userBlogList != null) {
                BlogUtils.addBlogs(userBlogList, mUsername, mPassword, mHttpUsername, mHttpPassword);
                // refresh first blog
                refreshFirstBlogContent();
            }

            trackAnalyticsSignIn();

            setResult(Activity.RESULT_OK);
            finish();
        }

        @Override
        public void onError(final int messageId, final boolean httpAuthRequired,
                            final boolean erroneousSslCertificate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /*if (erroneousSslCertificate) {
                        askForSslTrust();
                        return;
                    }
                    if (httpAuthRequired) {
                        askForHttpAuthCredentials();
                        return;
                    }
                    if (messageId != 0) {
                        signInError(messageId);
                        return;
                    }
                    endProgress();*/
                    ToastUtils.showToast(CdSignInActivity.this, R.string.toast_login_fail);
                    finish();
                }
            });
        }
    };

    private void refreshBlogContent(Map<String, Object> blogMap) {
        String blogId = blogMap.get("blogId").toString();
        String xmlRpcUrl = blogMap.get("url").toString();
        int intBlogId = StringUtils.stringToInt(blogId, -1);
        if (intBlogId == -1) {
            AppLog.e(AppLog.T.NUX, "Can't refresh blog content - invalid blogId: " + blogId);
            return;
        }
        int blogLocalId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(intBlogId,
                xmlRpcUrl);
        Blog firstBlog = WordPress.wpDB.instantiateBlogByLocalId(blogLocalId);
        new ApiHelper.RefreshBlogContentTask(firstBlog, null).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, false);
    }

    /**
     * Get first blog and call RefreshBlogContentTask. First blog will be autoselected when user login.
     * Also when a user add a new self hosted blog, userBlogList contains only one element.
     * We don't want to refresh the whole list because it can be huge and each blog is refreshed when
     * user selects it.
     */
    private void refreshFirstBlogContent() {
        List<Map<String, Object>> visibleBlogs = WordPress.wpDB.getAccountsBy("isHidden = 0", null,
                1);
        if (visibleBlogs != null && !visibleBlogs.isEmpty()) {
            Map<String, Object> firstBlog = visibleBlogs.get(0);
            refreshBlogContent(firstBlog);
        }
    }

    private void trackAnalyticsSignIn() {
        Map<String, Boolean> properties = new HashMap<String, Boolean>();
        properties.put("dotcom_user", false);
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_IN, properties);
        AnalyticsTracker.refreshMetadata();
        AnalyticsTracker.track(AnalyticsTracker.Stat.ADDED_SELF_HOSTED_SITE);
    }

}
