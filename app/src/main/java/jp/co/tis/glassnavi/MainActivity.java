package jp.co.tis.glassnavi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.location.LocationListener;
import android.location.LocationManager;

import android.util.Log;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;

public class MainActivity extends Activity
        implements LocationListener{

    private LocationManager mLocationManager;
    private GoogleAPIAccessor mAccessor;

    //経由点とナビメッセージ
    private ArrayList<Double> mViaPointLat;
    private ArrayList<Double> mViaPointLng;
    private ArrayList<String> mMessage;

    //ステップの総数と現在のステップ数
    private int mTotalSteps;
    private int mViaPointNumber = 0;

    //現在位置、次の移動先との誤差(緯度、経度)
    private double mNowLat, mNowLng;
    private double mDiffLat, mDiffLng;

    //Displayサイズ

    private Point mSize = new Point();
    private int mWidth;

    private TextView mDiffLatText;
    private TextView mDiffLngText;


    //緯度経度約20m
    //参考：http://mononofu.hatenablog.com/entry/20090324/1237894846
    private final static double DIFF_LAT = 0.0001797538;
    private final static double DIFF_LNG = 0.0002193964;

    private Bitmap mNaviView = null;
    private ImageView mImageView;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDiffLatText = (TextView)findViewById(R.id.NowLatitude);
        mDiffLngText = (TextView)findViewById(R.id.NowLongitude);

        //前のActivityから渡された値を取得
        Intent intent = getIntent();
        mMessage = intent.getStringArrayListExtra("messages");
        mViaPointLat = (ArrayList<Double>)intent.getSerializableExtra("latitudes");
        mViaPointLng = (ArrayList<Double>)intent.getSerializableExtra("longitudes");
        mTotalSteps = intent.getExtras().getInt("steps");
        mAccessor = (GoogleAPIAccessor)intent.getSerializableExtra("aG");

        //StreetViewの画像を張るImageViewをaccessGoogleオブジェクトに渡す
        mImageView = (ImageView)findViewById(R.id.NextImage);
        mAccessor.setImageview(mImageView);

        //imageViewをタップしたら画像を消去(nullを代入)
        mImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mImageView.setImageBitmap(null);
            }
        });

        //ロケーションマネージャーの再設定
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_LOW);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        //Criteriaを元にプロバイダを取得
        String provider = mLocationManager.getBestProvider(criteria, true);
        //プロバイダを表示
        System.out.println("Provider: " + provider);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //位置情報の取得
        //mLocationManager.requestLocationUpdates(provider, 100, 0, this);
        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, this);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 0, this);

    }


    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
    }

    /*
    現在地情報が変化した時に実行
    現在地の取得とアラート表示を行うメソッド
    */
    @Override
    public void onLocationChanged(Location location) {

        System.out.println(location.getLatitude());

        mNowLat = location.getLatitude();
        mDiffLat = Math.abs(mNowLat - mViaPointLat.get(mViaPointNumber));

        mDiffLatText.setText("diff_latitude:" + BigDecimal.valueOf(mDiffLat).toPlainString());
        System.out.println("diff_latitude:" + BigDecimal.valueOf(mDiffLat).toPlainString());
        System.out.println("now_latitude:" + BigDecimal.valueOf(mNowLat).toPlainString());

        mNowLng = location.getLongitude();
        System.out.println(mNowLng);
        System.out.println(mViaPointLng.get(0));
        mDiffLng = Math.abs(mNowLng - mViaPointLng.get(mViaPointNumber));

        mDiffLngText.setText("diff_longitude:" + BigDecimal.valueOf(mDiffLng).toPlainString());
        System.out.println("diff_longitude:" + BigDecimal.valueOf(mDiffLng).toPlainString());
        System.out.println("now_longitude:" + BigDecimal.valueOf(mNowLng).toPlainString());

        System.out.print(mViaPointNumber + "/");
        System.out.println(mTotalSteps);

        //アラートメッセージの設定
        //20m以内に近づいたらアラート
        //http://mononofu.hatenablog.com/entry/20090324/1237894846
        if (mViaPointNumber < mTotalSteps -1 &&
                mDiffLat < DIFF_LAT && mDiffLng < DIFF_LNG) {

            mNaviView = mAccessor.getStreetViewBitmap(mViaPointNumber);
            System.out.println("ok:" + mViaPointNumber);
            showDialog(mMessage.get(mViaPointNumber));

            mViaPointNumber += 1;

        }else if(mViaPointNumber == mTotalSteps -1 &&
                mDiffLat < DIFF_LAT && mDiffLng < DIFF_LNG) {

            mNaviView = mAccessor.getStreetViewBitmap(mViaPointNumber);
            showDialog(mMessage.get(mViaPointNumber));

            mViaPointNumber += 1;

        }else if(mViaPointNumber == mTotalSteps &&
                mDiffLat < DIFF_LAT && mDiffLng < DIFF_LNG) {
            showDialog("Goal!!");
            mLocationManager.removeUpdates(this);
            finish();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    /*
    アラートをセットし、showするメソッド
    */
    public Dialog showDialog(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //アラートのタイトルをセット
        builder.setTitle(message);
        //アラートのボタン設定
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d("Info", "ok");
            }
        });
        return builder.show();
    }

}