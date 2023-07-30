package br.com.icaro.findbluetooth.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH
import android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL
import android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.icaro.findbluetooth.R
import br.com.icaro.findbluetooth.modelo.DeviceBluetooth
import br.com.icaro.findbluetooth.ui.adapter.ListaDispositivosBluetoothAdapter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.Locale
import java.util.UUID


class MainActivity : Activity() {

    private val REQUEST_ENABLE_BT: Int =1
    private  val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var pairedDevices: MutableSet<BluetoothDevice>? = mutableSetOf()
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    private lateinit var bluetoothHeadset: BluetoothHeadset
    private var bluetoothDevice: BluetoothDevice? = null
    private var isHeadsetConnected = false
    private lateinit var audioManager: AudioManager

    private val REQUEST_CODE_SPEECH_INPUT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activy_main)
        val  spinner: ProgressBar = findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        initializeBluetooth()
    }

    @SuppressLint("MissingPermission")
    private fun initializeBluetooth() {
        if ( this.bluetoothAdapter != null) {


            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

            }

            val button = findViewById<Button>(br.com.icaro.findbluetooth.R.id.buscabluetoothpareado)
            button.setOnClickListener {
                PreparaDevicesPareados();

            }

            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (audioManager.isBluetoothScoAvailableOffCall) {
                val profileListener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.HEADSET) {
                            bluetoothHeadset = proxy as BluetoothHeadset
                            val connectedDevices = bluetoothHeadset.connectedDevices
                            if (connectedDevices.isNotEmpty()) {
                                bluetoothDevice = connectedDevices[0]
                                isHeadsetConnected = true
                            }
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HEADSET) {
                            isHeadsetConnected = false
                        }
                    }
                }


                bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)

                // Registre o receptor de transmissões Bluetooth para detectar a conexão e desconexão do fone de ouvido Bluetooth
                val filter = IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                registerReceiver(headsetReceiver, filter)
            }

        }


    }

    @SuppressLint("MissingPermission")
    private fun PreparaDevicesPareados() {
        val spinner: ProgressBar = findViewById(R.id.progressBar1);
        spinner.setVisibility(View.VISIBLE);
        val listaDevices = mutableListOf<DeviceBluetooth>();
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->

            val m: Method = device.javaClass.getMethod("isConnected")
            if ( m.invoke(device) as Boolean) {
                Toast.makeText(this, "${device!!.name} já Conectado!", Toast.LENGTH_LONG).show();

                var ligarFuntion: (() -> Unit)? = null
                var gravarFuntion: (() -> Unit)? = null
                if (isHeadsetConnected && bluetoothDevice != null) {
                    ligarFuntion = { startCall(bluetoothDevice!!) }
                    gravarFuntion = { startVoiceRecognition() }
                } else {
                    Toast.makeText(
                        this,
                        "Fone de ouvido Bluetooth não está conectado",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                listaDevices.add(
                    DeviceBluetooth(
                        device!!.name,
                        device!!.address,
                        true,
                        ligarFuntion, gravarFuntion
                    )
                )
            } else {
                listaDevices.add(
                    DeviceBluetooth(
                        device!!.name,
                        device!!.address,
                        false,
                        null,
                        null
                    )
                )
            }

        }

        listDispositivosBluetooth(listaDevices)
        spinner.setVisibility(View.GONE)
    }

    private fun startCall(device: BluetoothDevice) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:993529337") // Substitua pelo número que você deseja ligar
        intent.putExtra("android.bluetooth.device.extra.DEVICE", device)
        intent.putExtra("android.bluetooth.device.extra.BLUETOOTH_SCO", true)
        startActivity(intent)
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    // Fone de ouvido Bluetooth conectado
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        bluetoothDevice = device
                        isHeadsetConnected = true
                        PreparaDevicesPareados()
                        Toast.makeText(context, "Dispositivo conectado: ${bluetoothDevice!!.name}",Toast.LENGTH_LONG).show();
                    }
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {

                    Toast.makeText(context, "Dispositivo disconetado: ${bluetoothDevice!!.name}",Toast.LENGTH_LONG).show();
                    bluetoothDevice = null
                    isHeadsetConnected = false
                    PreparaDevicesPareados()
                }
            }
        }
    }


    private fun listDispositivosBluetooth(listDevices:List<DeviceBluetooth>) {

            var recycledView = findViewById<RecyclerView>(R.id.recyclerView)
            recycledView.adapter = ListaDispositivosBluetoothAdapter(this, listDevices)
            recycledView.layoutManager = LinearLayoutManager(this)

    }

    fun startVoiceRecognition() {
        val intent = Intent(ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {

                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results != null && results.isNotEmpty()) {
                    val spokenText = results[0]
                    Toast.makeText(this, "Você disse: $spokenText", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        unregisterReceiver(headsetReceiver)
    }
}