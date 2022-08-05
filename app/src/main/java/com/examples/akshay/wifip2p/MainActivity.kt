package com.examples.akshay.wifip2p

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.*
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.example.murtaza.walkietalkie.MicRecorder
import com.example.murtaza.walkietalkie.MicRecorder.SAMPLE_RATE
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : AppCompatActivity(), View.OnClickListener, ConnectionInfoListener {
    var mManager: WifiP2pManager? = null
    var mChannel: WifiP2pManager.Channel? = null
    var mReceiver: WifiBroadcastReceiver? = null
    var mIntentFilter: IntentFilter? = null
    var device: WifiP2pDevice? = null
    var serviceDisvcoery: ServiceDiscovery? = null
    var serverSocketThread: ServerSocketThread? = null
    lateinit var deviceListItems: Array<WifiP2pDevice?>


    var buttonDiscoveryStart: Button? = null
    var buttonDiscoveryStop: Button? = null

    var statrtRedordTimer: Button? = null
    var ednRecordTimer: Button? = null

    var buttonConnect: Button? = null
    var buttonServerStart: Button? = null
    var buttonClientStart: Button? = null
    var buttonClientStop: Button? = null
    var buttonServerStop: Button? = null
    var buttonConfigure: Button? = null
    var editTextTextInput: EditText? = null
    var listViewDevices: ListView? = null
    var textViewDiscoveryStatus: TextView? = null
    var textViewWifiP2PStatus: TextView? = null
    var textViewConnectionStatus: TextView? = null
    var textViewReceivedData: TextView? = null
    var textViewReceivedDataStatus: TextView? = null
    var AudioRecordTest: ImageView? = null
    var mAdapter: ArrayAdapter<*>? = null

    var numberOfBytes = 0

    private var micRecorder: MicRecorder? = null
    var outputStream: OutputStream? = null
    var t: Thread? = null
    var keepRecording = true

    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        serviceDisvcoery = ServiceDiscovery()
        setUpUI()
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager!!.initialize(this, mainLooper, null)
        mReceiver = WifiBroadcastReceiver(mManager, mChannel, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
            )
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            discoverDevices()
            discoverPeers()
            //do something, permission was previously granted; or legacy device
        }
        serverSocketThread = ServerSocketThread()
    }

    private fun discoverDevices() {
        mManager!!.requestPeers(mChannel) { wifiP2pDeviceList ->
            for (device in wifiP2pDeviceList.deviceList) {
                Log.d(TAG, "onPeersAvailable: $$device")
                if (device.deviceName == "ABC") Log.d("tag", "found device!!! ")
                // device.deviceName
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setUpIntentFilter()
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    private fun setUpIntentFilter() {
        mIntentFilter = IntentFilter()
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private fun setUpUI() {
        buttonDiscoveryStart = findViewById(R.id.main_activity_button_discover_start)
        buttonDiscoveryStop = findViewById(R.id.main_activity_button_discover_stop)
        buttonConnect = findViewById(R.id.main_activity_button_connect)
        buttonServerStart = findViewById(R.id.main_activity_button_server_start)
        buttonServerStop = findViewById(R.id.main_activity_button_server_stop)
        buttonClientStart = findViewById(R.id.main_activity_button_client_start)
        buttonClientStop = findViewById(R.id.main_activity_button_client_stop)
        buttonConfigure = findViewById(R.id.main_activity_button_configure)
        listViewDevices = findViewById(R.id.main_activity_list_view_devices)
        textViewConnectionStatus = findViewById(R.id.main_activiy_textView_connection_status)
        textViewDiscoveryStatus = findViewById(R.id.main_activiy_textView_dicovery_status)
        textViewWifiP2PStatus = findViewById(R.id.main_activiy_textView_wifi_p2p_status)
        textViewReceivedData = findViewById(R.id.main_acitivity_data)
        textViewReceivedDataStatus = findViewById(R.id.main_acitivity_received_data)



        AudioRecordTest = findViewById(R.id.record_voice)
        statrtRedordTimer = findViewById(R.id.statrtRedordTimer)
        ednRecordTimer = findViewById(R.id.ednRecordTimer)
        statrtRedordTimer!!.setOnClickListener(this)
        ednRecordTimer!!.setOnClickListener(this)
        AudioRecordTest!!.setOnClickListener(this)


        editTextTextInput = findViewById(R.id.main_acitivity_input_text)
        buttonServerStart!!.setOnClickListener(this)
        buttonServerStop!!.setOnClickListener(this)
        buttonClientStart!!.setOnClickListener(this)
        buttonClientStop!!.setOnClickListener(this)
        buttonConnect!!.setOnClickListener(this)
        buttonDiscoveryStop!!.setOnClickListener(this)
        buttonDiscoveryStart!!.setOnClickListener(this)
        buttonConfigure!!.setOnClickListener(this)
        buttonClientStop!!.visibility = View.INVISIBLE
        buttonClientStart!!.visibility = View.INVISIBLE
        buttonServerStop!!.visibility = View.INVISIBLE
        buttonServerStart!!.visibility = View.INVISIBLE
        editTextTextInput!!.visibility = View.INVISIBLE
        textViewReceivedDataStatus!!.visibility = View.INVISIBLE
        textViewReceivedData!!.visibility = View.INVISIBLE

        listViewDevices!!.onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
            device = deviceListItems[i]
            Toast.makeText(
                this@MainActivity,
                "Selected device :" + device!!.deviceName,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun discoverPeers() {
        Log.d(TAG, "discoverPeers()")
        setDeviceList(ArrayList())
        mManager!!.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                stateDiscovery = true
                Log.d(TAG, "peer discovery started")
                makeToast("peer discovery started")

            }

            override fun onFailure(i: Int) {
                stateDiscovery = false
                if (i == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(TAG, " peer discovery failed :" + "P2P_UNSUPPORTED")
                    makeToast(" peer discovery failed :" + "P2P_UNSUPPORTED")
                } else if (i == WifiP2pManager.ERROR) {
                    Log.d(TAG, " peer discovery failed :" + "ERROR")
                    makeToast(" peer discovery failed :" + "ERROR" + WifiP2pManager.ERROR)
                } else if (i == WifiP2pManager.BUSY) {
                    Log.d(TAG, " peer discovery failed :" + "BUSY")
                    makeToast(" peer discovery failed :" + "BUSY")
                }
            }
        })
    }

    private fun stopPeerDiscover() {
        mManager!!.stopPeerDiscovery(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                stateDiscovery = false
                Log.d(TAG, "Peer Discovery stopped")
                makeToast("Peer Discovery stopped")
                //buttonDiscoveryStop.setEnabled(false);
            }

            override fun onFailure(i: Int) {
                Log.d(TAG, "Stopping Peer Discovery failed")
                makeToast("Stopping Peer Discovery failed")
                //buttonDiscoveryStop.setEnabled(true);
            }
        })
    }


    fun connect(device: WifiP2pDevice) {
        // Picking the first device found on the network.
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        config.wps.setup = WpsInfo.PBC
        Log.d(TAG, "Trying to connect : " + device.deviceName)
        mManager!!.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connected to :" + device.deviceName)
                Toast.makeText(
                    application,
                    "Connection successful with " + device.deviceName,
                    Toast.LENGTH_SHORT
                ).show()
                //setDeviceList(new ArrayList<WifiP2pDevice>());
            }

            override fun onFailure(reason: Int) {
                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(TAG, "P2P_UNSUPPORTED")
                    makeToast("Failed establishing connection: " + "P2P_UNSUPPORTED")
                } else if (reason == WifiP2pManager.ERROR) {
                    Log.d(TAG, "Conneciton falied : ERROR")
                    makeToast("Failed establishing connection: " + "ERROR")
                } else if (reason == WifiP2pManager.BUSY) {
                    Log.d(TAG, "Conneciton falied : BUSY")
                    makeToast("Failed establishing connection: " + "BUSY")
                }
            }
        })
    }

    fun setDeviceList(deviceDetails: ArrayList<WifiP2pDevice>) {
        deviceListItems = arrayOfNulls(deviceDetails.size)
        val deviceNames = arrayOfNulls<String>(deviceDetails.size)
        for (i in deviceDetails.indices) {
            deviceNames[i] = deviceDetails[i].deviceName
            deviceListItems[i] = deviceDetails[i]
        }
        mAdapter = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            deviceNames
        )
        listViewDevices!!.adapter = mAdapter
    }

    fun setStatusView(status: Int) {
        when (status) {
            Constants.DISCOVERY_INITATITED -> {
                stateDiscovery = true
                textViewDiscoveryStatus!!.text = "DISCOVERY_INITIATED"
            }
            Constants.DISCOVERY_STOPPED -> {
                stateDiscovery = false
                textViewDiscoveryStatus!!.text = "DISCOVERY_STOPPED"
            }
            Constants.P2P_WIFI_DISABLED -> {
                stateWifi = false
                textViewWifiP2PStatus!!.text = "P2P_WIFI_DISABLED"
                buttonDiscoveryStart!!.isEnabled = false
                buttonDiscoveryStop!!.isEnabled = false
            }
            Constants.P2P_WIFI_ENABLED -> {
                stateWifi = true
                textViewWifiP2PStatus!!.text = "P2P_WIFI_ENABLED"
                buttonDiscoveryStart!!.isEnabled = true
                buttonDiscoveryStop!!.isEnabled = true
            }
            Constants.NETWORK_CONNECT -> {
                stateConnection = true
                makeToast("It's a connect")
                textViewConnectionStatus!!.text = "Connected"
            }
            Constants.NETWORK_DISCONNECT -> {
                stateConnection = false
                textViewConnectionStatus!!.text = "Disconnected"
                makeToast("State is disconnected")
            }
            else -> Log.d(TAG, "Unknown status")
        }
    }

    override fun onClick(view: View) {
        val id = view.id
        when (id) {
            R.id.statrtRedordTimer -> {
                Log.d(TAG, "onClick: ${MicRecorder.outputStream}")

                startStreaming()

            }
            R.id.ednRecordTimer -> {
                if (micRecorder != null) {
                    MicRecorder.keepRecording = false
                }
            }
            R.id.record_voice -> {
                micRecorder = MicRecorder()
                t = Thread(micRecorder)
                if (micRecorder != null) {
                    MicRecorder.keepRecording = true
                }
                t!!.start()

            }

            R.id.main_activity_button_discover_start -> if (!stateDiscovery) {
                discoverPeers()
            }
            R.id.main_activity_button_discover_stop -> if (stateDiscovery) {
                stopPeerDiscover()
            }
            R.id.main_activity_button_connect -> {
                if (device == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Please discover and select a device",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                connect(device!!)
            }
            R.id.main_activity_button_server_start -> {
                serverSocketThread = ServerSocketThread()
                serverSocketThread!!.setUpdateListener { obj -> setReceivedText(obj) }
                serverSocketThread!!.execute()
            }
            R.id.main_activity_button_server_stop -> if (serverSocketThread != null) {
                serverSocketThread!!.isInterrupted = true
            } else {
                Log.d(TAG, "serverSocketThread is null")
            }
            R.id.main_activity_button_client_start -> {
                //serviceDisvcoery.startRegistrationAndDiscovery(mManager,mChannel);


                val dataToSend = editTextTextInput!!.text.toString()
                //--power on echo 0x40 0x04
                //--power off
                val clientSocket = ClientSocket(this@MainActivity, this, dataToSend)
                clientSocket.execute()
            }
            R.id.main_activity_button_configure -> mManager!!.requestConnectionInfo(mChannel, this)
            R.id.main_activity_button_client_stop -> makeToast("Yet to do")
            else -> {}
        }
    }

    private fun makeToast(s: String) {

    }

    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        var hostAddress = wifiP2pInfo.groupOwnerAddress.hostAddress
        if (hostAddress == null) hostAddress = "host is null"
        Log.d(
            TAG,
            "wifiP2pInfo.groupOwnerAddress.getHostAddress() " + wifiP2pInfo.groupOwnerAddress.hostAddress
        )
        IP = wifiP2pInfo.groupOwnerAddress.hostAddress
        IS_OWNER = wifiP2pInfo.isGroupOwner
        if (IS_OWNER) {
            buttonClientStop!!.visibility = View.GONE
            buttonClientStart!!.visibility = View.GONE
            editTextTextInput!!.visibility = View.GONE
            buttonServerStop!!.visibility = View.VISIBLE
            buttonServerStart!!.visibility = View.VISIBLE
            textViewReceivedData!!.visibility = View.VISIBLE
            textViewReceivedDataStatus!!.visibility = View.VISIBLE
        } else {
            //buttonClientStop.setVisibility(View.VISIBLE);
            buttonClientStart!!.visibility = View.VISIBLE
            editTextTextInput!!.visibility = View.VISIBLE
            buttonServerStop!!.visibility = View.GONE
            buttonServerStart!!.visibility = View.GONE
            textViewReceivedData!!.visibility = View.GONE
            textViewReceivedDataStatus!!.visibility = View.GONE
        }
        makeToast("Configuration Completed")
    }

    fun setReceivedText(data: String?) {
        runOnUiThread { textViewReceivedData!!.text = data }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Do something with granted permission
            discoverPeers()
        }
    }

    companion object {
        const val TAG = "===MainActivity"
        private const val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 222

        @JvmField
        var IP: String? = null
        var IS_OWNER = false
        var stateDiscovery = false
        var stateWifi = false
        var stateConnection = false
    }


    fun audioStream() {
        var stopped: Boolean = false
        var readSize = 0
        val sampleRate = 48000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val microphone = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize * 10
        )

        microphone.startRecording()

