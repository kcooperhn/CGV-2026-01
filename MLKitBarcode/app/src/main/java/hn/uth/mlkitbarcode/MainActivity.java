package hn.uth.mlkitbarcode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

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

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final int REQUEST_PERMISSIONS = 200;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private String directorioImagen;

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

        binding.contentMain.imgBarcode.setVisibility(View.INVISIBLE);
        binding.contentMain.txtResultado.setText("Sin escaneo realizado... selecciona una foto de la galería o toma una con la cámara para iniciar.");

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
                }else{
                    Log.d("IMAGEN_CAMARA","Imagen no encontrada");
                    Bundle extras = data.getExtras();
                    if(extras != null){
                        Bitmap imagen = (Bitmap) extras.get("data");
                        binding.contentMain.imgBarcode.setVisibility(View.VISIBLE);
                        binding.contentMain.imgBarcode.setImageBitmap(imagen);
                    }
                }
                binding.contentMain.txtResultado.setText("Foto recuperada, esperando escaneo de código de barras...");
                Log.d("IMAGEN_CAMARA","Ejecución de escaneo enviada");
                ejecutarEscaneoCodidoBarras();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ejecutarEscaneoCodidoBarras() {





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