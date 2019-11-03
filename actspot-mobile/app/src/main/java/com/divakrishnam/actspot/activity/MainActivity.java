package com.divakrishnam.actspot.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.divakrishnam.actspot.R;
import com.divakrishnam.actspot.api.APIService;
import com.divakrishnam.actspot.model.Result;
import com.divakrishnam.actspot.sharedpref.SharedPrefManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import id.zelory.compressor.Compressor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener{

    private TextView tvName;
    private TextView tvLocation;
    private EditText etAddress;
    private ImageView ivPhoto;
    private Button btnCapture;
    private Button btnSend;
    private Button btnLogout;

    private String deviceid = "";
    private String latitude = "";
    private String longtitude = "";
    private String name = "";
    private String time = "";
    private Calendar calendar;
    private SimpleDateFormat simpledateformat;
    private LocationManager locationManager;

    public static final int REQUEST_IMAGE = 100;
    public static final int REQUEST_PERMISSION = 200;
    private String imageFilePath = "";

    private TelephonyManager mTelephonyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (!SharedPrefManager.getInstance(this).isLoggedIn()) {
            finish();
            startActivity(new Intent(this, SignInActivity.class));
        }else{
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                        300);
            }
        }

        tvName = findViewById(R.id.tv_name);
        tvLocation = findViewById(R.id.tv_location);
        etAddress = findViewById(R.id.et_address);
        ivPhoto = findViewById(R.id.iv_photo);
        btnCapture = findViewById(R.id.btn_capture);
        btnSend = findViewById(R.id.btn_send);
        btnLogout = findViewById(R.id.btn_logout);

        name = SharedPrefManager.getInstance(this).getUser().getName();
        tvName.setText(name);

        getLocation();
        getDeviceImei();

        btnCapture.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
    }



    private void capturePhoto() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(pictureIntent, REQUEST_IMAGE);
        }
    }

    private void sendData() {

        getLocation();

        String id_user = SharedPrefManager.getInstance(this).getUser().getUsername();
        calendar = Calendar.getInstance();
        simpledateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        time = simpledateformat.format(calendar.getTime());

        File image = new File(imageFilePath);
        File file = null;
        try {
            file = new Compressor(this).compressToFile(image);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(etAddress.getText().toString())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        APIService service = retrofit.create(APIService.class);

        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part mPicture = MultipartBody.Part.createFormData("picture", file.getName(), requestFile);

        RequestBody mIdUser = RequestBody.create(MediaType.parse("text/plain"), id_user);
        RequestBody mLatitude = RequestBody.create(MediaType.parse("text/plain"), latitude);
        RequestBody mLongtitude = RequestBody.create(MediaType.parse("text/plain"), longtitude);
        RequestBody mTime = RequestBody.create(MediaType.parse("text/plain"), time);
        RequestBody mImei = RequestBody.create(MediaType.parse("text/plain"), deviceid);

        Call<Result> call = service.insertData(mIdUser, mLatitude, mLongtitude, mTime, mImei, mPicture);

        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                String status = response.body().getStatus();
                String message = response.body().getMessage();
                ivPhoto.setImageBitmap(null);
                Toast.makeText(MainActivity.this, status+"-"+message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Jaringan Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        SharedPrefManager.getInstance(this).logout();
        finish();
        startActivity(new Intent(this, SignInActivity.class));
    }

    private void getLocation(){
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getDeviceImei() {
        try {
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            deviceid = mTelephonyManager.getDeviceId();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = String.valueOf(location.getLatitude());
        longtitude = String.valueOf(location.getLongitude());

        tvLocation.setText(String.format("Latitude: %s\nLongitude: %s", location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(MainActivity.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thanks for granting Permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                ivPhoto.setImageURI(Uri.parse(imageFilePath));
                btnSend.setEnabled(true);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "You cancelled the operation", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        imageFilePath = image.getAbsolutePath();



        return image;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_capture:
                capturePhoto();
                break;
            case R.id.btn_send:
                sendData();
                break;
            case R.id.btn_logout:
                logout();
                break;
        }
    }
}
