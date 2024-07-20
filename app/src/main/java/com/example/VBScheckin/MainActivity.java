package com.example.VBScheckin;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.VBScheckin.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean isProcessingQRCode = false; // Flag to track QR code processing state
    private ProcessCameraProvider cameraProvider;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showCameraPreview();
                } else {
                    Toast.makeText(MainActivity.this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    private void setResult(String contents) {
        if (contents.startsWith(getString(R.string.jotform_url))) {
            int lastIndex = contents.lastIndexOf('/');
            String uniqueID = contents.substring(lastIndex + 1);
            stopCameraAndShowResult(uniqueID);
        }
        else{
            Toast.makeText(this, "Not a valid QR Code",Toast.LENGTH_SHORT).show();
            isProcessingQRCode = false;
        }
    }

    private void stopCameraAndShowResult(String result) {
        // Stop the camera
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        // Hide the camera preview
        binding.viewFinder.setVisibility(View.GONE);

        // Display the result and show the "Go" button
        binding.textResult.setText(result);
        binding.textResult.setVisibility(View.VISIBLE);
        binding.fabGo.setVisibility(View.VISIBLE);
    }

    private void fetchData(final Context context, String submission_ID) {
        String url = String.format("https://sheetdb.io/api/v1/{SHEETSDB KEY}/search?sheet=2024+VBS+Master+List&Submission%%20ID=%s", submission_ID);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                String jsonString = "{\"values\":" + response + "}";
                Intent intent = new Intent(context, MainActivity2.class);
                intent.putExtra("jsonString", jsonString);
                startActivity(intent);
                resetToDefaultState();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                resetToDefaultState();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initBinding();
        initViews();
    }

    private void initViews() {
        binding.fab.setOnClickListener(v -> {
            checkPermissionAndShowActivity(this);
        });

        binding.fabGo.setOnClickListener(v -> {
            fetchData(this, binding.textResult.getText().toString());
        });
    }

    private void checkPermissionAndShowActivity(Context context) {
        if(ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            showCameraPreview();
        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show();
        }else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void initBinding() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void showCameraPreview() {
        binding.fab.setVisibility(View.GONE); // Hide the original FAB
        binding.viewFinder.setVisibility(View.VISIBLE); // Show the camera preview
        startCamera();
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QRCodeAnalyzer());

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }

    private class QRCodeAnalyzer implements ImageAnalysis.Analyzer {
        @OptIn(markerClass = ExperimentalGetImage.class)
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (isProcessingQRCode) {
                imageProxy.close(); // Ignore the frame if a QR code is already being processed a fetch is in progress
                return;
            }

            @androidx.camera.core.ExperimentalGetImage
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();
                BarcodeScanner scanner = BarcodeScanning.getClient(options);
                isProcessingQRCode = true; // Set the flag to indicate a QR code is being processed

                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            for (Barcode barcode : barcodes) {
                                String rawValue = barcode.getRawValue();
                                if (rawValue != null) {
                                    setResult(rawValue);
                                    break; // Only process the first barcode found
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Barcode scanning failed: " + e.getMessage());
                            isProcessingQRCode = false; // Reset the flag on failure
                        })
                        .addOnCompleteListener(task -> {
                            imageProxy.close();
                            isProcessingQRCode = false; // Reset the flag after completion
                        });
            } else {
                imageProxy.close();
            }
        }
    }

    private void resetToDefaultState() {
        // Reset the UI elements to their default state
        binding.viewFinder.setVisibility(View.GONE);
        binding.textResult.setVisibility(View.GONE);
        binding.fabGo.setVisibility(View.GONE);
        binding.fab.setVisibility(View.VISIBLE);

        // Reset the flags
        isProcessingQRCode = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Log.d(TAG, "options created");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        if (item.getItemId() == R.id.options) {
            openOptions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openOptions() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}