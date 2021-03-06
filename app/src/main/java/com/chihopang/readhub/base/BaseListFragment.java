package com.chihopang.readhub.base;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.chihopang.readhub.R;
import com.chihopang.readhub.base.mvp.INetworkView;
import java.util.List;
import me.yokeyword.fragmentation.SupportActivity;
import me.yokeyword.fragmentation.SupportFragment;
import mehdi.sakout.dynamicbox.DynamicBox;

public abstract class BaseListFragment<T> extends SupportFragment implements INetworkView<List> {
  private static final int VIEW_TYPE_LAST_ITEM = 1;

  private SupportActivity mActivity;
  private BaseListPresenter<T> mPresenter = createPresenter();
  public DynamicBox mBox;
  boolean hasMore = true;

  @BindView(R.id.fab) FloatingActionButton mFAB;
  @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
  @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
  @BindView(R.id.frame_list_container) FrameLayout mFrameContainer;
  private BaseAdapter<T> mAdapter = new BaseAdapter<T>() {
    @Override public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      if (viewType == VIEW_TYPE_LAST_ITEM) return new LoadingViewHolder(parent, hasMore());
      return provideViewHolder(parent, viewType);
    }

    @Override public int getItemCount() {
      return super.getItemCount() == 0 ? super.getItemCount()
          : super.getItemCount() + 1;//有数据时，显示 loadingViewHolder
    }

    @Override public int getItemViewType(int position) {
      if (position == getItemCount() - 1) {
        return VIEW_TYPE_LAST_ITEM;
      }
      return super.getItemViewType(position);
    }

  };
  private LinearLayoutManager mManager;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_base_list, container, false);
    ButterKnife.bind(this, view);
    initContent();
    if (mAdapter.getItemCount() == 0) requestData();
    setRetainInstance(true);
    return view;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    mActivity = (SupportActivity) context;
  }

  private void initContent() {
    mBox = new DynamicBox(mActivity, mFrameContainer);
    mBox.setClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        //加载失败时的按钮点击事件
        mAdapter.clear();
        requestData();
      }
    });
    mRecyclerView.setAdapter(mAdapter);
    mManager = new LinearLayoutManager(mActivity);
    mRecyclerView.setLayoutManager(mManager);
    mFAB.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        mManager.smoothScrollToPosition(mRecyclerView, null, 0);
      }
    });
    mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
        mAdapter.clear();
        requestData();
      }
    });
    mSwipeRefreshLayout.setColorSchemeColors(Color.parseColor("#607D8B"), Color.BLACK, Color.BLUE);
    mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        mFAB.setVisibility(
            (mManager.findFirstVisibleItemPosition() > 0) ? View.VISIBLE : View.GONE);
        if (!mSwipeRefreshLayout.isRefreshing()
            && hasMore()
            && mManager.findLastVisibleItemPosition() == mAdapter.getItemCount() - 1) {
          requestMore();
        }
      }
    });
  }

  @SuppressWarnings("unchecked") @Override public void onSuccess(List datas) {
    if (mSwipeRefreshLayout.isRefreshing()) mSwipeRefreshLayout.setRefreshing(false);
    mBox.hideAll();
    if (datas != null && datas.size() != 0) mAdapter.addItems(datas);
  }

  @Override public void onError(Throwable e) {
    mBox.setOtherExceptionMessage(e.getMessage());
    mBox.showExceptionLayout();
  }

  public BaseListPresenter<T> getPresenter() {
    return mPresenter;
  }

  private void requestData() {
    mBox.showLoadingLayout();
    hasMore = true;
    getPresenter().start();
  }

  private void requestMore() {
    mSwipeRefreshLayout.setRefreshing(true);//同时防止多次 requestMore
    getPresenter().startRequestMore();
  }

  public boolean hasMore() {
    return hasMore;
  }

  public void onTabClick() {
    if (mManager.findFirstCompletelyVisibleItemPosition() != 0) {
      mRecyclerView.smoothScrollToPosition(0);
      return;
    }
    if (!mSwipeRefreshLayout.isRefreshing()) {
      mSwipeRefreshLayout.setRefreshing(true);
      mAdapter.clear();
      requestData();
    }
  }

  public abstract BaseViewHolder<T> provideViewHolder(ViewGroup parent, int viewType);

  public abstract BaseListPresenter<T> createPresenter();
}
