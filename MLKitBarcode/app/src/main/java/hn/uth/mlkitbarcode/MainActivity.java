package hn.uth.mlkitbarcode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import hn.uth.mlkitbarcode.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final int REQUEST_PERMISSIONS = 200;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private String directorioImagen;
    private Bitmap imagenSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        binding.fabCam.setOnClickListener(view -> {
            if(checkAndRequestPermissions()){
                Log.d("IMAGEN_CAMARA","Permisos aceptados");
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d("IMAGEN_CAMARA","Intent camara resuelto");
                    File archivoImagen = null;
                    try{
                        archivoImagen = createImageFile();
                    }catch(Exception error){
                        error.printStackTrace();
                        Log.d("IMAGEN_CAMARA","Error al generar archivo de imagen");
                    }
                    if(archivoImagen != null){
                        Uri fotoUri = FileProvider.getUriForFile(getApplicationContext(), "hn.uth.mlkitbarcode.fileprovider", archivoImagen);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
                    }

                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    Log.d("IMAGEN_CAMARA","Activity camara iniciada");
                }else{
                    Log.d("IMAGEN_CAMARA","No se encontró app para manejo de camara");
                    Snackbar.make(binding.getRoot(), "No se encontró app para manejo de camara", Snackbar.LENGTH_LONG).show();
                }
            }else{
                Snackbar.make(binding.getRoot(), "Favor otorgar permisos", Snackbar.LENGTH_LONG).show();
            }
        });

        binding.contentMain.btnSeleccionarGaleria.setOnClickListener(v -> {
            abrirGaleria();
        });

        binding.contentMain.btnEjecutarEscaneo.setOnClickListener(v -> {
            ejecutarEscaneoCodidoBarras();
        });

        binding.contentMain.imgBarcode.setVisibility(View.INVISIBLE);
        binding.contentMain.txtResultado.setText("Sin escaneo realizado... selecciona una foto de la galería o toma una con la cámara para iniciar.");

    }

    private void abrirGaleria() {
        if(checkAndRequestPermissions()){
            Intent galeriaIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galeriaIntent.setType("image/*");

            Intent selectorIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            selectorIntent.setType("image/*");

            Intent menuSelection = Intent.createChooser(galeriaIntent, "Seleccione una Imagen");
            menuSelection.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{selectorIntent});

            startActivityForResult(menuSelection, REQUEST_PICK_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("IMAGEN_CAMARA","Datos de la camara recibidos");
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            Log.d("IMAGEN_CAMARA","Datos contienen imagen (result ok)");

            if(!"".equals(directorioImagen) && directorioImagen != null){
                File imgFile = new File(directorioImagen);
                if(imgFile.exists()){
                    Log.d("IMAGEN_CAMARA","Imagen encontrada");
                    binding.contentMain.imgBarcode.setVisibility(View.VISIBLE);
                    binding.contentMain.imgBarcode.setImageURI(Uri.fromFile(imgFile));
                    try {
                        InputImage imagen = InputImage.fromFilePath(this, Uri.fromFile(imgFile));
                        imagenSeleccionada = imagen.getBitmapInternal();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }else{
                    Log.d("IMAGEN_CAMARA","Imagen no encontrada");
                    Bundle extras = data.getExtras();
                    if(extras != null){
                        Bitmap imagen = (Bitmap) extras.get("data");
                        binding.contentMain.imgBarcode.setVisibility(View.VISIBLE);
                        binding.contentMain.imgBarcode.setImageBitmap(imagen);

                        imagenSeleccionada = imagen;
                    }
                }
                binding.contentMain.txtResultado.setText("Foto recuperada, esperando escaneo de código de barras...");
                Log.d("IMAGEN_CAMARA","Ejecución de escaneo enviada");
                ejecutarEscaneoCodidoBarras();
            }
        }

        if(requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK){
            //RESULTADO VIENE DE LA GALERIA
            if(data == null || data.getData() == null){
                Log.d("IMAGEN_GALERIA","No se seleccionó ninguna foto de la galería");
                Snackbar.make(binding.getRoot(), "No se seleccionó ninguna foto de la galería", Snackbar.LENGTH_LONG).show();
            }else{
                Log.d("IMAGEN_GALERIA","Se seleccionó una foto de la galería");
                Uri imagenUri = data.getData();
                try{
                    InputImage imagen = InputImage.fromFilePath(this, imagenUri);

                    imagenSeleccionada = imagen.getBitmapInternal();
                    binding.contentMain.imgBarcode.setVisibility(View.VISIBLE);
                    binding.contentMain.imgBarcode.setImageBitmap(imagenSeleccionada);
                    binding.contentMain.txtResultado.setText("Foto recuperada de la galería, esperando escaneo de código de barras...");
                    Snackbar.make(binding.getRoot(), "Imagen recuperada de la galería", Snackbar.LENGTH_LONG).show();
                    ejecutarEscaneoCodidoBarras();
                }catch(Exception error){
                    error.printStackTrace();
                    Log.d("IMAGEN_GALERIA","Error al recuperar imagen de la galería");
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ejecutarEscaneoCodidoBarras() {

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .enableAllPotentialBarcodes()
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        String resultadoEscaneo="Escaneo ejecutado...\n\n";
        int rotation  = 0;
        InputImage image = InputImage.fromBitmap(imagenSeleccionada, rotation);

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    Log.d("BARCODE","Escaneo de código de barras realizado");
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue()+"\n\n";
                        binding.contentMain.txtResultado.setText(resultadoEscaneo+rawValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("BARCODE","Escaneo de código de barras fallido, no se detectaron códigos");
                    binding.contentMain.txtResultado.setText(resultadoEscaneo+"No se detectaron códigos de barras en la imagen");
                    Snackbar.make(binding.getRoot(), "No se lograron recuperar códigos de barra legibles", Snackbar.LENGTH_LONG).show();
                });

    }

    private boolean checkAndRequestPermissions() {
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        Log.d("IMAGEN_CAMARA","Evaluando permisos");

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS);
            Log.d("IMAGEN_CAMARA","Permiso rechazado");
            return false;
        }
        Log.d("IMAGEN_CAMARA","Permiso concedido");
        return true;
    }

    private File createImageFile() throws IOException {
        String fechaHoy = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nombreArchivo = "JPEG_"+fechaHoy+"_";
        File directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imagen = File.createTempFile(nombreArchivo, ".jpg", directorio);
        directorioImagen = imagen.getAbsolutePath();

        return imagen;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}