package com.aman.uploadpdf001;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private String CategoryName, Description , Price, Pname, saveCurrentDate, saveCurrentTime;
    private Button AddNewProduct;
    private ImageView InputProductImage;
    private EditText InputProductName, InputProductDescription, InputProductPrize;
    private static final int GalleryPick = 1;
    private Uri PdfUri;
    private String ProductRandomKey, DownloadPdfUrl;
    private StorageReference ProductImagesRef;
    private DatabaseReference productRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ProductImagesRef = FirebaseStorage.getInstance().getReference().child("PDF");
        productRef = FirebaseDatabase.getInstance().getReference().child("PDF");


        InputProductName = (EditText) findViewById(R.id.product_name);
        InputProductDescription = (EditText) findViewById(R.id.product_description);
        InputProductPrize = (EditText) findViewById(R.id.product_price);
        InputProductImage = (ImageView) findViewById(R.id.select_product_image);
        AddNewProduct = (Button) findViewById(R.id.add_new_product);



        InputProductImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                OpenGallery();
            }
        });

        AddNewProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                validateProductData();
            }
        });

    }

    private void OpenGallery()
    {
        Intent galleryIntent =new Intent();
        galleryIntent.setType("application/pdf");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(galleryIntent, GalleryPick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == GalleryPick && resultCode ==RESULT_OK && data !=null )
        {
            PdfUri = data.getData();

        }
        else
        {
            Toast.makeText(MainActivity.this, "Failed ", Toast.LENGTH_SHORT).show();
        }

    }

    private void validateProductData()
    {
        Description = InputProductDescription.getText().toString();
        Price = InputProductPrize.getText().toString();
        Pname = InputProductName.getText().toString();


        if (PdfUri==null)
        {
            Toast.makeText(this, "Select pdf", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Description))
        {
            Toast.makeText(this, "Select pdf", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Price))
        {
            Toast.makeText(this, "Price", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Pname))
        {
            Toast.makeText(this, "name", Toast.LENGTH_SHORT).show();
        }

        else
        {
            StoreProductDetail();
        }

    }

    private void StoreProductDetail()
    {


        Calendar calendar= Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, YYYY");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        ProductRandomKey = saveCurrentDate + saveCurrentTime;

        final StorageReference filepath = ProductImagesRef.child(PdfUri.getLastPathSegment() + ProductRandomKey + ".pdf");

        final UploadTask uploadtask = filepath.putFile(PdfUri);

        uploadtask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                String message = e.toString();
                Toast.makeText(MainActivity.this, "Error "+ message, Toast.LENGTH_SHORT).show();

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "Pdf Uploaded succesFully", Toast.LENGTH_SHORT).show();

                Task<Uri> urlTask = uploadtask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception
                    {
                        if (!task.isSuccessful())
                        {
                            throw task.getException();
                        }

                        DownloadPdfUrl = filepath.getDownloadUrl().toString();
                        return filepath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task)
                    {
                        if (task.isSuccessful())
                        {
                            DownloadPdfUrl = task.getResult().toString();
                            Toast.makeText(MainActivity.this, "Successs Url", Toast.LENGTH_SHORT).show();

                            SaveProductInfoToDataBase();
                        }
                    }
                });
            }
        });
    }


    private void SaveProductInfoToDataBase()
    {
        HashMap<String, Object> productMap = new HashMap<>();
        productMap.put("pid",ProductRandomKey);
        productMap.put("date",saveCurrentDate);
        productMap.put("time",saveCurrentTime);
        productMap.put("description",Description);
        productMap.put("image",DownloadPdfUrl);
        productMap.put("category",CategoryName);
        productMap.put("pname",Pname);

        productRef.child(ProductRandomKey).updateChildren(productMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Final finish", Toast.LENGTH_SHORT).show();


                            Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                            startActivity(intent);

                        }
                        else {

                            String Message= task.getException().toString();
                            Toast.makeText(MainActivity.this, "Faildes: "+ Message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }


}
