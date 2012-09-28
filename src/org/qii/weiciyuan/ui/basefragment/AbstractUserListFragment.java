package org.qii.weiciyuan.ui.basefragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.UserBean;
import org.qii.weiciyuan.bean.UserListBean;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.lib.pulltorefresh.PullToRefreshBase;
import org.qii.weiciyuan.support.lib.pulltorefresh.PullToRefreshListView;
import org.qii.weiciyuan.ui.Abstract.AbstractAppActivity;
import org.qii.weiciyuan.ui.Abstract.ICommander;
import org.qii.weiciyuan.ui.Abstract.IToken;
import org.qii.weiciyuan.ui.Abstract.IUserInfo;
import org.qii.weiciyuan.ui.actionmenu.FriendSingleChoiceModeListener;
import org.qii.weiciyuan.ui.adapter.UserListAdapter;
import org.qii.weiciyuan.ui.main.AvatarBitmapWorkerTask;
import org.qii.weiciyuan.ui.userinfo.UserInfoActivity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: qii
 * Date: 12-8-18
 */
public abstract class AbstractUserListFragment extends Fragment {

//    protected View headerView;
    protected View footerView;
    public volatile boolean isBusying = false;
    protected ICommander commander;
    protected PullToRefreshListView pullToRefreshListView;
    protected TextView empty;
    protected ProgressBar progressBar;
    protected UserListAdapter timeLineAdapter;
    protected UserBean currentUser;
    protected UserListBean bean = new UserListBean();
    protected String uid;

    private UserListGetNewDataTask newTask;
    private UserListGetOlderDataTask oldTask;

    protected ListView getListView() {
        return pullToRefreshListView.getRefreshableView();
    }

    protected void clearAndReplaceValue(UserListBean value) {


        bean.setNext_cursor(value.getNext_cursor());
        bean.getUsers().clear();
        bean.getUsers().addAll(value.getUsers());
        bean.setTotal_number(value.getTotal_number());
        bean.setPrevious_cursor(value.getPrevious_cursor());

    }

    protected ActionMode mActionMode;

    public void setmActionMode(ActionMode mActionMode) {
        this.mActionMode = mActionMode;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (newTask != null)
            newTask.cancel(true);
        if (oldTask != null)
            oldTask.cancel(true);
    }

    public AbstractUserListFragment(String uid) {
        this.uid = uid;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        commander = ((AbstractAppActivity) getActivity()).getCommander();
        if (savedInstanceState != null && bean.getUsers().size() == 0) {
            clearAndReplaceValue((UserListBean) savedInstanceState.getSerializable("bean"));
            timeLineAdapter.notifyDataSetChanged();
            refreshLayout(bean);
        } else {
            pullToRefreshListView.startRefreshNow();
            refresh();

        }

        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if (position - 1 < getList().getUsers().size() && position - 1 >= 0) {
                    if (mActionMode != null) {
                        mActionMode.finish();
                        mActionMode = null;
                        getListView().setItemChecked(position, true);
                        timeLineAdapter.notifyDataSetChanged();
                        mActionMode = getActivity().startActionMode(new FriendSingleChoiceModeListener(getListView(), timeLineAdapter, AbstractUserListFragment.this, bean.getUsers().get(position - 1)));
                        return true;
                    } else {
                        getListView().setItemChecked(position, true);
                        timeLineAdapter.notifyDataSetChanged();
                        mActionMode = getActivity().startActionMode(new FriendSingleChoiceModeListener(getListView(), timeLineAdapter, AbstractUserListFragment.this, bean.getUsers().get(position - 1)));
                        return true;
                    }
                }
                return false;
            }
        }

        );

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = ((IUserInfo) getActivity()).getUser();

        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    public UserListBean getList() {
        return bean;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("bean", bean);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listview_layout, container, false);
        empty = (TextView) view.findViewById(R.id.empty);
        progressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        pullToRefreshListView = (PullToRefreshListView) view.findViewById(R.id.listView);
        pullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {


            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {


                refresh();
            }
        });
