package com.ybdev.facerecognition.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.ybdev.facerecognition.R;
import com.ybdev.facerecognition.adapters.MatchAdapter;
import com.ybdev.facerecognition.ml.MobilenetV110224Quant;
import com.ybdev.facerecognition.ml.SsdMobilenetV11Metadata1;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    private static final int RESULT_LOAD_IMAGE = 1;
    private ImageView Main_IMG;
    private RecyclerView Main_RecyclerView;
    private MaterialButton Main_BTN_select;
    private MaterialButton Main_BTN_detect;
    private ArrayList<String> arrLabel;

    //https://www.tensorflow.org/lite/examples/image_classification/overview
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        setArrayFromFile();
        Main_BTN_select.setOnClickListener(view -> SelectImage());
        Main_BTN_detect.setOnClickListener(view -> detectImage());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){

            try {
                Uri imageUri;
                if (data != null) {
                    imageUri = data.getData();
                    InputStream imageStream;
                    imageStream = getContentResolver().openInputStream(imageUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    Main_IMG.setImageBitmap(selectedImage);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(this, "No picture was selected", Toast.LENGTH_SHORT).show();
        }
    }



    private void setArrayFromFile() {
        arrLabel = new ArrayList<>();
        String fileName = "label.txt";


        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
            String line;
            while((line = reader.readLine()) != null){
                arrLabel.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void detectImage(){
        Bitmap bitmap = ((BitmapDrawable) Main_IMG.getDrawable()).getBitmap();

        Bitmap resizeImage = Bitmap.createScaledBitmap(bitmap, 224,224,true);
        try {
            MobilenetV110224Quant model = MobilenetV110224Quant.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);

            TensorImage selectImage = TensorImage.fromBitmap(resizeImage);

            ByteBuffer byteBuffer = selectImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            MobilenetV110224Quant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            sortArray(outputFeature0.getFloatArray());

            SsdMobilenetV11Metadata1 ssdMobilenetV11Metadata1 = SsdMobilenetV11Metadata1.newInstance(this);

            // Creates inputs for reference.
            TensorImage image = TensorImage.fromBitmap(resizeImage);


            // Runs model inference and gets result.
            SsdMobilenetV11Metadata1.Outputs outputs2 = ssdMobilenetV11Metadata1.process(image);
            TensorBuffer locations = outputs2.getLocationsAsTensorBuffer();

            findDetection(locations.getFloatArray(), outputFeature0.getFloatArray(), resizeImage);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    private void sortArray(float[] arr){
        float[] temp = new float[arr.length];
        System.arraycopy(arr, 0, temp, 0, arr.length);

        ArrayList<String> sortedArray = new ArrayList<>();
        double percentage;
        int index;
        for (int i = 0; i < 100; i++) {
            index = getMax(temp);
            percentage = (temp[index]/ 255) *100;
            String result = String.format(Locale.getDefault(), "%.2f", percentage);
            temp[index] = -256;
            sortedArray.add(arrLabel.get(index) + " " + result+ "%");
        }
        setRecyclerView(sortedArray);
    }

    private void setRecyclerView(ArrayList<String> array) {
        MatchAdapter matchAdapter = new MatchAdapter(this);
        matchAdapter.setArrayList(array);
        Main_RecyclerView.setLayoutManager(new LinearLayoutManager(this));
        Main_RecyclerView.setAdapter(matchAdapter);
        Main_RecyclerView.setBackgroundColor(getResources().getColor(R.color.black));
    }

    private int getMax(float[] arr){
        int index = 0;
        float max = -1;

        for (int i = 0; i < arr.length; i++) {
            if (max < arr[i]){
                max = arr[i];
                index = i;
            }
        }
        return index;
    }

    private void SelectImage(){
        Intent photoSelect = new Intent(Intent.ACTION_PICK);
        photoSelect.setType("image/*");
        startActivityForResult(photoSelect, RESULT_LOAD_IMAGE);
    }
    private void findViews() {
        Main_IMG = findViewById(R.id.Main_IMG);
        Main_RecyclerView = findViewById(R.id.Main_RecyclerView);
        Main_BTN_select = findViewById(R.id.Main_BTN_select);
        Main_BTN_detect = findViewById(R.id.Main_BTN_detect);
    }

    private void findDetection(float[] locations, float[] probability, Bitmap photo) {
        final float TEXT_SIZE_DIP = 4;
        final int DIM_PHOTO = 300;
        Paint myRectangle = new Paint();
        myRectangle.setColor(Color.RED);
        myRectangle.setStyle(Paint.Style.STROKE);
        myRectangle.setStrokeWidth(2);

        Paint myTitle = new Paint();
        float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        myTitle.setTextSize(textSizePx + 6);
        myTitle.setColor(Color.BLACK);

        Canvas c = new Canvas(photo);

        float left,  top,  right,  bottom ;

        for (int i = 0; i < locations.length; i = i + 4) {
            if ((probability[getMax(probability)]/ 255) * 100 > 50) {

                float percentage = (probability[getMax(probability)]/ 255) * 100;
                String result = String.format(Locale.getDefault(), "%.2f", percentage);
                top = locations[i] * DIM_PHOTO ;
                left = locations[i + 1] * DIM_PHOTO ;
                bottom = locations[i + 2] * DIM_PHOTO ;
                right = locations[i + 3] * DIM_PHOTO ;

                c.drawText(arrLabel.get(getMax(probability)) + " " + result + "%", left, top, myTitle);
                c.drawRect(left, top+5, right, bottom , myRectangle);
                probability[getMax(probability)] = -256;
            }
        }
        Main_IMG.setImageBitmap(photo);
    }
}
