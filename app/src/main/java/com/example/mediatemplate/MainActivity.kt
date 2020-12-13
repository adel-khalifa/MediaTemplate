package com.example.mediatemplate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediatemplate.databinding.ActivityMainBinding
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior


class MainActivity : AppCompatActivity(), MediaAdapter.OnTrackClickListener {
    private val TAG = Constants.TAG

    private lateinit var mAdapter: MediaAdapter
    private lateinit var recycler: RecyclerView
    private var exoPlayer: SimpleExoPlayer? = null
    private lateinit var styledExoView: StyledPlayerView
    private lateinit var controlView: PlayerControlView
    private lateinit var binding: ActivityMainBinding
    private lateinit var mServiceConnection: ServiceConnection
    private lateinit var mediaSheet: BottomSheetBehavior<ConstraintLayout>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { binding ->
            setContentView(binding.root)
            styledExoView = binding.mediaCoordi.mainExo
            controlView = binding.mediaCoordi.peekController
            recycler = binding.mainRv
        }
        handleNightThemes()
        binding.mainIvMode.setOnClickListener {
            Log.i(TAG, "onCreate: clicked")
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                Log.i(TAG, "onCreate: set MODE to NO")

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                Log.i(TAG, "onCreate: set  MODE to YES")

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

            }
        }
        val mediaList = assets.list("")?.filter { it.contains(".mp3") or (it.contains(".mp4")) }
        initAndFeedRecyclerWithList(mediaList)

        initPlayerViews()
        Log.i(TAG, "onCreate: ***intent*** ${intent.toString()}")

        initServiceConnection()
        mediaSheetCallBacks()
        startService(getServiceIntent())
        bindService(getServiceIntent(), mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun handleNightThemes() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.mainIvMode.setImageResource(R.drawable.ic_switch_to_light)
        } else {
            binding.mainIvMode.setImageResource(R.drawable.ic_switch_to_dark)
        }



    }

    private fun initServiceConnection() {
        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.i(TAG, "onServiceConnected: componentName....... ${name?.className}")
                Log.i(TAG, "onServiceConnected: serviceBinder...... $service")
                val serviceBinder = service as MyService.AudioServiceBinder

                // attach the same service's exoPlayer instance for player's Views to sync with activity
                // with consideration of detach it in onDestroy
                exoPlayer = serviceBinder.exoPlayer
                Log.i(TAG, "onServiceConnected: exoHashCode...... ${exoPlayer.hashCode()}")

                styledExoView.player = exoPlayer
                controlView.player = exoPlayer

            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.i(TAG, "@@@@@@@@@ onServiceDisconnected: $name @@@@@@@@@@@@@")
                exoPlayer?.release()
                exoPlayer = null

            }
        }

    }

    private fun getServiceIntent() = Intent(this@MainActivity, MyService::class.java)

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: ")
        // if media is playing , unBind it also keep the service running and we can reBind it again in onCreate
        // otherwise if the media is paused that means (we stopped the service) and unBind it leads to destroy service and clear resources

        if (!exoPlayer?.isPlaying!!) {
            unbindService(mServiceConnection)
            stopService(getServiceIntent())
        } else {

            unbindService(mServiceConnection)
        }

        // avoid memory leaks by removing player instance  (which contains the  destroyed activity context) from exoPlayerViews
        styledExoView.player = null
        controlView.player = null

        super.onDestroy()
    }

    private fun mediaSheetCallBacks() {
        mediaSheet =
            BottomSheetBehavior.from(binding.mediaCoordi.mediaSheetConstraintLayout).also { sheet ->
                sheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_EXPANDED -> {
                                controlView.visibility = View.GONE
                                controlView.player = null
                                styledExoView.showController()
                            }
                            BottomSheetBehavior.STATE_COLLAPSED -> {
                                controlView.visibility = View.VISIBLE
                                controlView.player = exoPlayer
                            }
                            else -> {
                            }
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }
                })
            }
    }

    private fun initPlayerViews() {
        styledExoView.apply {
            setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL)
        }
    }

    override fun onBackPressed() {
        if (mediaSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            mediaSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        } else super.onBackPressed()
    }

    private fun initAndFeedRecyclerWithList(mediaList: List<String>?) {
        mAdapter = MediaAdapter()

        mAdapter.setOnTrackClickListener(this@MainActivity)
        mAdapter.asyncListDiffer.submitList(mediaList)

        recycler.apply {
            layoutManager =
                LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = mAdapter

        }
    }

    override fun onTrackClick(position: Int, trackName: String) {

        exoPlayer?.apply {
            seekTo(position, 0)
            prepare()
            play()
        }
    }


}

