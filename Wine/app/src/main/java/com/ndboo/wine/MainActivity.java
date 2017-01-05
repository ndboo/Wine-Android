package com.ndboo.wine;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.ndboo.base.BaseActivity;
import com.ndboo.ui.fragment.BusinessFragment;
import com.ndboo.ui.fragment.IndexFragment;
import com.ndboo.ui.fragment.MallFragment;
import com.ndboo.ui.fragment.MineFragment;
import com.ndboo.widget.FixedViewPager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class MainActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {


    public AMapLocationClient mAMapLocationClient = null;
    public AMapLocationListener mAMapLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode()==0) {
                    String location=
                            aMapLocation.getCountry()+aMapLocation.getProvince()+
                                    aMapLocation.getCity()+aMapLocation.getDistrict()+
                                    aMapLocation.getStreet()+aMapLocation.getStreetNum();
                    mTvLocation.setText(location);
                }else {
                    Toast.makeText(MainActivity.this, "定位失败"+aMapLocation.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    public AMapLocationClientOption mLocationClientOption = null;
    @BindView(R.id.view_pager_main)
    FixedViewPager mViewPagerMain;
    @BindView(R.id.rb_index)
    RadioButton mRbIndex;
    @BindView(R.id.rb_type)
    RadioButton mRbType;
    @BindView(R.id.rb_shopping_car)
    RadioButton mRbShoppingCar;
    @BindView(R.id.rb_mine)
    RadioButton mRbMine;
    @BindView(R.id.tv_location)
    TextView mTvLocation;
    /**
     * fragment集合
     * 包含首页、商城、商家、我的四个界面
     */
    private List<Fragment> mFragments;
    private MallFragment mMallFragment;
    private IndexFragment mIndexFragment;

    public IndexFragment getIndexFragment() {
        return mIndexFragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void init() {
        getLocation();
        mFragments = new ArrayList<>();

        mIndexFragment = new IndexFragment();
        mMallFragment = new MallFragment();
        BusinessFragment carFragment = new BusinessFragment();
        MineFragment mineFragment = new MineFragment();
        mFragments.add(mIndexFragment);
        mFragments.add(mMallFragment);
        mFragments.add(carFragment);
        mFragments.add(mineFragment);

        mViewPagerMain.setOffscreenPageLimit(mFragments.size());
        mViewPagerMain.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }
        });
        mRbIndex.setOnCheckedChangeListener(this);
        mRbType.setOnCheckedChangeListener(this);
        mRbShoppingCar.setOnCheckedChangeListener(this);
        mRbMine.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            int tag = Integer.parseInt(buttonView.getTag().toString());
            mViewPagerMain.setCurrentItem(tag, false);
        }
    }

    /**
     * 返回键处理
     * 此处暂时用来处理按下时，在商城Tab中，下拉筛选是否关闭
     */
    @Override
    public void onBackPressed() {
        if (mMallFragment.getDropLayout().isMenuOpen()) {
            mMallFragment.getDropLayout().closeMenu();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 转到商城
     */
    public void turnToMall() {
        mRbType.setChecked(true);
    }

    private void getLocation() {
        mAMapLocationClient = new AMapLocationClient(getApplicationContext());
        mAMapLocationClient.setLocationListener(mAMapLocationListener);

        mLocationClientOption = new AMapLocationClientOption();
        mLocationClientOption.setOnceLocation(true);
        mLocationClientOption.setOnceLocationLatest(true);
        mAMapLocationClient.setLocationOption(mLocationClientOption);
        mAMapLocationClient.startLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAMapLocationClient.stopLocation();
        mAMapLocationClient.onDestroy();
    }
}
