package com.atleastitworks.proximity2hc05;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Process;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Texto de estado
    TextView texto_estado, texto_input;
    // Lista a mostrar
    ListView devicelist;
    // Botones
    Button boton_act, boton_disconect;
    // Swich
    Switch switch_back;
    // Estado
    ImageView iv;

    // Background
    private boolean keepMinimized = false;

    // Sensor de proximidad
    private SensorManager sensorManager = null;
    private Sensor mProximity = null;
    private float proximidad_act = 0;
    private float proximidad_ant = 0;

    // ID del request de bluetooth
    int REQUEST_ENABLE_BT=1;
    // Bluetooth
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private boolean isBtConnecting = false;
    String address = null;
    // UUID del Serial Port Profile (SPP):
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Traemos el texto de estado
        texto_estado = (TextView) findViewById(R.id.text_estado);
        texto_input = (TextView) findViewById(R.id.text_estado2);
        // Traemos la lista
        devicelist = (ListView) findViewById(R.id.lista_bt);
        // Traemos boton
        boton_act = (Button) findViewById(R.id.button_Act);
        boton_disconect = (Button) findViewById(R.id.button_disconect);
        // el indicador...
        iv = findViewById(R.id.imageView);
        // venga el switch
        switch_back = findViewById(R.id.switch1);
        switch_back.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Context context = getApplicationContext();
                CharSequence text;

                if (isChecked) {
                    keepMinimized = true;
                    text = "La app seguirá funcionando minimizada.";
                } else {
                    keepMinimized = false;
                    text = "La app se suspendera al minimizar.";
                }
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
        //------------------------------------------------------------------------------------------
        //------------------------------------ SENSOR ----------------------------------------------
        //------------------------------------------------------------------------------------------
        // Primero vamos a activar el sensor de proximidad
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Nos traemos la lista
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        // Pedimos el sensor de proximidad
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (mProximity != null)
        {
            // Habemus sensorem!
            // Registramos
            sensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else
        {
            Toast toast = Toast.makeText(this, "No proximity sensor (wtf?!). Terminating...", Toast.LENGTH_LONG);
            toast.show();
            finish();
            return;
        }




        //------------------------------------------------------------------------------------------
        //------------------------------------ BLUETOOTH -------------------------------------------
        //------------------------------------------------------------------------------------------
        // Vemos a hacerlo bien...
        // Nos fijamos que tenga bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (myBluetooth == null) {
            // Si no soporta dienteazul nos vamos...
            Toast toast = Toast.makeText(this, "No bluetooth available. Terminating...", Toast.LENGTH_LONG);
            toast.show();
            finish();
            return;
        }

        // Prendemos el bluetooth (en caso de estar apagado)
        if (!myBluetooth.isEnabled()) {
            // Es un intent porque vamos a llamar al framework (van a ver otra ventanita)
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        // Si llegamos acá, vamos a ver si el HC-05 esta apareado con nuestro dispositivo
        // Pedimos la lista de dispositivos apareados
        get_btDevicesList();
        // Si acabamos de prender el bluetooth la lista va a estar vacia, y hay que actualizar manualmente
        boton_act.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get_btDevicesList();
            }
        });

        // La lista en si va a manejar la conexion, cuando le hagamos click (ver el myListClickListener)

        boton_disconect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });




        //------------------------------------------------------------------------------------------
        //------------------------------------ WORK THREAD -----------------------------------------
        //------------------------------------------------------------------------------------------
        // Ahora vamos a ejecutar un thread que va a leer el valor de proximidad y enviar una señal
        // al arduino en caso de que el valor de proximidad sea menor a un valor dado.
        runThread();
        //------------------------------------------------------------------------------------------



    }


    private void runThread() {

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run()
                            {
                                if (isBtConnected)
                                {
                                    if(proximidad_act != proximidad_ant)
                                    {
                                        // Si es menor a 1cm envio 1, sino 0
                                        if (proximidad_act < 1.0) sendBT("1");
                                        else sendBT("0");

                                        // Actualizo
                                        proximidad_ant = proximidad_act;
                                    }

                                    // Leo si mandaron algo

                                    String input_msg = getBT ();
                                    if (input_msg.length() > 0)
                                    {
                                        texto_input.setText(input_msg);
                                        if (input_msg.equals("on")) iv.setImageResource(android.R.drawable.presence_online);
                                        else if (input_msg.equals("off")) iv.setImageResource(android.R.drawable.presence_invisible);
                                        else iv.setImageResource(android.R.drawable.presence_offline);
                                    }
                                }
                            }
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void sendBT ( String mensaje ) {
        if ( btSocket != null ) {
            try {
                btSocket.getOutputStream().write(mensaje.getBytes());
            } catch (IOException e) {
                Toast toast = Toast.makeText(this, "Error al enviar mensaje: "+mensaje, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private String getBT () {
        String mensaje = "";
        if ( btSocket != null ) {
            try {
                int num_datos = btSocket.getInputStream().available();
                if( num_datos > 0 )
                {
                    byte[] b = new byte[num_datos];
                    btSocket.getInputStream().read(b, 0, num_datos);
                    mensaje = new String(b, StandardCharsets.UTF_8);

                }

            } catch (IOException e) {
                Toast toast = Toast.makeText(this, "Error al recibir mensaje.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        return mensaje;
    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                btSocket.close();
                texto_estado.setText("Desconectado.");
                isBtConnected = false;
            } catch(IOException e) {
                Toast toast = Toast.makeText(this, "Error al desconectar el dispositivo.", Toast.LENGTH_LONG);
                toast.show();
            }
        }

    }


    private void get_btDevicesList()
    {
        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();
        // Si hay alguno, armamos la lista
        if (pairedDevices.size() > 0)
        {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices)
            {
                list.add(device.getName().toString() + "\n" + device.getAddress().toString());
            }

            // LLenamos la lista
            final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
            devicelist.setAdapter(adapter);
            devicelist.setOnItemClickListener(myListClickListener);

        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }
    }


    // Obtenemos el resultado de la actividad que ejecutamos con el intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Checkeamos el codigo (acá retornan todos los intent)
        if(requestCode==REQUEST_ENABLE_BT)
        {
            // Si dijo que no, chau
            if (resultCode==0)
            {
                Toast toast = Toast.makeText(this, "Bluetooth is required. Terminating...", Toast.LENGTH_LONG);
                toast.show();
                finish();
                return;
            }

        }
    }


    // Esto va a "escuchar" los clicks que hagmos en la lista de dispositivos bluetooth
    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // Guardo la MAC del dispositivo seleccionado
            String info = ((TextView) view).getText().toString();
            address = info.substring(info.length()-17);

            // Muestro la seleccion
            texto_estado.setText("Seleccionado: "+info);

            // Conecto
            if (!isBtConnecting)
                new ConnectBT().execute();

        }


    };

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        // Checkeamos que sea el de proximidad (aunque no puede ser otro, es a modo ejemplo)
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY)
        {
            proximidad_act = sensorEvent.values[0];
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Nada...
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (keepMinimized == false)
        {
            // Registramos el sensor
            if (sensorManager != null)
                sensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

            // Conectamos el bluetooth
            if (address != null)
                new ConnectBT().execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (keepMinimized == false)
        {
            // Des-registramos el sensor
            if (sensorManager != null)
                sensorManager.unregisterListener(this);

            // Desconectamos el bluetooth
            if (address != null)
                Disconnect();
        }
    }




    // Con esto vamos a hacer la conexion con el HC-05, inspirado en https://github.com/danz1ka19/Android-App-HC05-Arduino
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute ()
        {
            texto_estado.setText("Conectando... Espere...");
            isBtConnecting=true;
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected )
                {
                    // Pedimos el dispositivo en la MAC seleccionada
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    // Creamos una conexion entre el android y el HC05
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    //btSocket = dispositivo.createRfcommSocketToServiceRecord(MY_UUID);

//                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    // Nos conectamos
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            isBtConnecting=false;

            return null;
        }

        @Override
        protected void onPostExecute (Void result)
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                texto_estado.setText("Fallo la conexion, ¿es un HC-05 o un Serial Port Profile?");
            }
            else
            {
                texto_estado.setText("¡Conectado!");
                isBtConnected = true;
            }
        }
    }



}