package jp.co.tis.glassnavi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class SetGoalActivity extends Activity implements OnClickListener, LocationListener {


    private LocationManager mLocationManager;

    private String mLatitude, mLongitude;
    private String mDestination;
    private int steps;
    private boolean mLocationExist;
    private Criteria mCriteria;

    private String mProvider;
    private TextView mLocationText;

    private Point mSize = new Point();
    private int mWidth;
    private int mHeight;

    private AccessGoogleAPI mAccessGoogle;
    private ProgressDialog mProgressDialog;
    private Intent mIntent;

    private Context mContext;

    private AsyncTask<Void, Void, String> mTask;

    private ArrayList<Double> mViaPointLat = new ArrayList<Double>();
    private ArrayList<Double> mViaPointLng = new ArrayList<Double>();
    private ArrayList<String> mMessages = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_goal);

        mLocationText = (TextView) findViewById(R.id.location_parameter);

        Display display =  getWindowManager().getDefaultDisplay();
        display.getSize(mSize);
        mWidth = mSize.x;
        mHeight = mSize.y;


        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mCriteria = new Criteria();
        //精度高
        mCriteria.setAccuracy(Criteria.ACCURACY_LOW);
        //電池消費量低
        mCriteria.setPowerRequirement(Criteria.POWER_LOW);


        //位置情報の取得
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //Criteriaを元にプロバイダを取得
        mProvider = mLocationManager.getBestProvider(mCriteria, true);
        //プロバイダを表示
        System.out.println("Provider: " + mProvider);
        //ロケーションサービスを開始
        //mLocationManager.requestLocationUpdates(mProvider, 100, 0, this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, this);
        //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 0, this);

    }


    //戻るボタンを押した時に現在位置とルートをリセット
    @Override
    protected void onRestart(){
        super.onRestart();

        mTask = null;

        mLocationText.setText("現在位置を取得中");
        //mLocationManager.requestLocationUpdates(mProvider, 100, 0, this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, this);
        //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 0, this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
    }


    @Override
    public void onLocationChanged(Location location) {

        //取得した現在地の緯度経度情報をStringで取得
        mLatitude = String.valueOf(location.getLatitude());
        mLongitude = String.valueOf(location.getLongitude());

        //位置情報が取得できたフラグ
        mLocationExist = true;

        //緯度経度が1度でも取得できた時点で処理を終了
        mLocationManager.removeUpdates(this);

        //位置情報が取得できたらUIに表示
        mLocationText.setText("現在位置の取得を完了");

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
    OKボタンが押された際の処理を記述
     */
    @Override
    public void onClick(View v) {

        //入力された目的地を取得
        EditText destinationText = (EditText) findViewById(R.id.edit_destination);
        mDestination = destinationText.getText().toString();



        //目的地が入力されていない場合
        if (mDestination.length() == 0) {

            Toast.makeText(this, "目的地を入力してください", Toast.LENGTH_LONG).show();

        }
        //現在位置の取得が未完な場合
        else if (!mLocationExist) {

            Toast.makeText(this, "現在位置を取得中です", Toast.LENGTH_LONG).show();

        }
        //情報が揃っている望むべき処理
        else {

            System.out.println(mDestination);
            System.out.println(mLatitude);
            System.out.println(mLongitude);

            mAccessGoogle = new AccessGoogleAPI(mWidth, mHeight);
            mProgressDialog = new ProgressDialog(this);
            mIntent = new Intent(getApplicationContext(), MainActivity.class);

            //非同期処理でルート情報を取得
            if (mTask == null) {
                mTask = new AsyncTask<Void, Void, String>() {


                    //通信において発生したエラー
                    private Throwable mError = null;

                    //非同期処理の前処理
                    @Override
                    protected void onPreExecute() {

                        //ProgressDialogの設定
                        mProgressDialog.setTitle("ルート情報を取得中");
                        mProgressDialog.setMessage("しばらくお待ちください");
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // ProgressDialog をキャンセル
                                        Log.d("test", "BUTTON_CANCEL clicked");
                                        dialog.cancel();
                                        mTask = null;

                                    }

                                });
                        //progressDialogの開始
                        mProgressDialog.show();
                    }

                    //非同期で行う処理
                    @Override
                    protected String doInBackground(Void... params) {

                        //現在地と目的地を渡し、DirectionsAPIからルート情報のjsonを取得
                        String result = mAccessGoogle.getDirections(mLatitude, mLongitude, mDestination);
                        if (isCancelled()) {
                            return result;
                        }
                        return result;
                    }

                    //ルート取得後
                    @Override
                    protected void onPostExecute(String result) {

                        if (mError == null) {
                            //非同期で取得したjsonをパース
                            mAccessGoogle.parseRoutes(result);

                            if(!mAccessGoogle.getJsonErrorExist()){

                                Toast.makeText(getApplicationContext(),
                                        "入力した目的地がみつかりません", Toast.LENGTH_LONG).show();
                                mTask = null;
                                mProgressDialog.dismiss();

                            }else {
                                //パースしたルート上方からナビメッセージアレイを取得
                                mMessages = mAccessGoogle.getMessages();
                                //経由点の緯度アレイを取得
                                mViaPointLat = mAccessGoogle.getLatitudes();
                                //経由点の経度アレイを取得
                                mViaPointLng = mAccessGoogle.getLongitudes();
                                //総ステップ数を取得
                                steps = mAccessGoogle.getSteps();

                                //エラーが発生していれば取得
                                mError = mAccessGoogle.getError();
                                System.out.println("エラー:" + mError);

                                //ProgressDialogを終了
                                mProgressDialog.dismiss();

                                //通信エラーが発生していた場合
                                if (mAccessGoogle.getErrorExist()) {
                                    Toast.makeText(mContext.getApplicationContext(), "通信エラー", Toast.LENGTH_LONG).show();
                                    mTask = null;
                                } else {
                                    //インテントに取得した値を渡す
                                    mIntent.putExtra("latitudes", mViaPointLat);
                                    mIntent.putExtra("longitudes", mViaPointLng);
                                    mIntent.putExtra("messages", mMessages);
                                    mIntent.putExtra("steps", steps);

                                    //accessGoogleオブジェクトを渡す
                                    mIntent.putExtra("aG", mAccessGoogle);

                                    //Activityの開始
                                    startActivity(mIntent);
                                }
                            }
                        }
                        //非同期処理のエラー
                        else {
                            Log.d("postExecute", "error");
                            mTask = null;
                        }

                    }

                    @Override
                    protected void onCancelled() {
                        onCancelled(null);
                    }

                    @Override
                    protected void onCancelled(String result) {
                        Log.e("onCancel", "error");

                    }
                }.execute();
            }


        }


    }
}