package com.cry.zero_camera.activity.ppt;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.cry.zero_camera.R;

import java.util.ArrayList;

public class PickMorePicsActivity extends AppCompatActivity {

    ArrayList<Bitmap> bitmaps = new ArrayList<>();
    private RecyclerView recyclerView;
    private PhotoViewAdapter photoViewAdapter;
    private ArrayList<String> bitmapResult = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_more_pics);


        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                i.setType("image/*");
                startActivityForResult(i, 1);
            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent1 = getIntent();
                Intent intent = new Intent(PickMorePicsActivity.this, PhotoAnimateSimpleActivity.class);
                intent.putExtra("bitmapPath", bitmapResult);
                setResult(5, intent);
                PickMorePicsActivity.this.finish();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.rcv);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        photoViewAdapter = new PhotoViewAdapter(bitmaps);
        recyclerView.setAdapter(photoViewAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 选取图片的返回值
        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                Cursor cursor = null;
                try {
                    String[] filePathColumn = {MediaStore.Video.Media.DATA};

                    cursor = getContentResolver().query(uri,
                            filePathColumn, null, null, null);
                    if (cursor == null) {
                        return;
                    }
                    cursor.moveToFirst();
                    String filePath = cursor.getString(0); // 图片编号

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                    BitmapFactory.decodeFile(filePath, options);
                    int widthPixels = displayMetrics.widthPixels / 2;
                    System.out.println("widthPixels=" + widthPixels);
                    int outWidth = options.outWidth;
                    System.out.println("outWidth=" + outWidth);
                    options.inJustDecodeBounds = false;


                    int scale = ((int) (outWidth * 1f / widthPixels * 1f)) >> 1 << 1;

                    options.inSampleSize = scale;

                    Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                    int height = bitmap.getHeight();
                    int width = bitmap.getWidth();
                    Toast.makeText(this, "bitmap height=" + height + ",width=" + width + " ,scale = " + scale, Toast.LENGTH_SHORT).show();

                    bitmaps.add(bitmap);
                    bitmapResult.add(filePath);
                    photoViewAdapter.notifyItemInserted(bitmaps.size() - 1);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    public static class PhotoViewAdapter extends RecyclerView.Adapter<PhotoViewHolder> {

        private final ArrayList<Bitmap> bitmaps;

        public PhotoViewAdapter(ArrayList<Bitmap> bitmaps) {
            this.bitmaps = bitmaps;
        }


        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new PhotoViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.simple_photo, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder photoViewHolder, int i) {
            photoViewHolder.ImagePic.setImageBitmap(bitmaps.get(i));
        }

        @Override
        public int getItemCount() {
            return bitmaps.size();
        }
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ImagePic;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ImagePic = itemView.findViewById(R.id.pic);
        }
    }
}
