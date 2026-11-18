
package com.redwave.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val CLIENT_ID = "6d039eb0"

data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val image: String,
    val audio: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedwaveApp()
        }
    }
}

private val background = Color(0xFF080808)
private val panel = Color(0xFF191919)
private val red = Color(0xFFFF304F)
private val muted = Color(0xFF999999)

suspend fun fetchTracks(
    query: String = "",
    genre: String = ""
): List<Track> = withContext(Dispatchers.IO) {
    try {
        val params = buildString {
            append("client_id=$CLIENT_ID")
            append("&format=json")
            append("&limit=30")
            append("&audioformat=mp32")
            append("&imagesize=300")
            append("&order=popularity_week")

            if (query.isNotBlank()) {
                append("&namesearch=")
                append(URLEncoder.encode(query, "UTF-8"))
            }

            if (genre.isNotBlank()) {
                append("&fuzzytags=")
                append(URLEncoder.encode(genre, "UTF-8"))
            }
        }

        val connection = URL(
            "https://api.jamendo.com/v3.0/tracks/?$params"
        ).openConnection() as HttpURLConnection

        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val response = connection.inputStream.bufferedReader().use {
            it.readText()
        }

        connection.disconnect()

        val results = JSONObject(response).getJSONArray("results")

        buildList {
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val audio = item.optString("audio")

                if (audio.isNotBlank()) {
                    add(
                        Track(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            artist = item.optString("artist_name"),
                            image = item.optString("image"),
                            audio = audio
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun RedwaveApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    var tracks by remember {
        mutableStateOf<List<Track>>(emptyList())
    }

    var currentTrack by remember {
        mutableStateOf<Track?>(null)
    }

    var search by remember {
        mutableStateOf("")
    }

    var selectedTab by remember {
        mutableStateOf("Home")
    }

    var selectedGenre by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var playing by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf(false)
    }

    val liked = remember {
        mutableStateListOf<String>()
    }

    fun loadMusic(
        query: String = "",
        genre: String = ""
    ) {
        scope.launch {
            loading = true
            error = false

            val result = fetchTracks(query, genre)

            tracks = result
            error = result.isEmpty()
            loading = false
        }
    }

    fun playTrack(track: Track) {
        currentTrack = track
        player.setMediaItem(MediaItem.fromUri(track.audio))
        player.prepare()
        player.play()
        playing = true
    }

    LaunchedEffect(Unit) {
        loadMusic()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    val displayedTracks = if (selectedTab == "Library") {
        tracks.filter { it.id in liked }
    } else {
        tracks
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = background,
            surface = panel,
            primary = red
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(background)
        ) {

            // HEADER
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RED",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "WAVE",
                    color = red,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.weight(1f))

                Text("●", color = red, fontSize = 13.sp)

                Text(
                    "  JAMENDO",
                    color = muted,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
            }

            // NAVIGATION
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                listOf("Home", "Explore", "Search", "Library").forEach { tab ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedTab = tab

                            if (tab == "Home") {
                                selectedGenre = ""
                                loadMusic()
                            }
                        }
                    ) {
                        Text(
                            tab,
                            color = if (selectedTab == tab)
                                Color.White else muted,
                            fontWeight = if (selectedTab == tab)
                                FontWeight.Bold else FontWeight.Normal
                        )

                        if (selectedTab == tab) {
                            Spacer(Modifier.height(5.dp))

                            Box(
                                Modifier
                                    .width(22.dp)
                                    .height(3.dp)
                                    .background(
                                        red,
                                        RoundedCornerShape(5.dp)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(15.dp))

            // SEARCH
            if (selectedTab == "Search") {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    placeholder = {
                        Text("Songs, artists...")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                loadMusic(query = search)
                            }
                        ) {
                            Text("GO", color = red)
                        }
                    }
                )
            }

            // EXPLORE
            if (selectedTab == "Explore") {
                Text(
                    "EXPLORE GENRES",
                    color = muted,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    listOf(
                        "Electronic",
                        "Hip Hop",
                        "Rock",
                        "Jazz",
                        "Pop",
                        "Ambient",
                        "Classical"
                    ).forEach { genre ->
                        FilterChip(
                            selected = selectedGenre == genre,
                            onClick = {
                                selectedGenre = genre
                                loadMusic(genre = genre.lowercase())
                            },
                            label = {
                                Text(genre)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = red,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // LIBRARY HEADER
            if (selectedTab == "Library") {
                Text(
                    "YOUR MUSIC",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Text(
                    "${liked.size} liked tracks",
                    color = muted,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // HOME HEADER
            if (selectedTab == "Home") {
                Text(
                    "Discover something new.",
                    color = muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = 12.dp
                    )
                )

                Text(
                    "Made for your mood.",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(15.dp))

            // MUSIC LIST
            if (loading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = red)
                }
            } else if (displayedTracks.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (selectedTab == "Library")
                            "Your liked tracks will appear here."
                        else if (error)
                            "Couldn't load music. Check your connection."
                        else
                            "No tracks found. Try another search.",
                        color = muted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        bottom = 15.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    item {
                        Text(
                            when (selectedTab) {
                                "Search" -> "SEARCH RESULTS"
                                "Library" -> "LIKED TRACKS"
                                else -> "RECOMMENDED FOR YOU"
                            },
                            color = muted,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    items(
                        displayedTracks,
                        key = { it.id }
                    ) { track ->

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playTrack(track)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.image,
                                contentDescription = track.name,
                                modifier = Modifier
                                    .size(57.dp)
                                    .background(
                                        panel,
                                        RoundedCornerShape(9.dp)
                                    )
                            )

                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    track.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    track.artist,
                                    color = muted,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            TextButton(
                                onClick = {
                                    if (track.id in liked) {
                                        liked.remove(track.id)
                                    } else {
                                        liked.add(track.id)
                                    }
                                }
                            ) {
                                Text(
                                    if (track.id in liked) "♥" else "♡",
                                    color = red,
                                    fontSize = 23.sp
                                )
                            }

                            Text(
                                "▶",
                                color = red,
                                fontSize = 16.sp,
                                modifier = Modifier.clickable {
                                    playTrack(track)
                                }
                            )
                        }
                    }
                }
            }

            // MINI PLAYER
            currentTrack?.let { track ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(panel)
                        .clickable {
                            if (playing) {
                                player.pause()
                                playing = false
                            } else {
                                player.play()
                                playing = true
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.image,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )

                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            track.name,
                            color = Color.White,
                            maxLines = 1
                        )

                        Text(
                            track.artist,
                            color = muted,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        if (playing) "Ⅱ" else "▶",
                        color = red,
                        fontSize = 22.sp
                    )
                }
            }

            // FOOTER
            Text(
                "FEEL EVERY WAVE.",
                color = muted,
                fontSize = 9.sp,
                letterSpacing = 3.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(10.dp)
            )
        }
    }
}
