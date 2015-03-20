package jp.co.tis.glassnavi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;


public class NaviDialogFragment extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //アラートのタイトルをセット
        builder.setTitle("Navigation");
        //アラートのメッセージをセット
        builder.setMessage("");
        //アラートのボタン設定
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d("Info", "ok");
            }
        });
        // AlertDialogをクリエイトしてリターン
        return builder.create();
    }

}
