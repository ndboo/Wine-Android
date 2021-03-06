package com.ndboo.ui.fragment;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ndboo.adapter.CartAdapter;
import com.ndboo.base.BaseFragment;
import com.ndboo.bean.CartBean;
import com.ndboo.interfaces.ShoppingCarOnItemClickListener;
import com.ndboo.net.RetrofitHelper;
import com.ndboo.utils.SharedPreferencesUtil;
import com.ndboo.utils.ToastUtil;
import com.ndboo.widget.DeletePopupWindow;
import com.ndboo.widget.TopBar;
import com.ndboo.wine.EditOrderActivity;
import com.ndboo.wine.LoginActivity;
import com.ndboo.wine.MainActivity;
import com.ndboo.wine.R;
import com.ndboo.wine.WineDetailActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Li on 2016/12/26.
 * “购物车”界面
 */

public class ShoppingCarFragment extends BaseFragment {
    private static final boolean TYPE_EDIT = false;
    private static final boolean TYPE_COMPLETE = true;

    @BindView(R.id.list_view_car_wines)
    ListView mListViewCarWines;
    @BindView(R.id.tv_edit_complete)
    TextView mTvEditComplete;
    @BindView(R.id.top_bar)
    TopBar mTopBar;

    private List<CartBean> mCartBeanList = new ArrayList<>();
    private CartAdapter mCartAdapter;

    private String mEdit;
    private String mComplete;

    // 当前操作类型
    private boolean mCurrentType = TYPE_COMPLETE;
    //选中的复选框集合
    private List<String> mSelectedList = new ArrayList<>();

    //底部
    @BindView(R.id.layout_cart_bottom)
    LinearLayout mBottomLinearLayout;

    //底部-结算
    @BindView(R.id.cart_bottom_pay)
    RelativeLayout mBottomLayout1;
    @BindView(R.id.cart_bottom_pay_totalprice)
    TextView mTotalPriceTextView;

    //底部-删除
    @BindView(R.id.cart_bottom_delete)
    RelativeLayout mBottomLayout2;
    @BindView(R.id.cart_bottom_delete_checkbox)
    CheckBox mCheckBox;

    //没有商品
    @BindView(R.id.layout_cart_null)
    LinearLayout mNoProductLayout;
    @BindView(R.id.layout_cart_null_text)
    TextView mNoProductText;

    //总价
    private String mTotalMoney = "0.00";

    //是否是第一次进入
    private boolean mIsFirstIn = true;

