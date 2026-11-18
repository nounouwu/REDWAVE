package com.redwave.player

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.DisposableEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedwaveApp()
        }
    }
}

@Composable
fun RedwaveApp() {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    var currentTrack by remember { mutableStateOf("Nothing playing") }
    var streamUrl by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
            currentTrack = uri.lastPathSegment ?: "Local audio"
            isPlaying = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00D69A),
            background = Color(0xFF080D0B),
            surface = Color(0xFF121A16)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF080D0B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(35.dp))

                Text(
                    text = "REDWAVE",
                    color = Color(0xFF00D69A),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp
                )

                Text(
                    text = "YOUR MUSIC. YOUR WORLD.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(55.dp))

                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(
                            Color(0xFF14251D),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RW",
                        color = Color(0xFF00D69A),
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = currentTrack,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "LOCAL PLAYER",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        filePicker.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D69A),
                        contentColor = Color.Black
                    )
                ) {
                    Text("CHOOSE LOCAL MUSIC", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            player.play()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isPlaying) "PAUSE" else "PLAY",
                        color = Color(0xFF00D69A)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                HorizontalDivider(color = Color(0xFF26352D))

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "STREAM FROM A LEGAL AUDIO URL",
                    color = Color(0xFF00D69A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Paste an audio stream URL")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        if (streamUrl.startsWith("https://") ||
                            streamUrl.startsWith("http://")
                        ) {
                            player.setMediaItem(
                                MediaItem.fromUri(streamUrl)
                            )
                            player.prepare()
                            player.play()
                            currentTrack = "Online stream"
                            isPlaying = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PLAY STREAM", color = Color(0xFF00D69A))
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "REDWAVE 1.0 • MADE FOR MUSIC",
                    color = Color.DarkGray,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