//Since audioformat is 16 bit, we need to create a 16 bit (short data type) buffer

//Since audioformat is 16 bit, we need to create a 16 bit (short data type) buffer
        val buffer = ShortArray(1024)


        Handler(Looper.getMainLooper()).postDelayed({
            stopped = false
            Log.d(TAG, "audioStream: $buffer ")
            Log.d(TAG, "audioStream: $readSize ")
        }, 3000)

        while (!stopped) {
            readSize = microphone.read(buffer, 0, buffer.size)
            Log.d(TAG, "audioStream: asdasds")
        }


        microphone.stop()
        microphone.release()
    }

    fun startStreaming() {
        val audioPlayerRunnable = Runnable {
            var bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                bufferSize = SAMPLE_RATE * 2
            }
            Log.d("PLAY", "buffersize = $bufferSize")
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack!!.play()
            Log.v("PLAY", "Audio streaming started")
            val buffer = ByteArray(bufferSize)
            val offset = 0
            try {
                val inputStream = ByteArrayInputStream(MicRecorder.outputStream.toByteArray())
                var bytes_read = inputStream.read(buffer, 0, bufferSize)
                while (MicRecorder.keepRecording && bytes_read != -1) {
                    audioTrack!!.write(buffer, 0, buffer.size)
                    bytes_read = inputStream.read(buffer, 0, bufferSize)
                }
                inputStream.close()
                audioTrack!!.release()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
        val t = Thread(audioPlayerRunnable)
        t.start()
    }
}