    /**
     * 删除商品弹框
     */
    private DeletePopupWindow mDeletePopupWindow;
    //要删除的商品id
    private String mDeleteIds;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_shopping_car;
    }


    /**
     * 修改商品数量
     */
    private void updateProductCount(String productId, int productCount) {
        Subscription subscription = RetrofitHelper.getApi()
                .modifyProductNum(SharedPreferencesUtil.getUserId(getActivity()), productId, "" + productCount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String string) {
//                        Log.e("ndb", "result:" + string);
                        try {
                            JSONObject jsonObject = new JSONObject(string);
                            String result = jsonObject.optString("result");
                            if (result.equals("true")) {
                                requestData();
                            } else {
                                ToastUtil.showToast(getActivity(), "修改数量失败");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        ToastUtil.showToast(getActivity(), "网络异常，请检查网络状况");
                    }
                });
        addSubscription(subscription);
    }

    @Override
    protected void visibleDeal() {
        super.visibleDeal();
        mCartBeanList.clear();
        mSelectedList.clear();
        requestData();
    }

    /**
     * 获取购物车列表
     */
    private void requestData() {
        if (!SharedPreferencesUtil.isUserLoginIn(getActivity())) {
            if (mCartBeanList.size() != 0) {
                mCartBeanList.clear();
                mCartAdapter.notifyDataSetChanged();
            }
            mNoProductLayout.setVisibility(View.VISIBLE);
            mBottomLinearLayout.setVisibility(View.GONE);
            return;
        }
        mCheckBox.setChecked(false);
        Subscription subscription = RetrofitHelper.getApi()
                .getCartProductsList(SharedPreferencesUtil.getUserId(getActivity()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String string) {
                        mIsFirstIn = false;
//                        Log.e("ndb", "result:" + string);
                        try {
                            JSONObject jsonObject = new JSONObject(string);
                            //总价
                            mTotalMoney = jsonObject.optString("totalMoney", "0.00");
                            mTotalPriceTextView.setText("总价：" + mTotalMoney + "元");
                            //商品信息
                            JSONArray jsonArray = jsonObject.optJSONArray("productInfromation");
                            mCartBeanList.clear();
                            if (jsonArray == null) {
                                mCurrentType = TYPE_EDIT;
                                changeEditMode();
                                mCartAdapter.notifyDataSetChanged();
                                mNoProductLayout.setVisibility(View.VISIBLE);
                                mBottomLinearLayout.setVisibility(View.GONE);
                                return;
                            }
                            mBottomLinearLayout.setVisibility(View.VISIBLE);
                            mNoProductLayout.setVisibility(View.GONE);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject cartObject = jsonArray.getJSONObject(i);
                                mCartBeanList.add(new Gson().fromJson(cartObject.toString(), CartBean.class));
                            }
                            mCartAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        ToastUtil.showToast(getActivity(), "网络异常，请检查网络状况");
                    }
                });
        addSubscription(subscription);
    }

    @OnClick({R.id.tv_edit_complete, R.id.cart_bottom_delete_delete,
            R.id.cart_bottom_pay_topay})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_edit_complete:
                if (!SharedPreferencesUtil.isUserLoginIn(getActivity())) {
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    return;
                }
                if (mCartBeanList.size() == 0) {
                    ToastUtil.showToast(getActivity(), "暂无商品");
                    return;
                }
                changeEditMode();
                mCartAdapter.notifyDataSetChanged();
                break;
            case R.id.cart_bottom_delete_delete:
                //删除商品
                if (mCartBeanList.size() == 0) {
                    ToastUtil.showToast(getActivity(), "暂无商品");
                    return;
                }
                mSelectedList = mCartAdapter.getCheckedPositionList();
                if (mSelectedList.size() == 0) {
                    ToastUtil.showToast(getActivity(), "请选择商品");
                    return;
                }
                mDeleteIds = "";
                for (int i = 0; i < mSelectedList.size(); i++) {
                    String position = mSelectedList.get(i);
                    CartBean cartBean = mCartBeanList.get(Integer.parseInt(position));
                    mDeleteIds += cartBean.getProductId();
                    if (i != mSelectedList.size() - 1) {
                        mDeleteIds += ",";
                    }
                }
                deleteDialog();
                break;
            case R.id.cart_bottom_pay_topay:
                if (!SharedPreferencesUtil.isUserLoginIn(getActivity())) {
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                } else {
                    //去结算
                    if (mCartBeanList.size() == 0) {
                        ToastUtil.showToast(getActivity(), "暂无商品");
                        return;
                    }
                    /*double money = Double.parseDouble(mTotalMoney);
                    if (money < 100) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                                .setTitle("温馨提示")
                                .setMessage("商品满100起送,请再添加一些商品")
                                .setPositiveButton("确定", null);
                        builder.create().show();
                        return;
                    }*/
                    Intent intent = new Intent(getActivity(), EditOrderActivity.class);
                    //获取所有id
                    String goodsIds = "";
                    for (int i = 0; i < mCartBeanList.size(); i++) {
                        goodsIds += mCartBeanList.get(i).getProductId();
                        if (i != mCartBeanList.size() - 1) {
                            goodsIds += ",";
                        }
                    }
                    intent.putExtra("productIds", goodsIds);
                    intent.putExtra("type", 1);
                    startActivity(intent);
                }
                break;
        }
    }

    private void deleteDialog() {
        if (mDeletePopupWindow == null) {
            mDeletePopupWindow = new DeletePopupWindow(getActivity());
            mDeletePopupWindow.setOnPopupWindowClickListener(new DeletePopupWindow.OnPopupWindowClickListener() {
                @Override
                public void cancleClicked(View view) {
                }

                @Override
                public void ensureClicked(View view) {
                    deleteProduct();
                }
            });
        }
        mDeletePopupWindow.showAtLocation(mTopBar, Gravity.CENTER, 0, 0);
    }

    /**
     * 删除购物车商品
     */
    private void deleteProduct() {
        Subscription subscription = RetrofitHelper.getApi()
                .deleteFromCart(SharedPreferencesUtil.getUserId(getActivity()), mDeleteIds)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String string) {
//                        Log.e("ndb", "result:" + string);
                        try {
                            JSONObject jsonObject = new JSONObject(string);
                            String result = jsonObject.optString("result");
                            if (result.equals("true")) {
                                ToastUtil.showToast(getActivity(), "已删除");
                                mSelectedList.clear();
                                mCartAdapter.setCheckedPositionList(mSelectedList);
                                ((MainActivity) getActivity()).queryWineNum();
                                requestData();
                            } else {
                                ToastUtil.showToast(getActivity(), "删除失败,请刷新重试");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        ToastUtil.showToast(getActivity(), "网络异常，请检查网络状况");
                    }
                });
        addSubscription(subscription);
    }

    private void changeEditMode() {
        if (mCurrentType == TYPE_EDIT) {
            mTvEditComplete.setText(mEdit);
            mBottomLayout1.setVisibility(View.VISIBLE);
            mBottomLayout2.setVisibility(View.GONE);
        } else {
            mTvEditComplete.setText(mComplete);
            mBottomLayout2.setVisibility(View.VISIBLE);
            mBottomLayout1.setVisibility(View.GONE);
        }
        mCartAdapter.setShowCheckBox(mCurrentType);
        mCurrentType = !mCurrentType;
    }

    @Override
    public void firstVisibleDeal() {
        super.firstVisibleDeal();
        mEdit = getResources().getString(R.string.car_edit);
        mComplete = getResources().getString(R.string.car_complete);

        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = mCheckBox.isChecked();
                mSelectedList.clear();
                if (checked) {
                    for (int i = 0; i < mCartBeanList.size(); i++) {
                        mSelectedList.add("" + i);
                    }
                }
                mCartAdapter.setCheckedPositionList(mSelectedList);
                mCartAdapter.notifyDataSetChanged();
            }
        });

        mCartBeanList = new ArrayList<>();
        mCartAdapter = new CartAdapter(getActivity(), mCartBeanList);
        mCartAdapter.setListener(new ShoppingCarOnItemClickListener() {
            @Override
            public void numAdd(int position, View view) {
                CartBean cartBean = mCartBeanList.get(position);
                int productCount = Integer.parseInt(cartBean.getProductCount());
                updateProductCount(cartBean.getProductId(), ++productCount);
            }

            @Override
            public void numReduce(int position, View view) {
                CartBean cartBean = mCartBeanList.get(position);
                int productCount = Integer.parseInt(cartBean.getProductCount());
                if (productCount == 1) {
                    mDeleteIds = cartBean.getProductId();
                    deleteDialog();
                } else {
                    updateProductCount(cartBean.getProductId(), --productCount);
                }
            }

            @Override
            public void viewClick(int position, View view) {
            }

            @Override
            public void onAllChanged(boolean isChecked) {
                if (mCheckBox.isChecked() != isChecked) {
                    mCheckBox.setChecked(isChecked);
                    mCartAdapter.notifyDataSetChanged();
                }
            }
        });
        mListViewCarWines.setAdapter(mCartAdapter);

        mListViewCarWines.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getActivity(), WineDetailActivity.class);
                intent.putExtra("wineId", mCartBeanList.get(i).getProductId());
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsFirstIn)
            requestData();
    }

    /**
     * 弹框是否已经弹出
     */
    public boolean isShow() {
        if (mDeletePopupWindow == null) {
            return false;
        } else {
            return mDeletePopupWindow.isShowing();
        }
    }

    /**
     * 关闭PopupWindow
     */
    public void closePop() {
        mDeletePopupWindow.dismiss();
    }
}
