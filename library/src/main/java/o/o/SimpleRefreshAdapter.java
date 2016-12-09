package o.o;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author https://github.com/timelessx
 *         判断RecyclerView到达底部或顶部目前常见有以下方法：
 *         <ol>
 *         <li>1.通过 {@link LinearLayoutManager#findFirstCompletelyVisibleItemPosition()},
 *         {@link LinearLayoutManager#findLastCompletelyVisibleItemPosition()},
 *         {@link GridLayoutManager}继承自{@link LinearLayoutManager}同样方法,
 *         {@link StaggeredGridLayoutManager#findFirstCompletelyVisibleItemPositions(int[])},
 *         {@link StaggeredGridLayoutManager#findLastCompletelyVisibleItemPositions(int[])} (int[])}
 *         这一系列方法会遍历view比较位置来判断，代价比较高</li>
 *         <li>2.通过 {@link RecyclerView#canScrollVertically(int)} 判断</li>
 *         </ol>
 *         这两种方法都需要监听滚动事件,不停判断查找,最关键的是滑动过程中大部分时间都不是在顶部或底部，大量运算浪费了
 *         本类提供了一种全新的判断是否滑动到顶部的方法,通过{@link #onViewAttachedToWindow(RecyclerView.ViewHolder)}与
 *         {@link #onViewDetachedFromWindow(RecyclerView.ViewHolder)}或{@link RecyclerView.OnChildAttachStateChangeListener}来判断
 *         这里只不过提供了一种全新的思路,至于功能和其他细节大家自己处理，这里只不过是一个简单示例
 *         <p>刷新完成后需要手动调用 {@link #notifyRefreshCompleted()} 来重置状态并隐藏Header</p>
 *         <p>加载更多完成后需要手动调用 {@link #notifyLoadMoreCompleted()} 重置状态</p>
 */

public abstract class SimpleRefreshAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected static final int TYPE_HEADER = 666;
    protected static final int TYPE_FOOTER = 668;

    private RefreshHeader mHeaderView;
    protected View mFooterView;
    private boolean mHeaderEnabled = true;
    private boolean mFooterEnabled = true;
    private OnRefreshListener mOnRefreshListener;
    private boolean isRefreshing;
    private boolean isLoading;
    private boolean isHeaderShowing;

    public void setHeaderEnable(boolean enable) {
        mHeaderEnabled = enable;
    }

    public void setFooterEnable(boolean enable) {
        mFooterEnabled = enable;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
        mHeaderView.setRefreshing(refreshing);
    }

    public void notifyRefreshCompleted() {
        mHeaderView.refreshCompleted();
        isRefreshing = false;
    }

    public void notifyLoadMoreCompleted() {
        isLoading = false;
    }

    public void notifyNetError() {
        isRefreshing = false;
        isLoading = false;
        mHeaderView.reset();
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        //Log.e("onViewAttached()", "holder: " + holder);
        if (holder.itemView instanceof RefreshHeader)   //^_^,到顶了
            isHeaderShowing = true;

        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams && holder instanceof ViewHolder) {
            //if (isHeader(holder.getLayoutPosition()) || isFooter(holder.getLayoutPosition()))
            ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        //Log.e("onViewDetached()", "holder: " + holder);
        if (holder.itemView instanceof RefreshHeader)   //o.o,顶部已经划过了
            isHeaderShowing = false;
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        if (mHeaderEnabled)
            mHeaderView = new RefreshHeader(recyclerView.getContext());

        if (mFooterEnabled)
            mFooterView = LayoutInflater.from(recyclerView.getContext()).inflate(R.layout.footer, recyclerView, false);

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return isHeader(position) || isFooter(position) ? gridLayoutManager.getSpanCount() : 1;
                }
            });
        }

        /*recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                //Log.e("onAttached()", "view: " + view);
                if (view instanceof RefreshHeader)
                    isHeaderShowing = true;
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                //Log.e("onDetached()", "view: " + view);
                if (view instanceof RefreshHeader)
                    isHeaderShowing = false;
            }
        });*/

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            private float mLastY;
            private float dY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isRefreshing && isHeaderShowing) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mLastY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float moveY = event.getRawY();
                            dY += moveY - mLastY;
                            mHeaderView.onMove((int) dY / 3);
                            mLastY = moveY;
                            break;
                        case MotionEvent.ACTION_UP:
                            dY = 0;
                            if (mHeaderView.onRelease()) {
                                isRefreshing = true;
                                mOnRefreshListener.onRefresh();
                            }
                            break;
                    }
                    return mHeaderView.getHeight() > 1;
                }
                return false;
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER)
            return new ViewHolder(mHeaderView);
        if (viewType == TYPE_FOOTER)
            return new ViewHolder(mFooterView);
        return onCreateCustomViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //Log.e("onBindViewHolder()", "position: " + position);
        if (holder.getItemViewType() == TYPE_HEADER)
            return;
        if (holder.getItemViewType() == TYPE_FOOTER) {
            if (!isLoading) {
                isLoading = true;
                mOnRefreshListener.onLoadMore();
            }
            return;
        }
        onBindCustomViewHolder((T) holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position))
            return TYPE_HEADER;
        if (isFooter(position))
            return TYPE_FOOTER;
        return getCustomItemViewType(position);
    }

    protected boolean isHeader(int position) {
        return position == 0 && mHeaderEnabled;
    }

    protected boolean isFooter(int position) {
        return position == getItemCount() - 1 && mFooterEnabled;
    }

    @Override
    public int getItemCount() {
        int count = getCustomItemCount();
        if (count == 0)
            return mHeaderEnabled ? 1 : 0;
        if (mHeaderEnabled)
            count++;
        if (mFooterEnabled)
            count++;
        return count;
    }

    protected abstract int getCustomItemViewType(int position);

    protected abstract int getCustomItemCount();

    protected abstract T onCreateCustomViewHolder(ViewGroup parent, int viewType);

    protected abstract void onBindCustomViewHolder(T holder, int position);

    public interface OnRefreshListener {

        void onRefresh();

        void onLoadMore();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }

}
