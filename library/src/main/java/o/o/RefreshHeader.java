package o.o;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RefreshHeader extends FrameLayout {
    public static final String NORMAL = "下拉刷新";
    public static final String RELEASE_TO_REFRESH = "释放立即刷新";
    public static final String REFRESHING = "正在刷新…";
    public static final String REFRESH_COMPLETED = "刷新成功";
    public static final String LAST_REFRESH_TIME = "上次更新时间: ";
    public static final int STATE_NORMAL = 0;
    public static final int STATE_RELEASE_TO_REFRESH = 1;
    public static final int STATE_REFRESHING = 2;
    public static final int STATE_COMPLETED = 3;

    private int height;
    private ImageView iv_arrow;
    private ProgressBar mProgressBar;
    private TextView tv_status;
    private TextView tv_time;
    private ObjectAnimator rotation_up, rotation_down;
    public int mState = STATE_NORMAL;

    public RefreshHeader(Context context) {
        this(context, null);
    }

    public RefreshHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getContext().getResources().getDisplayMetrics());
        LayoutInflater.from(getContext()).inflate(R.layout.header, this);
        LayoutParams layoutParams = new LayoutParams(-1, 1);
        setLayoutParams(layoutParams);
        iv_arrow = (ImageView) findViewById(R.id.iv_arrow);
        tv_status = (TextView) findViewById(R.id.tv_status);
        tv_time = (TextView) findViewById(R.id.tv_time);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        rotation_up = ObjectAnimator.ofFloat(iv_arrow, "Rotation", 0, 180).setDuration(150);
        rotation_down = ObjectAnimator.ofFloat(iv_arrow, "Rotation", 180, 0).setDuration(150);
        tv_time.setText(LAST_REFRESH_TIME + "Long Long ago");
    }

    public void setState(int state) {
        if (state == mState)
            return;
        switch (state) {
            case STATE_NORMAL:
                iv_arrow.setVisibility(VISIBLE);
                mProgressBar.setVisibility(INVISIBLE);
                tv_time.setVisibility(VISIBLE);
                //iv_arrow.setRotation(0);
                if (rotation_down.isRunning())
                    rotation_down.cancel();
                rotation_down.start();
                tv_status.setText(NORMAL);
                break;
            case STATE_RELEASE_TO_REFRESH:
                //iv_arrow.setRotation(180);
                if (rotation_up.isRunning())
                    rotation_up.cancel();
                rotation_up.start();
                tv_status.setText(RELEASE_TO_REFRESH);
                break;
            case STATE_REFRESHING:
                iv_arrow.setVisibility(INVISIBLE);
                mProgressBar.setVisibility(VISIBLE);
                tv_status.setText(REFRESHING);
                break;
            case STATE_COMPLETED:
                iv_arrow.setVisibility(GONE);
                mProgressBar.setVisibility(GONE);
                tv_time.setVisibility(GONE);
                tv_status.setText(REFRESH_COMPLETED);
                break;
            default:
                break;
        }
        mState = state;
    }

    public void setRefreshing(boolean refreshing) {
        if (refreshing) {
            ObjectAnimator.ofInt(this, "VisibleHeight", 0, height).setDuration(500).start();
            setState(STATE_REFRESHING);
        } else
            reset();
    }

    public void refreshCompleted() {
        tv_time.setText(LAST_REFRESH_TIME + new SimpleDateFormat("H:mm").format(new Date()));
        setState(STATE_COMPLETED);
        reset();
    }

    public int getVisibleHeight() {
        return getLayoutParams().height;
    }

    public void setVisibleHeight(int height) {
        getLayoutParams().height = height < 0 ? 1 : height;
        requestLayout();
    }

    public void onMove(int distance) {
        //Log.e("onMove", "distance: " + distance);
        //setVisibleHeight(distance >> 1);
        setVisibleHeight(distance);
        setState(getVisibleHeight() > height ? STATE_RELEASE_TO_REFRESH : STATE_NORMAL);
    }

    public boolean onRelease() {
        boolean flag = getVisibleHeight() > height;
        if (flag) {
            setState(STATE_REFRESHING);
            smoothScrollTo(height);
        } else
            smoothScrollTo(1);
        return flag;
    }

    public void reset() {
        smoothScrollTo(1);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                setState(STATE_NORMAL);
            }
        }, 600);
    }

    private void smoothScrollTo(int height) {
        ObjectAnimator.ofInt(this, "VisibleHeight", getVisibleHeight(), height).setDuration(300).start();
    }

}