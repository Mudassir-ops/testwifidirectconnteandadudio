package com.examples.akshay.wifip2p

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.AsyncTask
import android.util.Log
import com.example.murtaza.walkietalkie.MicRecorder
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket

/**
 * Created by ash on 16/2/18.
 */
class ServerSocketThread : AsyncTask<Any?, Any?, Any?>() {
    var serverSocket: ServerSocket? = null
    var receivedData = "null"
    private val port = 8888
    var isInterrupted = false
    var listener: OnUpdateListener? = null

    interface OnUpdateListener {
        fun onUpdate(data: String?)
    }

    fun setUpdateListener(listener: OnUpdateListener?) {
        this.listener = listener
    }

    override fun doInBackground(objects: Array<Any?>): Void? {
        try {
            Log.d(TAG, " started DoInBackground")
            serverSocket = ServerSocket(8888)
            while (!isInterrupted) {
                val client = serverSocket!!.accept()
                Log.d(TAG, "Accepted Connection")

                val inputstream = client.getInputStream()

                startStreaming(client.getInputStream())

//                val bufferedReader = BufferedReader(InputStreamReader(inputstream))
//                val sb = StringBuilder()
//                var line: String?
//                while (bufferedReader.readLine().also { line = it } != null) {
//                    sb.append(line)
//                }
//                bufferedReader.close()
//                Log.d(TAG, "Completed ReceiveDataTask")
//                receivedData = sb.toString()
//                if (listener != null) {
//                    listener!!.onUpdate(receivedData)
//                }
                Log.d(TAG, " ================ $inputstream")
            }
            serverSocket!!.close()
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "IOException occurred")
        }
        return null
    }

    companion object {
        private const val TAG = "===ServerSocketThread"
    }

    fun startStreaming(inputStream1: InputStream) {
        var audioTrack: AudioTrack? = null
        val audioPlayerRunnable = Runnable {
            var bufferSize = AudioTrack.getMinBufferSize(
                MicRecorder.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                bufferSize = MicRecorder.SAMPLE_RATE * 2
            }
            Log.d("PLAY", "buffersize = $bufferSize")
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                MicRecorder.SAMPLE_RATE,
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
                //  val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                val inputStream = inputStream1
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