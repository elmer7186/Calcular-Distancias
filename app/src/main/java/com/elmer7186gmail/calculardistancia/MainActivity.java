package com.elmer7186gmail.calculardistancia;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Elmer on 11/11/2017.
 */
public class MainActivity extends Activity implements SensorEventListener {

    private static final int LECTURA_PENDIENTE = 0;
    private static final int LECTURA_REALIZADA = 1;
    private static final int MY_REQUEST_CODE = 1;

    private Camera mCamera;
    private CameraPreview mPreview;

    private SensorManager sensorManager = null;
    private Sensor sensorAcelerometro = null;

    private EditText decimalAltura = null;
    private TextView textViewCalculo = null;
    private Button buttonCalculo = null;

    private int modoCalculo = LECTURA_PENDIENTE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        decimalAltura = (EditText) findViewById(R.id.decimal_altura);
        decimalAltura.setText("1.5");
        textViewCalculo = (TextView) findViewById(R.id.textView_calculo);
        buttonCalculo = (Button) findViewById(R.id.button_calculo);

        pedirPermiso();
        inicializarAcelerometro();

    }

    public void inicializarAcelerometro() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (sensorAcelerometro == null) {
            Toast.makeText(getApplicationContext(), "No hay Sensor de movimiento", Toast.LENGTH_SHORT).show();
        }
    }

    public void pedirPermiso() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_REQUEST_CODE);
        } else {
            mCamera = getCameraInstance();

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {

        Camera c = null;
        try {
            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK); // attempt to get a Camera instance
        } catch (Exception e) {
            throw e;
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == MY_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to use camera
            } else {
                // Your app will not have this permission. Turn off all functions
                // that require this permission or it will force close like your
                // original question

                // Create an instance of Camera
                mCamera = getCameraInstance();

                // Create our Preview view and set it as the content of our activity.
                mPreview = new CameraPreview(this, mCamera);
                FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
                preview.addView(mPreview);
            }
        }
    }

    public void capturarMedida(View v) {
        switch (modoCalculo) {
            case LECTURA_PENDIENTE:
                mCamera.stopPreview();
                buttonCalculo.setText("Realizar Otra Lectura");
                modoCalculo = LECTURA_REALIZADA;
                break;
            case LECTURA_REALIZADA:
                mCamera.startPreview();
                buttonCalculo.setText("Calcular");
                modoCalculo = LECTURA_PENDIENTE;
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent arg0) {
        synchronized (this) {
            if (modoCalculo == LECTURA_PENDIENTE) {
                float[] masData;
                float x;
                float y;
                float z;
                switch (arg0.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        masData = arg0.values;
                        x = masData[0];
                        y = masData[1];
                        z = masData[2];

                        if (y < 0 || z < 0) {
                            textViewCalculo.setText("Lectura no valida.");
                        } else {
                            float grados = (90 / 10) * y;
                            String valor = Float.toString(grados);
                            double radianes = Math.toRadians(Double.parseDouble(valor));
                            double tangente = Math.tan(radianes);
                            String alturaTexto = decimalAltura.getText().toString();
                            double altura;
                            try {
                                altura = Double.parseDouble(alturaTexto);
                            } catch (Exception e) {
                                return;
                            }
                            double distancia = tangente * altura;

                            //recortar decimales de resultados
                            DecimalFormat df = new DecimalFormat("#.00");
                            String resultado = df.format(distancia);
                            textViewCalculo.setText("Distancia: " + resultado + " m");
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.size() > 0) {
            sm.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onStop() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.unregisterListener(this);
        super.onStop();
    }
}
