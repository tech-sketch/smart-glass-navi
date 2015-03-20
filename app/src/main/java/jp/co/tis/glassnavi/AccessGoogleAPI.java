package jp.co.tis.glassnavi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class AccessGoogleAPI implements Serializable {


    private String distance;
    private String duration;
    //スタート地点の緯度経度
    private double startPointLat, startPointLng;
    //ゴール地点の緯度経度
    private double goalLat, goalLng;
    private ArrayList<Double> viaPointLat = new ArrayList<Double>();
    private ArrayList<Double> viaPointLng = new ArrayList<Double>();
    private ArrayList<String> message = new ArrayList<String>();
    private int numOfSteps;
    private Bitmap streetViewBitmap;
    private ImageView imageview;

    private AsyncTask<Uri, Void, String> mTask;
    private AsyncTask<Uri, Void, Bitmap> streetViewTask;

    /*
    * for GetStreetViewImage
    * */
    private String latPoint, lngPoint;
    //Latitudeの増加量[Amount of Increase]
    private double IncreaseOfLat;
    //Longitudeの増加量
    private double IncreaseOfLng;
    //緯度経度の傾き
    private double slope;
    private double angle;
    private int width;

    private int height;

    private boolean check = false;
    private boolean errorExist = false;
    private boolean jsonErrorExist = false;
    private Throwable mError;


    /*
    * ゲッター
    * */

    AccessGoogleAPI(){
    }


    AccessGoogleAPI(int width, int height){

        this.width = width;
        this.height = height;

    }

    public  String getDistance(){
        return distance;
    }

    public  String getDuration(){
        return duration;
    }

    public int getSteps(){
        return numOfSteps;
    }

    public double getStartLatitude(){
        return startPointLat;
    }

    public double getStartLongitude(){
        return startPointLng;
    }

    public double getEndLatitude(){
        return goalLat;
    }

    public double getEndLongitude(){
        return goalLng;
    }

    public double getCheckPointLatitude(int i){
        return viaPointLat.get(i);
    }

    public double getCheckPointLongitude(int i){
        return viaPointLng.get(i);
    }

    public String getMessage(int i){
        return message.get(i);
    }

    public void setNull(){
        streetViewBitmap = null;
    }

    public boolean getCheck(){
        return check;
    }

    public void setImageview(ImageView iv){ this.imageview = iv;}

    public boolean getErrorExist(){
        return errorExist;
    }

    public boolean getJsonErrorExist(){ return jsonErrorExist;}

    public ArrayList<String> getMessages(){
        return message;
    }

    public ArrayList<Double> getLatitudes(){
        return viaPointLat;
    }

    public ArrayList<Double> getLongitudes(){
        return viaPointLng;
    }

    public Throwable getError(){
        return mError;
    }



    public String getDirections(String latitude, String longitude, String destination) {

        String origin = latitude + "," + longitude;

        // URLを、扱いやすいUri型で組む
        Uri baseUri = Uri.parse("http://maps.googleapis.com/maps/api/directions/json");

        // パラメータの付与
        //出発点
        Uri uri = baseUri.buildUpon().appendQueryParameter("origin", origin).build();

        //到着地
        uri = uri.buildUpon().appendQueryParameter("destination", destination).build();
        //センサー
        uri = uri.buildUpon().appendQueryParameter("sensor", "false").build();
        //移動手段
        uri = uri.buildUpon().appendQueryParameter("mode", "walking").build();
        //言語
        uri = uri.buildUpon().appendQueryParameter("language", "ja").build();

        System.out.println(uri);

        String result = requestDirections(uri);

        return result;


    }


        private String requestDirections(Uri uri) {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        mError = null;


        // タイムアウトの設定
        HttpParams httpParams = httpClient.getParams();
        // 接続確立までのタイムアウト設定 (ミリ秒)
        HttpConnectionParams.setConnectionTimeout(httpParams,
                5 * 1000);
        // 接続後までのタイムアウト設定 (ミリ秒)
        HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);

        String result = null;
        HttpGet request = new HttpGet(uri.toString());
        try {
            Log.d("info", "connectionStart");
            result = httpClient.execute(request, new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse response) throws IOException {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {

                        String result = EntityUtils.toString(response.getEntity());
                        Log.d("info", "get");
                        return result;
                    } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                        throw new RuntimeException("404 NOT FOUND");
                    } else {
                        throw new RuntimeException("そのほかの通信エラー");
                    }
                }
            });
            Log.d("info", "Connection");
        } catch (RuntimeException e) {
            mError = e;
            errorExist = true;
            Log.e("info", "通信失敗1", e);
        } catch (ClientProtocolException e) {
            mError = e;
            errorExist = true;
            Log.e("info", "通信失敗2", e);
        } catch (IOException e) {
            mError = e;
            errorExist = true;
            Log.e("info", "通信失敗3", e);
        } finally {
            // リソースを開放する
            httpClient.getConnectionManager().shutdown();
        }


        if (mError == null) {

            check = true;
            parseRoutes(result);

        } else {
            errorExist = true;
            Log.d("postExecute", "error");

        }

        return result;
    }




    /*
    取得したJsonをパースするメソッド
    */
    public void parseRoutes(String json) {

        JSONArray jsonRoutes = null;
        JSONArray jsonLegs = null;
        JSONArray jsonSteps = null;


        try {


            JSONObject jsRoot = new JSONObject(json);

            //jsonが取得できているかの確認
            String status = jsRoot.getString("status");
            System.out.println("status:" + status);

            if(status.equals("OK")){
                jsonErrorExist = true;
            }
            jsonRoutes = jsRoot.getJSONArray("routes");



            jsonLegs = ((JSONObject) jsonRoutes.get(0)).getJSONArray("legs");


            //スタート地点・住所
            this.startPointLat = ((JSONObject) ((JSONObject) jsonLegs.get(0)).get("start_location")).getDouble("lat");
            this.startPointLng = ((JSONObject) ((JSONObject) jsonLegs.get(0)).get("start_location")).getDouble("lng");

            this.goalLat = ((JSONObject) ((JSONObject) jsonLegs.get(0)).get("end_location")).getDouble("lat");
            this.goalLng = ((JSONObject) ((JSONObject) jsonLegs.get(0)).get("end_location")).getDouble("lng");

            distance = ((JSONObject) ((JSONObject) jsonLegs.get(0)).get("distance")).getString("text");
            this.duration = ((JSONObject) ((JSONObject) jsonLegs.get(0)).get("duration")).getString("text");

            jsonSteps = ((JSONObject) jsonLegs.get(0)).getJSONArray("steps");

            this.numOfSteps = jsonSteps.length();


            for (int i = 0; i < jsonSteps.length(); i++) {

                this.viaPointLat.add(((JSONObject) ((JSONObject) jsonSteps.get(i)).get("start_location")).getDouble("lat"));
                this.viaPointLng.add(((JSONObject) ((JSONObject) jsonSteps.get(i)).get("start_location")).getDouble("lng"));
                String instructions = ((JSONObject) jsonSteps.get(i)).getString("html_instructions");
                //htmlタグの除去
                this.message.add(HtmlTagRemover(instructions));

            }

        } catch (JSONException e) {
            Log.e("json", "json parse error");
        } catch (Exception e) {
            Log.e("Error", "exception error");
        }
    }


    private static String HtmlTagRemover(String str) {
        // 文字列のすべてのタグを取り除く
        return str.replaceAll("<.+?>", "");
    }



    public Bitmap getStreetViewBitmap(int steps) {

        //緯度経度のString変換
        if(steps < numOfSteps) {
            latPoint = String.valueOf(viaPointLat.get(steps));
            lngPoint = String.valueOf(viaPointLng.get(steps));
        }else if(steps == numOfSteps){
            latPoint = String.valueOf(goalLat);
            lngPoint = String.valueOf(goalLng);
        }
        //APIに投げる角度の取得とString変換
        String angle = String.valueOf(GetAngle(steps));

        // URLを、扱いやすいUri型で組む
        Uri baseUri = Uri.parse("http://maps.googleapis.com/maps/api/streetview");

        // パラメータの付与
        //画像サイズ
        Uri uri = baseUri.buildUpon().appendQueryParameter("size", width + "x" + height).build();
        //取得位置
        uri = uri.buildUpon().appendQueryParameter
                //("location", "35.698770,139.769225").build();
                ("location", latPoint + ", " + lngPoint).build();
        //角度
        uri = uri.buildUpon().appendQueryParameter("heading", angle).build();
        //センサー
        uri = uri.buildUpon().appendQueryParameter("sensor", "false").build();
        System.out.println(uri);


        if (streetViewTask == null) {
            streetViewTask = new AsyncTask<Uri, Void, Bitmap>() {
                /**
                 * 通信において発生したエラー
                 */
                private Throwable mError = null;

                @Override
                protected Bitmap doInBackground(Uri... params) {
                    try {
                        String src = String.valueOf(params[0]);
                        URL url = new URL(src);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap myBitmap = BitmapFactory.decodeStream(input);
                        return myBitmap;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap result) {

                    imageview.setImageBitmap(result);
                    if (mError == null) {
                        streetViewBitmap = result;
                    } else {
                        Log.d("postExecute", "error");
                    }

                    streetViewTask = null;

                }

                @Override
                protected void onCancelled() {
                    onCancelled(null);
                }

                @Override
                protected void onCancelled(Bitmap result) {
                    Log.e("onCancel", "error");

                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);


        }
        return streetViewBitmap;
    }


    private double GetAngle(int i){

        //latitudeの増加量(Amount of increase)

        if(i < numOfSteps) {

            IncreaseOfLat = viaPointLat.get(i + 1) - viaPointLat.get(i);
            IncreaseOfLng = viaPointLng.get(i + 1) - viaPointLng.get(i);

        }else if(i == numOfSteps) {

            IncreaseOfLat = goalLat - viaPointLat.get(i);
            IncreaseOfLng = goalLng - viaPointLng.get(i);

        }
        //移動時の傾き
        slope = IncreaseOfLat / IncreaseOfLng;
        System.out.println("slope:" + slope);

        //北を０度とした右回りのの傾き角度
        angle = Math.toDegrees(Math.atan(slope));

        if(IncreaseOfLng >0.0){
            angle = 90.0 - angle;
        }else if(IncreaseOfLng <0.0){
            angle = 270.0 - angle;
        }
        System.out.println("angle:" + angle);

        return angle;
    }

}
