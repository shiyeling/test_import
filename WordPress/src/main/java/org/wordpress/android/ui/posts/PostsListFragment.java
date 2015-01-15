package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.caredear.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.widgets.FloatingActionButton;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;

import java.util.List;
import java.util.Vector;

public class PostsListFragment extends ListFragment
        implements WordPress.OnPostUploadedListener {
    public static final int POSTS_REQUEST_COUNT = 20;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnSinglePostLoadedListener mOnSinglePostLoadedListener;
    private PostsListAdapter mPostsListAdapter;
    private FloatingActionButton mFabButton;
    private ApiHelper.FetchPostsByCategoriesTask mCurrentFetchPostsTask;
    private ApiHelper.FetchSinglePostTask mCurrentFetchSinglePostTask;
    private View mProgressFooterView;
    private boolean mCanLoadMorePosts = true;
    private boolean mIsPage, mShouldSelectFirstPost, mIsFetchingPosts;
    private int mCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isAdded()) {
            Bundle extras = getActivity().getIntent().getExtras();
            if (extras != null) {
                mIsPage = extras.getBoolean(PostsActivity.EXTRA_VIEW_PAGES);
                mCategory = extras.getInt(PostsActivity.EXTRA_CATEGORY_ID);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean isRefreshing = mSwipeToRefreshHelper.isRefreshing();
        super.onConfigurationChanged(newConfig);
        // Swipe to refresh layout is destroyed onDetachedFromWindow,
        // so we have to re-init the layout, via the helper here
        initSwipeToRefreshHelper();
        mSwipeToRefreshHelper.setRefreshing(isRefreshing);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.post_listview, container, false);
    }

    private void initSwipeToRefreshHelper() {
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(
                getActivity(),
                (SwipeRefreshLayout) getView().findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        refreshPosts((PostsActivity) getActivity());
                    }
                });
    }

    private void refreshPosts(PostsActivity postsActivity) {
        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null) {
            ToastUtils.showToast(getActivity(), mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                    Duration.LONG);
            return;
        }
        boolean hasLocalChanges = WordPress.wpDB.findLocalChanges(currentBlog.getLocalTableBlogId(), mIsPage);
        if (hasLocalChanges) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(postsActivity);
            dialogBuilder.setTitle(getResources().getText(R.string.local_changes));
            dialogBuilder.setMessage(getResources().getText(R.string.overwrite_local_changes));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mSwipeToRefreshHelper.setRefreshing(true);
                            requestPosts(false);
                        }
                    }
            );
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mSwipeToRefreshHelper.setRefreshing(false);
                }
            });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            mSwipeToRefreshHelper.setRefreshing(true);
            requestPosts(false);
        }
    }

    public PostsListAdapter getPostListAdapter() {
        if (mPostsListAdapter == null) {
            PostsListAdapter.OnLoadMoreListener loadMoreListener = new PostsListAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if (mCanLoadMorePosts && !mIsFetchingPosts)
                        requestPosts(true);
                }
            };

            PostsListAdapter.OnPostsLoadedListener postsLoadedListener = new PostsListAdapter.OnPostsLoadedListener() {
                @Override
                public void onPostsLoaded(int postCount) {
                    if (!isAdded()) {
                        return;
                    }
                    // set the empty view now that posts have been loaded - this avoids the problem
                    // of the empty view immediately appearing when set at design time
                    if (getListView().getEmptyView() == null) {
                        getListView().setEmptyView(getView().findViewById(R.id.empty_view));
                    }
                    if (postCount == 0) {
                        TextView textView = (TextView) getView().findViewById(R.id.title_empty);
                        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                            if (textView != null) {
                                textView.setText(R.string.no_network_message);
                            }
                            return;
                        }
                        if (mCanLoadMorePosts) {
                            // No posts, let's request some if network available
                            if (isAdded() && NetworkUtils.isNetworkAvailable(getActivity())) {
                                setRefreshing(true);
                                requestPosts(false);
                            }
                        } else {
                            // when we got 0 posts loaded and can not load more, display empty msg
                            //TextView textView = (TextView) getView().findViewById(R.id.title_empty);
                            if (textView != null) {
                                textView.setText(getText(R.string.posts_empty_list));
                            }
                        }
                    } else if (mShouldSelectFirstPost) {
                        // Select the first row on a tablet, if requested
                        mShouldSelectFirstPost = false;
                        if (mPostsListAdapter.getCount() > 0) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(0);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                                getListView().setItemChecked(0, true);
                            }
                        }
                    } else if (isAdded() && ((PostsActivity) getActivity()).isDualPane()) {
                        // Reload the last selected position, if available
                        int selectedPosition = getListView().getCheckedItemPosition();
                        if (selectedPosition != ListView.INVALID_POSITION && selectedPosition < mPostsListAdapter.getCount()) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(selectedPosition);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                            }
                        }
                    }
                }
            };
            mPostsListAdapter = new PostsListAdapter(getActivity(), mIsPage, mCategory,
                    loadMoreListener, postsLoadedListener);
        }

        return mPostsListAdapter;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mProgressFooterView = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        getListView().addFooterView(mProgressFooterView, null, false);
        mProgressFooterView.setVisibility(View.GONE);
        getListView().setDivider(getResources().getDrawable(R.drawable.list_divider));
        getListView().setDividerHeight(1);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                if (position >= getPostListAdapter().getCount()) //out of bounds
                    return;
                if (v == null) //view is gone
                    return;
                PostsListPost postsListPost = (PostsListPost) getPostListAdapter().getItem(position);
                if (postsListPost == null)
                    return;
                postsListPost.setRead(true);
                if (!mIsFetchingPosts || isLoadingMorePosts()) {
                    showPost(postsListPost.getPostId());
                } else if (isAdded()) {
                    Toast.makeText(getActivity(),
                            mIsPage ? R.string.loading_pages : R.string.loading_posts,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView textView = (TextView) getView().findViewById(R.id.title_empty);
        if (textView != null) {
            if (mIsPage) {
                textView.setText(getText(R.string.pages_empty_list));
            } else {
                textView.setText(getText(R.string.loading_posts));
            }
        }
        initSwipeToRefreshHelper();
        WordPress.setOnPostUploadedListener(this);

        mFabButton = (FloatingActionButton) getView().findViewById(R.id.fab_button);
        mFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPost();
            }
        });

        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            ((PostsActivity) getActivity()).requestPosts();
        }
    }

    private void newPost() {
        if (getActivity() instanceof PostsActivity) {
            ((PostsActivity)getActivity()).newPost();
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnSinglePostLoadedListener = (OnSinglePostLoadedListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void onResume() {
        super.onResume();
        if (WordPress.getCurrentBlog() != null) {
            if (getListView().getAdapter() == null) {
                getListView().setAdapter(getPostListAdapter());
            }

            getPostListAdapter().loadPosts();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        /*if (mFabButton != null) {
            mFabButton.setVisibility(hidden ? View.GONE : View.VISIBLE);
        }*/
    }

    public boolean isRefreshing() {
        return mSwipeToRefreshHelper.isRefreshing();
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    private void showPost(long selectedId) {
        if (WordPress.getCurrentBlog() == null)
            return;

        Post post = WordPress.wpDB.getPostForLocalTablePostId(selectedId);
        if (post != null) {
            WordPress.currentPost = post;
            mOnPostSelectedListener.onPostSelected(post);
        } else {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment.newAlertDialog(getString(R.string.post_not_found));
                ft.add(alert, "alert");
                ft.commitAllowingStateLoss();
            }
        }
    }

    boolean isLoadingMorePosts() {
        return mIsFetchingPosts && (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE);
    }

    public void requestPosts(boolean loadMore) {
        if (!isAdded() || WordPress.getCurrentBlog() == null || mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            return;
        }

        ((TextView) getView().findViewById(R.id.title_empty)).setText(getText(R.string.loading_posts));

        /*
         * in the old way, they need to know how much is loaded to calculate total load count.
         * now we only need to skip the loaded count and just load POSTS_REQUEST_COUNT more*
         */
        int loadedPostCount = getPostListAdapter().getRemotePostCount();
        if (!loadMore) {
            mCanLoadMorePosts = true;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(mCategory);
        apiArgs.add(loadMore ? loadedPostCount : 0);
        apiArgs.add(POSTS_REQUEST_COUNT);
        if (mProgressFooterView != null && loadMore) {
            mProgressFooterView.setVisibility(View.VISIBLE);
        }

        mCurrentFetchPostsTask = new ApiHelper.FetchPostsByCategoriesTask(new ApiHelper.FetchPostsByCategoriesTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded())
                    return;
                mSwipeToRefreshHelper.setRefreshing(false);
                if (mProgressFooterView != null) {
                    mProgressFooterView.setVisibility(View.GONE);
                }

                if (postCount == 0) {
                    mCanLoadMorePosts = false;
                } else if (postCount == getPostListAdapter().getRemotePostCount() && postCount != POSTS_REQUEST_COUNT) {
                    mCanLoadMorePosts = false;
                }

                getPostListAdapter().loadPosts();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded()) {
                    return;
                }
                mSwipeToRefreshHelper.setRefreshing(false);
                if (mProgressFooterView != null) {
                    mProgressFooterView.setVisibility(View.GONE);
                }
                if (errorType != ErrorType.TASK_CANCELLED && errorType != ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            ToastUtils.showToast(getActivity(),
                                    mIsPage ? R.string.error_refresh_unauthorized_pages : R.string.error_refresh_unauthorized_posts,
                                    Duration.LONG);
                            return;
                        default:
                            ToastUtils.showToast(getActivity(),
                                    mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                                    Duration.LONG);
                            return;
                    }
                }
            }
        });

        mIsFetchingPosts = true;
        mCurrentFetchPostsTask.execute(apiArgs);
    }

    protected void clear() {
        if (getPostListAdapter() != null) {
            getPostListAdapter().clear();
        }
        mCanLoadMorePosts = true;
        if (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE) {
            mProgressFooterView.setVisibility(View.GONE);
        }
    }

    public void setShouldSelectFirstPost(boolean shouldSelect) {
        mShouldSelectFirstPost = shouldSelect;
    }

    @Override
    public void OnPostUploaded(int localBlogId, String postId, boolean isPage) {
        if (!isAdded()) {
            return;
        }

        // If the user switched to a different blog while uploading his post, don't reload posts and refresh the view
        boolean sameBlogId = true;
        if (WordPress.getCurrentBlog() == null || WordPress.getCurrentBlog().getLocalTableBlogId() != localBlogId) {
            sameBlogId = false;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            return;
        }

        // Fetch the newly uploaded post
        if (!TextUtils.isEmpty(postId)) {
            final boolean reloadPosts = sameBlogId;
            List<Object> apiArgs = new Vector<Object>();
            apiArgs.add(WordPress.wpDB.instantiateBlogByLocalId(localBlogId));
            apiArgs.add(postId);
            apiArgs.add(isPage);

            mCurrentFetchSinglePostTask = new ApiHelper.FetchSinglePostTask(
                    new ApiHelper.FetchSinglePostTask.Callback() {
                @Override
                public void onSuccess() {
                    mCurrentFetchSinglePostTask = null;
                    mIsFetchingPosts = false;
                    if (!isAdded() || !reloadPosts) {
                        return;
                    }
                    mSwipeToRefreshHelper.setRefreshing(false);
                    getPostListAdapter().loadPosts();
                    mOnSinglePostLoadedListener.onSinglePostLoaded();
                }

                @Override
                public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                    mCurrentFetchSinglePostTask = null;
                    mIsFetchingPosts = false;
                    if (!isAdded() || !reloadPosts) {
                        return;
                    }
                    if (errorType != ErrorType.TASK_CANCELLED) {
                        ToastUtils.showToast(getActivity(),
                                mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts, Duration.LONG);
                    }
                    mSwipeToRefreshHelper.setRefreshing(false);
                }
            });

            mSwipeToRefreshHelper.setRefreshing(true);
            mIsFetchingPosts = true;
            mCurrentFetchSinglePostTask.execute(apiArgs);
        }
    }

    public void onBlogChanged() {
        if (mCurrentFetchPostsTask != null) {
            mCurrentFetchPostsTask.cancel(true);
        }
        if (mCurrentFetchSinglePostTask != null) {
            mCurrentFetchSinglePostTask.cancel(true);
        }
        mIsFetchingPosts = false;
        mSwipeToRefreshHelper.setRefreshing(false);
    }

    public interface OnPostSelectedListener {
        public void onPostSelected(Post post);
    }

    public interface OnPostActionListener {
        public void onPostAction(int action, Post post);
    }

    public interface OnSinglePostLoadedListener {
        public void onSinglePostLoaded();
    }
}
