package com.samychen.gracefulwrapper.idcardrecognization;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.reactivestreams.Subscription;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private TessBaseAPI baseApi;
    private TextView tesstext;
    private ImageView idCard;
    private ProgressDialog progressDialog;
    private String language = "card";

    private Bitmap template;
    int index = 0;
    int[] ids = {R.drawable.id_card0, R.drawable.id_card1, R.drawable.id_card2};


    private void showProgress() {
        if (null != progressDialog) {
            progressDialog.show();
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("请稍候...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void dismissProgress() {
        if (null != progressDialog) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        idCard = (ImageView) findViewById(R.id.idcard);
        tesstext = (TextView) findViewById(R.id.tesstext);
        idCard.setImageResource(ids[index]);
        template = BitmapFactory.decodeResource(getResources(), R.drawable.template);
        initTess();
    }

    private void initTess() {
        //加载需要在子线程
        baseApi = new TessBaseAPI();
        Flowable.just("").map(new Function<String, Boolean>() {
            @Override
            public Boolean apply(String s) throws Exception {
                //目录+文件名 目录下需要tessdata目录
                InputStream is = getAssets().open(language + ".traineddata");
                File file = new File("/sdcard/tess/tessdata/" + language + ".traineddata");
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                is.close();
                return baseApi.init("/sdcard/tess", language);
            }
        }).onErrorReturnItem(false).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers
                .mainThread())
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription subscription) throws Exception {
                        showProgress();
                    }
                }).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean s) throws Exception {
                dismissProgress();
                if (s) {
                    Toast.makeText(MainActivity.this, "初始化OCR成功", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });
    }


    public void previous(View view) {
        tesstext.setText(null);
        index--;
        if (index < 0) {
            index = ids.length - 1;
        }
        idCard.setImageResource(ids[index]);
    }

    public void next(View view) {
        tesstext.setText(null);
        index++;
        if (index >= ids.length) {
            index = 0;
        }
        idCard.setImageResource(ids[index]);
    }


    public void rt(View view) {
        tesstext.setText(null);
        Bitmap input = BitmapFactory.decodeResource(getResources(), ids[index]);
        Bitmap result = Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888);
        ImageUtils.findIdNumber(input, result, template);
        input.recycle();
        idCard.setImageBitmap(result);
        baseApi.setImage(result);
        tesstext.setText(baseApi.getUTF8Text());
        // Flowable.just(ids[index]).map(new Function<Integer, Bitmap>() {
        //     @Override
        //     public Bitmap apply(Integer integer) throws Exception {
        //         //获得需要处理的图片
        //         Bitmap input = BitmapFactory.decodeResource(getResources(), integer);
        //         Bitmap result = Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888);
        //         ImageProcess.findIdNumber(input, result, template);
        //         input.recycle();
        //         return result;
        //     }
        // }).map(new Function<Bitmap, String>() {
        //     @Override
        //     public String apply(Bitmap bitmap) throws Exception {
        //         //待识别图片 识别需要ARGB_8888
        //         baseApi.setImage(bitmap);
        //         bitmap.recycle();
        //         return baseApi.getUTF8Text();
        //     }
        // }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe
        //         (new Consumer<Subscription>() {
        //             @Override
        //             public void accept(Subscription subscription) throws Exception {
        //                 showProgress();
        //             }
        //         }).onErrorReturn(new Function<Throwable, String>() {
        //     @Override
        //     public String apply(Throwable throwable) throws Exception {
        //         Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT)
        //                 .show();
        //         return "";
        //     }
        // }).subscribe(new Consumer<String>() {
        //     @Override
        //     public void accept(String s) throws Exception {
        //         dismissProgress();
        //         tesstext.setText(s);
        //     }
        // });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgress();
        baseApi.end();
        template.recycle();
    }

}
