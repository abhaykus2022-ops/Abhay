package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.media.YouTubeLinkResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SlateCut AI", appName)
  }

  @Test
  fun `youtube link resolver extracts ids from all valid formats`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resolver = YouTubeLinkResolver(context)

    assertEquals("dQw4w9WgXcQ", resolver.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", resolver.extractVideoId("https://youtu.be/dQw4w9WgXcQ?t=42"))
    assertEquals("dQw4w9WgXcQ", resolver.extractVideoId("https://youtube.com/shorts/dQw4w9WgXcQ?feature=share"))
    assertEquals("dQw4w9WgXcQ", resolver.extractVideoId("https://www.youtube.com/live/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", resolver.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", resolver.extractVideoId("dQw4w9WgXcQ"))
    assertNull(resolver.extractVideoId("https://vimeo.com/123456789"))
    assertNull(resolver.extractVideoId("not a link"))
  }
}
