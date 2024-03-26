package com.github.niqdev.ipcam

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.niqdev.ipcam.databinding.ActivityIpcamDefaultBinding
import com.github.niqdev.ipcam.settings.SettingsActivity
import com.github.niqdev.mjpeg.Mjpeg
import com.github.niqdev.mjpeg.MjpegInputStream
import com.github.niqdev.mjpeg.MjpegSurfaceView

class IpCamDefaultActivity : AppCompatActivity() {

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    private lateinit var binding: ActivityIpcamDefaultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIpcamDefaultBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    private fun getPreference(key: String): String? = sharedPreferences.getString(key, "")

    private fun getBooleanPreference(key: String) = sharedPreferences.getBoolean(key, false)

    private fun calculateDisplayMode() = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        MjpegSurfaceView.DisplayMode.FULLSCREEN
    else
        MjpegSurfaceView.DisplayMode.BEST_FIT

    private fun loadIpCam() {
         Mjpeg()
                .credential(getPreference(SettingsActivity.PREF_AUTH_USERNAME), getPreference(SettingsActivity.PREF_AUTH_PASSWORD))
                .open(getPreference(SettingsActivity.PREF_IPCAM_URL), TIMEOUT)
                .subscribe(
                        { inputStream: MjpegInputStream ->
                            binding.mjpegViewDefault.setSource(inputStream)
                            binding.mjpegViewDefault.setDisplayMode(calculateDisplayMode())
                        }
                ) { throwable: Throwable ->
                    Log.e(javaClass.simpleName, "mjpeg error", throwable)
                    Toast.makeText(this, "Error ${throwable.javaClass.simpleName}\n${getPreference(SettingsActivity.PREF_IPCAM_URL)}", Toast.LENGTH_LONG).show()
                }
    }

    override fun onResume() {
        super.onResume()
        loadIpCam()
    }

    override fun onPause() {
        super.onPause()
        binding.mjpegViewDefault.stopPlayback()
    }

    companion object {
        private const val TIMEOUT = 5
    }
}