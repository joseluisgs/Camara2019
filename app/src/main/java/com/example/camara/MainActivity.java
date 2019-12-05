package com.example.camara;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

import static android.media.MediaRecorder.VideoSource.CAMERA;

public class MainActivity extends AppCompatActivity {

    // Constantes
    private static final int GALERIA = 1 ;
    private static final int CAMARA = 2 ;

    // Si vamos a operar en modo público o privado
    private static final boolean PRIVADO = true;

    // Directorio para salvar las cosas
    private static final String IMAGE_DIRECTORY = "/pepito";
    Uri photoURI;

    private Button btnAccionMain;
    private ImageView ivMain;
    private TextView tvMain;
    private TextView tvModoMain;

    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.ivMain = (ImageView) findViewById(R.id.ivMain);
        this.btnAccionMain= (Button) findViewById(R.id.btnAccionMain);
        this.tvMain = (TextView) findViewById(R.id.tvMain);
        this.tvModoMain = (TextView) findViewById(R.id.tvModoMain);

        if(PRIVADO){
            this.tvModoMain.setText("MODO PRIVADO");
        }else{
            this.tvModoMain.setText("MODO PUBLICO");
        }


        // Pedimos los permisos
        pedirMultiplesPermisos();

        btnAccionMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoFoto();
            }
        });


    }

    private void mostrarDialogoFoto(){
        AlertDialog.Builder fotoDialogo= new AlertDialog.Builder(this);
        fotoDialogo.setTitle("Seleccionar Acción");
        String[] fotoDialogoItems = {
                "Seleccionar fotografía de galería",
                "Capturar fotografía desde la cámara" };
        fotoDialogo.setItems(fotoDialogoItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                elegirFotoGaleria();
                                break;
                            case 1:
                                tomarFotoCamara();
                                break;
                        }
                    }
                });
        fotoDialogo.show();
    }

    // Llamamos al intent de la galeria
    public void elegirFotoGaleria() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALERIA);
    }

    //Llamamos al intent de la camara
    // https://developer.android.com/training/camera/photobasics.html#TaskPath
    private void tomarFotoCamara() {
        // Si queremos hacer uso de fotos en aklta calidad
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        // Eso para alta o baja
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        // Esto para alta calidad
        photoURI = Uri.fromFile(this.crearFichero());
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);

        // Esto para alta y baja
        startActivityForResult(intent, CAMARA);
    }

    // Siempre se ejecuta al realizar las accion
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("FOTO", "Opción::--->" + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }

        if (requestCode == GALERIA) {
            Log.d("FOTO", "Entramos en Galería");
            if (data != null) {
                // Obtenemos su URI con su dirección temporal
                Uri contentURI = data.getData();
                try {
                    // Obtenemos el bitmap de su almacenamiento externo
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    path = salvarImagen(bitmap);
                    Toast.makeText(MainActivity.this, "¡Foto salvada!", Toast.LENGTH_SHORT).show();
                    this.ivMain.setImageBitmap(bitmap);

                    // Mostramos el path
                    this.tvMain.setText(this.path);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "¡Fallo Galeria!", Toast.LENGTH_SHORT).show();
                }
            }

        } else if (requestCode == CAMARA) {
            Log.d("FOTO", "Entramos en Camara");
            // Cogemos la imagen, pero podemos coger la imagen o su modo en baja calidad (thumbnail
            Bitmap thumbnail = null;
            try {
                // Esta línea para baja
                //thumbnail = (Bitmap) data.getExtras().get("data");
                // Esto para alta
                thumbnail = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);

                // salvamos
                path = salvarImagen(thumbnail); //  photoURI.getPath(); Podríamos poner esto, pero vamos a salvarla comprimida y borramos la no comprimida (por gusto)

                this.ivMain.setImageBitmap(thumbnail);

                // Mostramos el path
                this.tvMain.setText(path);

                // Borramos el fichero de la URI
                borrarFichero(photoURI.getPath());

                Toast.makeText(MainActivity.this, "¡Foto Salvada!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "¡Fallo Camara!", Toast.LENGTH_SHORT).show();
            }

        }

        if(PRIVADO){
            // Copiamos de la publica a la privada
            File ficheroDestino =  new File(getBaseContext().getFilesDir() ,crearNombreFichero());
            File ficheroOrigen = new File(path);
            Log.d("FOTO", "Copiamos los ficheros");
            try {
                // Copiamos
                copiarFicheros(ficheroOrigen,ficheroDestino);
                // Borramos
                borrarFichero(path);
                // Ponemos el nuevo path
               path = ficheroDestino.getPath();
                this.tvMain.setText(path);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "¡Fallo al pasar a memoria interna!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void borrarFichero(String path) {
        // Borramos la foto de alta calidad
        File fdelete = new File(path);
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.d("FOTO", "Foto borrada::--->" + path);
            } else {
                Log.d("FOTO", "Foto NO borrada::--->" +path);
            }
        }
    }

    public File crearFichero(){
        // Nombre del fichero
        String nombre = crearNombreFichero();
        return salvarFicheroPublico(nombre);
    }

    private String crearNombreFichero() {
        return Calendar.getInstance().getTimeInMillis() + ".jpg";
    }

    private File salvarFicheroPublico(String nombre) {
        // Vamos a obtener los datos de almacenamiento externo
        File dirFotos = new File(Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // Si no existe el directorio, lo creamos solo si es publico
        if (!dirFotos.exists()) {
            dirFotos.mkdirs();
        }

        // Vamos a crear el fichero con la fecha
        try {
            File f = new File(dirFotos, nombre);
            f.createNewFile();
            return f;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }


    // Función para salvar una imagem
    public String salvarImagen(Bitmap myBitmap) {
        // Comprimimos la imagen
        ByteArrayOutputStream bytes = comprimirImagen(myBitmap);

        // Creamos el nombre del fichero
        File f = crearFichero();

            // Escribimos en el el Bitmat en jpg creado
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("FOTO", "Fichero salvado::--->" + f.getAbsolutePath());

            // Devolvemos el path
            return f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "¡Fallo Salvar Fichero!", Toast.LENGTH_SHORT).show();
        }


        return "";
    }

    private ByteArrayOutputStream comprimirImagen(Bitmap myBitmap) {
        // Stream de binario
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // Seleccionamos la calidad y la trasformamos y comprimimos
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        return bytes;
    }




    // Funcion para programar los permisos usando Dexter
    private void pedirMultiplesPermisos(){
        // Indicamos el permisos y el manejador de eventos de los mismos
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // ccomprbamos si tenemos los permisos de todos ellos
                        if (report.areAllPermissionsGranted()) {
                            Toast.makeText(getApplicationContext(), "¡Todos los permisos concedidos!", Toast.LENGTH_SHORT).show();
                        }

                        // comprobamos si hay un permiso que no tenemos concedido ya sea temporal o permanentemente
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // abrimos un diálogo a los permisos
                            //openSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Existe errores! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }


    private boolean copiarFicheros(File origen,File destino)throws IOException{
        if(origen.getAbsolutePath().toString().equals(destino.getAbsolutePath().toString())){

            return true;

        }else{
            InputStream is=new FileInputStream(origen);
            OutputStream os=new FileOutputStream(destino);
            byte[] buff=new byte[1024];
            int len;
            while((len=is.read(buff))>0){
                os.write(buff,0,len);
            }
            is.close();
            os.close();
        }
        return true;
    }
}
