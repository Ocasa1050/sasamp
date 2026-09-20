package com.russia.launcher.ui.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.GsonBuilder
import com.russia.game.databinding.ActivitySplashBinding
import com.russia.launcher.NetworkService
import com.russia.launcher.async.dto.response.GameFileInfoDto
import com.russia.launcher.async.dto.response.LatestVersionInfoDto
import com.russia.launcher.async.dto.response.MonitoringDataLoaderListener
import com.russia.launcher.async.dto.response.ServersList
import com.russia.launcher.async.task.CacheChecker
import com.russia.launcher.config.Config
import com.russia.launcher.domain.enums.DownloadType
import com.russia.launcher.utils.MainUtils
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class SplashActivity : AppCompatActivity() {
    private var permissionsGranded = false
    private var apkVersionChecked = false
    private var monitoringDataLoaded = false
    private var filesListLoaded = false
    private var animationEnded = false
    private var gpuDetected = false
    private var gpuSurfaceView: GLSurfaceView? = null

    private val REQUEST_ID = 228
    private val permissionList = arrayOf(
//        Manifest.permission.READ_EXTERNAL_STORAGE,
//        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        //Manifest.permission.POST_NOTIFICATIONS // 13+ bug
    )

    private var networkService: NetworkService
    private lateinit var binding: ActivitySplashBinding

    init {
        val gson = GsonBuilder().setLenient().create()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // Таймаут на подключение
            .readTimeout(10, TimeUnit.SECONDS)    // Таймаут на чтение
            .writeTimeout(10, TimeUnit.SECONDS)   // Таймаут на запись
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Config.LIVE_RUSSIA_RESOURCE_SERVER_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient) // Устанавливаем OkHttpClient с таймаутами
            .build()

        networkService = retrofit.create(NetworkService::class.java)
    }

    private val isOnline: Boolean
        get() {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseCrashlytics.getInstance().deleteUnsentReports()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        binding.lottieLogo.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animationEnded = true
                startIfReady()
            }
        })

        if (!isOnline) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Ошибка!")
                .setMessage("Нет соединения с интернетом")
                .setPositiveButton("Закрыть") { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                    finishAffinity()
                }
            runOnUiThread { builder.create().show() }
        }

        ServersList.load(
            this,
            networkService,
            object : MonitoringDataLoaderListener {
                override fun monitoringDataLoadedSuccess() {
                    monitoringDataLoaded = true
                    startIfReady()
                }
            }
        )

        initializeGpuCheck()
        checkVersion()
        checkPermissions()

    }

    private fun initializeGpuCheck() {
        val surfaceView = GLSurfaceView(this)
        surfaceView.layoutParams = ViewGroup.LayoutParams(1, 1)
        surfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                if (gpuDetected) {
                    return
                }

                val extensions = gl?.glGetString(GL10.GL_EXTENSIONS).orEmpty()
                MainUtils.configureTextureSupport(extensions)
                gpuDetected = true

                runOnUiThread {
                    binding.activitySplash.removeView(surfaceView)
                    gpuSurfaceView = null
                    loadFilesList()
                    startIfReady()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) = Unit

            override fun onDrawFrame(gl: GL10?) = Unit
        })

        binding.activitySplash.addView(surfaceView)
        gpuSurfaceView = surfaceView
    }

    private fun loadFilesList() {
        val call = networkService.filesList

        call?.enqueue(object : Callback<GameFileInfoDto> {
            override fun onResponse(call: Call<GameFileInfoDto>, response: Response<GameFileInfoDto>) {
                if (response.isSuccessful) {
                    response.body()?.let { CacheChecker.setFilesList(this@SplashActivity, it) }
                }
                filesListLoaded = true
                startIfReady()
            }

            override fun onFailure(call: Call<GameFileInfoDto>, t: Throwable) {
                Log.d("tag", "onFailure = " + t.message);
                filesListLoaded = true
                startIfReady()
            }
        })
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_ID)
        } else {
            permissionsGranded = true
            startIfReady()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_ID) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranded = true
                    startIfReady()
                }
            }
        }
    }

    fun startIfReady() {
        if (permissionsGranded &&
            apkVersionChecked &&
            filesListLoaded &&
            monitoringDataLoaded &&
            animationEnded &&
            gpuDetected
        ) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun checkVersion() {
        if (!Config.AUTO_APK_UPDATE_ENABLED) {
            apkVersionChecked = true
            startIfReady()
            return
        }

        val latestVersionInfoCall = networkService?.latestVersionInfoDto
        latestVersionInfoCall?.enqueue(object : Callback<LatestVersionInfoDto?> {
            override fun onResponse(call: Call<LatestVersionInfoDto?>, response: Response<LatestVersionInfoDto?>) {
                if (!response.isSuccessful) {
                    finish()
                    exitProcess(0)
                }
                val currentVersion = currentVersion
                val latestVersion: Int = response.body()?.version?.toInt() ?: 0
                MainUtils.LATEST_APK_INFO = response.body()
                if (currentVersion >= latestVersion) {
                    apkVersionChecked = true
                    startIfReady()
                    return
                }
                MainUtils.type = DownloadType.UPDATE_APK
                startActivity(Intent(this@SplashActivity, LoaderActivity::class.java))
            }

            override fun onFailure(call: Call<LatestVersionInfoDto?>, t: Throwable) {
                finish()
                exitProcess(0)
            }
        })
    }


    private val currentVersion: Int
        get() {
            val pm = this.packageManager
            try {
                val pInfo = pm.getPackageInfo(this.packageName, 0)
                return pInfo.versionCode
            } catch (e1: PackageManager.NameNotFoundException) {
                e1.printStackTrace()
            }
            finish()
            exitProcess(0)
        }

    public override fun onDestroy() {
        gpuSurfaceView?.onPause()
        gpuSurfaceView?.let { binding.activitySplash.removeView(it) }
        gpuSurfaceView = null
        super.onDestroy()
    }

}