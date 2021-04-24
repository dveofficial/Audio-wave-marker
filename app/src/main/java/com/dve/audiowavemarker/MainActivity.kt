package com.dve.audiowavemarker

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dve.audiowavelib.soundfile.SoundFile
import com.dve.audiowavemarker.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {
    var mPermission = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initPermission()
        setButtonClicks()
    }

    private fun setButtonClicks() {
        binding.chooseFile.setOnClickListener {
            fileReq.launch("*/*")
        }
    }

    private val fileReq = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        Log.d(TAG, "uri: ${uri.path}")
        val mFilename = uri.toString().replaceFirst("file://".toRegex(), "").replace("%20".toRegex(), " ")
        val mFile = File(mFilename)
        var mLoadingLastUpdateTime = getCurrentTime()
        var mLoadingKeepGoing = true
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val mDensity = metrics.density
        CoroutineScope(Dispatchers.IO).launch{
            val mSoundFile = SoundFile.create(mFile.absolutePath, object : SoundFile.ProgressListener {
                override fun reportProgress(fractionComplete: Double): Boolean {
                    val now: Long = getCurrentTime()
                    if (now - mLoadingLastUpdateTime > 100) {
                        binding.progressHorizontal.apply {
                            progress = (max * fractionComplete).toInt()
                        }
                        mLoadingLastUpdateTime = now
                    }
                    return mLoadingKeepGoing
                }

            })

            mSoundFile?.let {
                binding.audioWaveView.setSoundFile(it)
                binding.audioWaveView.recomputeHeights(mDensity)
            }

        }

    }

    private fun initPermission() {
        try {
            if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            mPermission[0]
                    ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                            applicationContext,
                            mPermission[1]
                    ) != PackageManager.PERMISSION_GRANTED

            ) {
                ActivityCompat.requestPermissions(this, mPermission, 1)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentTime(): Long {
        return System.nanoTime() / 1000000
    }
}