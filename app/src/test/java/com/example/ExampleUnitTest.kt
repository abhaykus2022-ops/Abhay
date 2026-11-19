package com.example

import com.example.data.AspectRatioPreset
import com.example.data.ExportResolution
import com.example.data.VfxPreset
import com.example.data.formatFileSize
import com.example.data.formatTimecode
import com.example.media.VfxPipelineBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `timecode formatting renders minutes seconds and tenths`() {
    assertEquals("00:00.0", formatTimecode(0L))
    assertEquals("01:05.4", formatTimecode(65_420L))
    assertEquals("02:00", formatTimecode(120_000L, includeTenths = false))
  }

  @Test
  fun `export dimensions respect 9 to 16 shorts and even encoder alignment`() {
    val (w1080, h1080) = VfxPipelineBuilder.computeExportDimensions(
      aspect = AspectRatioPreset.SHORTS_9_16,
      resolution = ExportResolution.HD_1080P,
      sourceWidth = 1920,
      sourceHeight = 1080
    )
    assertEquals(1080, w1080)
    assertEquals(1920, h1080)

    val (w720, h720) = VfxPipelineBuilder.computeExportDimensions(
      aspect = AspectRatioPreset.LANDSCAPE_16_9,
      resolution = ExportResolution.HD_720P,
      sourceWidth = 1920,
      sourceHeight = 1080
    )
    assertEquals(1280, w720)
    assertEquals(720, h720)
    assertEquals(0, w720 % 2)
    assertEquals(0, h720 % 2)
  }

  @Test
  fun `vfx and aspect ratio presets resolve safely`() {
    assertEquals(AspectRatioPreset.SHORTS_9_16, AspectRatioPreset.fromId(null))
    assertEquals(VfxPreset.GLASS_PRISM, VfxPreset.fromId(null))
    assertEquals(VfxPreset.GLASS_PRISM, VfxPreset.fromId("GLASS_PRISM"))
    assertEquals(VfxPreset.CINEMA_TEAL, VfxPreset.fromId("CINEMA_TEAL"))
    assertEquals(com.example.data.ProjectSourceType.YOUTUBE_LINK, com.example.data.ProjectSourceType.fromId("YOUTUBE_LINK"))
    assertEquals(com.example.data.ProjectSourceType.LOCAL_VIDEO, com.example.data.ProjectSourceType.fromId(null))
    assertEquals("2.5 MB", formatFileSize(2_621_440L))
  }
}
