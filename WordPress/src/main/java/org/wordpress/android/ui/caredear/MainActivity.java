package org.wordpress.android.ui.caredear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import org.wordpress.caredear.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.caredear.CaredearUtils.CATEGORY_ID_ON_SERVER;
import org.wordpress.android.util.AppLog;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    private View mTitlePanel;
    private TextView mTitleTv;
    private GridView mCategoryGrid;
    private CategoryAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cd_activity_main);
        initResources();
        if (!WordPress.isSignedIn(this)) {
            Log.i(AppLog.TAG, "No accounts, start CdSignInActivity");
            Intent intent = new Intent(this, CdSignInActivity.class);
            startActivity(intent);
        }
    }

    private void initResources() {
        mAdapter = new CategoryAdapter(this);
        mTitlePanel = findViewById(R.id.cd_layout_title);
        mTitleTv = (TextView) findViewById(R.id.cd_title_text_center);
        mCategoryGrid = (GridView) findViewById(R.id.category_grid);

        mTitleTv.setText(R.string.cd_app_name);
        mCategoryGrid.setAdapter(mAdapter);
        mCategoryGrid.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, PostsActivity.class);
        int categoryId = (int) id;
        intent.putExtra(PostsActivity.EXTRA_CATEGORY_ID, categoryId);
        startActivity(intent);
    }

}
