package com.example.daktarsaab

import android.content.Context
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A composable function that plays a video from the assets folder
 *
 * @param assetFileName The name of the file in the assets folder (e.g., "td0HRrGzg8Ucjra91t.mp4")
 * @param title Optional title to display above the video
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun AssetVideoPlayer(
    assetFileName: String = "td0HRrGzg8Ucjra91t.mp4",
    title: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Use AndroidView to incorporate VideoView
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        // Create MediaController for playback controls
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                update = { videoView ->
                    // Create URI for assets file
                    val uri = "file:///android_asset/$assetFileName"
                    videoView.setVideoURI(Uri.parse(uri))

                    // Start playing automatically
                    videoView.setOnPreparedListener { mp ->
                        mp.isLooping = true
                        videoView.start()
                    }

                    // Handle errors
                    videoView.setOnErrorListener { _, what, extra ->
                        android.util.Log.e("AssetVideoPlayer", "Error playing video: what=$what, extra=$extra")
                        true
                    }
                }
            )
        }
    }

    // Clean up when leaving the composable
    DisposableEffect(Unit) {
        onDispose {
            // No cleanup needed for VideoView
        }
    }
}