//        listView.setScrollingCacheEnabled(false);
//        headerView = inflater.inflate(R.layout.fragment_listview_header_layout, null);
//        listView.addHeaderView(headerView);
//        listView.setHeaderDividersEnabled(false);
        footerView = inflater.inflate(R.layout.fragment_listview_footer_layout, null);
        getListView().addFooterView(footerView);

        if (bean.getUsers().size() == 0) {
            footerView.findViewById(R.id.listview_footer).setVisibility(View.GONE);
        }


        timeLineAdapter = new UserListAdapter(AbstractUserListFragment.this, ((AbstractAppActivity) getActivity()).getCommander(), bean.getUsers(), getListView());
        pullToRefreshListView.setAdapter(timeLineAdapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (mActionMode != null) {
                    getListView().clearChoices();
                    mActionMode.finish();
                    mActionMode = null;
                    return;
                }
                getListView().clearChoices();
                if (position - 1 < getList().getUsers().size()) {

                    listViewItemClick(parent, view, position - 1, id);
                } else {

                    listViewFooterViewClick(view);
                }

            }
        });
        return view;
    }

    protected void listViewFooterViewClick(View view) {
        if (!isBusying) {
            oldTask = new UserListGetOlderDataTask();
            oldTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    protected void listViewItemClick(AdapterView parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("token", ((IToken) getActivity()).getToken());
        intent.putExtra("user", bean.getUsers().get(position));
        startActivity(intent);
    }

    protected void refreshLayout(UserListBean bean) {
        if (bean.getUsers().size() > 0) {
            footerView.findViewById(R.id.listview_footer).setVisibility(View.VISIBLE);
            empty.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
//            listView.setVisibility(View.VISIBLE);
        } else {
            footerView.findViewById(R.id.listview_footer).setVisibility(View.INVISIBLE);
            empty.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
//            listView.setVisibility(View.INVISIBLE);
        }
    }


    public void refresh() {
        Map<String, AvatarBitmapWorkerTask> avatarBitmapWorkerTaskHashMap = ((AbstractAppActivity) getActivity()).getAvatarBitmapWorkerTaskHashMap();


        newTask = new UserListGetNewDataTask();
        newTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        Set<String> keys = avatarBitmapWorkerTaskHashMap.keySet();
        for (String key : keys) {
            avatarBitmapWorkerTaskHashMap.get(key).cancel(true);
            avatarBitmapWorkerTaskHashMap.remove(key);
        }


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.userlistfragment_menu, menu);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.refresh:
                pullToRefreshListView.startRefreshNow();

                refresh();

                break;
        }
        return super.onOptionsItemSelected(item);
    }


    class UserListGetNewDataTask extends MyAsyncTask<Void, UserListBean, UserListBean> {
        WeiboException e;

        @Override
        protected void onPreExecute() {
            showListView();
            isBusying = true;
            footerView.findViewById(R.id.listview_footer).setVisibility(View.GONE);
//            headerView.findViewById(R.id.header_progress).setVisibility(View.VISIBLE);
//            headerView.findViewById(R.id.header_text).setVisibility(View.VISIBLE);
//            headerView.findViewById(R.id.header_progress).startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.refresh));
            getListView().setSelection(0);
        }


        @Override
        protected UserListBean doInBackground(Void... params) {
            try {
                return getDoInBackgroundNewData();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
            }
            return null;

        }

        @Override
        protected void onCancelled(UserListBean newValue) {
            super.onCancelled(newValue);
            if (this.e != null)
                Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
            cleanWork();
        }


        @Override
        protected void onPostExecute(UserListBean newValue) {
            if (newValue != null && newValue.getUsers().size() > 0) {

                clearAndReplaceValue(newValue);
                timeLineAdapter.notifyDataSetChanged();
                getListView().setSelectionAfterHeaderView();
//                headerView.findViewById(R.id.header_progress).clearAnimation();


            }

            cleanWork();
            getActivity().invalidateOptionsMenu();
            super.onPostExecute(newValue);

        }

        private void cleanWork() {
//            headerView.findViewById(R.id.header_progress).clearAnimation();
//            headerView.findViewById(R.id.header_progress).setVisibility(View.GONE);
//            headerView.findViewById(R.id.header_text).setVisibility(View.GONE);
            pullToRefreshListView.onRefreshComplete();

            isBusying = false;
            if (bean.getUsers().size() == 0) {
                footerView.findViewById(R.id.listview_footer).setVisibility(View.GONE);
            } else if (bean.getUsers().size() > 0 && bean.getUsers().size() < bean.getTotal_number()) {
                footerView.findViewById(R.id.listview_footer).setVisibility(View.VISIBLE);
            } else if ((bean.getUsers().size() > 0 && bean.getUsers().size() == bean.getTotal_number())) {
                footerView.findViewById(R.id.listview_footer).setVisibility(View.GONE);

            }
        }
    }


    private void showListView() {
        empty.setVisibility(View.INVISIBLE);
//        listView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }


    class UserListGetOlderDataTask extends MyAsyncTask<Void, UserListBean, UserListBean> {
        WeiboException e;

        @Override
        protected void onPreExecute() {
            showListView();
            isBusying = true;

            ((TextView) footerView.findViewById(R.id.listview_footer)).setText(getString(R.string.loading));
            View view = footerView.findViewById(R.id.refresh);
            view.setVisibility(View.VISIBLE);
            view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.refresh));

        }

        @Override
        protected UserListBean doInBackground(Void... params) {


            try {
                return getDoInBackgroundOldData();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
            }
            return null;

        }

        @Override
        protected void onCancelled(UserListBean newValue) {
            super.onCancelled(newValue);
            ((TextView) footerView.findViewById(R.id.listview_footer)).setText(getString(R.string.more));
            if (this.e != null)
                Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
            cleanWork();

        }

        @Override
        protected void onPostExecute(UserListBean newValue) {
            if (newValue != null && newValue.getUsers().size() > 0 && newValue.getPrevious_cursor() != bean.getPrevious_cursor()) {
                List<UserBean> list = newValue.getUsers();
                getList().getUsers().addAll(list);
                bean.setNext_cursor(newValue.getNext_cursor());
            }

            cleanWork();
            timeLineAdapter.notifyDataSetChanged();
            getActivity().invalidateOptionsMenu();
            super.onPostExecute(newValue);
        }


        private void cleanWork() {
            isBusying = false;
            ((TextView) footerView.findViewById(R.id.listview_footer)).setText(getString(R.string.more));
            footerView.findViewById(R.id.refresh).clearAnimation();
            footerView.findViewById(R.id.refresh).setVisibility(View.GONE);
            if (bean.getNext_cursor() == 0)
                footerView.findViewById(R.id.listview_footer).setVisibility(View.GONE);
        }
    }

    protected abstract UserListBean getDoInBackgroundNewData() throws WeiboException;

    protected abstract UserListBean getDoInBackgroundOldData() throws WeiboException;


}
