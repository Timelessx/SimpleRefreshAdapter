package x.x;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import o.o.SimpleRefreshAdapter;

public class MainActivity extends AppCompatActivity {

    private MyAdapter adapter;
    private Handler handler;
    private RecyclerView.LayoutManager[] layoutManager = new RecyclerView.LayoutManager[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        handler = new Handler();

        layoutManager[0] = new LinearLayoutManager(this);
        layoutManager[1] = new GridLayoutManager(this, 2);
        layoutManager[2] = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        adapter = new MyAdapter<String>(getData());
        adapter.setOnRefreshListener(new SimpleRefreshAdapter.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }

            @Override
            public void onLoadMore() {
                loadMore();
            }
        });

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager[2]);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            private int index;

            @Override
            public void onClick(View view) {
                recyclerView.setLayoutManager(layoutManager[index++ % 3]);
                recyclerView.setAdapter(adapter);
                Snackbar.make(view, recyclerView.getLayoutManager().getClass().getSimpleName(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void refresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.setData(getData());
                adapter.notifyRefreshCompleted();
            }
        }, 2000);
    }

    private void loadMore() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.addData(getData());
                adapter.notifyLoadMoreCompleted();
            }
        }, 2000);
    }

    private ArrayList<String> getData() {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            list.add("0.0");
        return list;
    }

    private static class MyAdapter<T> extends SimpleRefreshAdapter<MyAdapter.ViewHolder> {

        public static final int TYPE_ITEM = 0;
        private List<T> mList;

        public MyAdapter(List<T> list) {
            mList = list;
        }

        public void setData(List<T> list) {
            mList = list;
            notifyDataSetChanged();
        }

        public void addData(List<T> list) {
            int size = mList.size();
            mList.addAll(list);
            notifyItemRangeInserted(size + 1, list.size());
        }

        @Override
        protected ViewHolder onCreateCustomViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false));
        }

        @Override
        protected void onBindCustomViewHolder(ViewHolder holder, int position) {
            holder.tv_position.setText("" + position);
            ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                if (position % 2 == 0)
                    layoutParams.height = 900;
                else
                    layoutParams.height = 600;
                holder.itemView.setLayoutParams(layoutParams);
            }
        }

        @Override
        protected int getCustomItemViewType(int position) {
            return TYPE_ITEM;
        }

        @Override
        protected int getCustomItemCount() {
            return mList == null ? 0 : mList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv_position;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_position = (TextView) itemView.findViewById(R.id.tv_position);
            }
        }
    }

}
