package com.samychen.gracefulwrapper.idcardrecognization;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
    private Bitmap fullImage;
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

    public void search(View v){
        Intent intent;
        if (Build.VERSION.SDK_INT>Build.VERSION_CODES.KITKAT){
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent,"选择图片"),100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==100&&null!=data){
            getResult(data.getData());
        }
    }

    private void getResult(Uri data) {
        String imagePath = null;
        if (null!=data){
            if ("file".equals(data.getScheme())){
                imagePath = data.getPath();
            } else if ("content".equals(data.getScheme())){
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor query = getContentResolver().query(data, filePathColumns, null, null, null);
                if (null!=query){
                    if (query.moveToFirst()){
                        int columnIndex = query.getColumnIndex(filePathColumns[0]);
                        imagePath = query.getString(columnIndex);
                    }
                    query.close();
                }
            }
        }
        if (!TextUtils.isEmpty(imagePath)){
            tesstext.setText(null);
            if (fullImage!=null){
                fullImage.recycle();
            }
            fullImage = toBitmap(imagePath);
            idCard.setImageBitmap(fullImage);
        }
    }

    private Bitmap toBitmap(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options); //此时返回 bm 为空
        options.inJustDecodeBounds = false; //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = (int)(options.outHeight / (float)320);
        if (be <= 0)
            be = 1;
        options.inSampleSize = be; //重新读入图片，注意此时已经把 options.inJustDecodeBounds 设回 false 了
        bitmap=BitmapFactory.decodeFile(imagePath,options);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        System.out.println(w+" "+h); //after zoom
        return bitmap;
    }

    public void rt(View view) {
        tesstext.setText(null);
        Bitmap input = BitmapFactory.decodeResource(getResources(), ids[index]);
        if (fullImage!=null){
            input = fullImage;
        }
        Bitmap result = Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888);
        Bitmap res = ImageUtils.findIdNumber(input, result, template,Bitmap.Config.ARGB_8888);
        input.recycle();
        idCard.setImageBitmap(res);
        baseApi.setImage(res);
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
