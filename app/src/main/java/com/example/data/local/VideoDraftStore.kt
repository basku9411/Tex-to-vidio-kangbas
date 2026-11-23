package com.example.data.local

import android.content.Context
import android.net.Uri
import com.example.model.AiModel
import com.example.model.AnimationFontOption
import com.example.model.AnimationTextColor
import com.example.model.AnimationTextMotion
import com.example.model.AnimationTextStyle
import com.example.model.AspectRatioOption
import com.example.model.DurationOption
import com.example.model.EffectColorTheme
import com.example.model.EffectDensity
import com.example.model.EffectSpeed
import com.example.model.GenerationMode
import com.example.model.PhotoAnimationConfig
import com.example.model.PhotoEffect
import com.example.model.PhotoTextOverlay
import com.example.model.PhotoVideoRatio
import com.example.model.ResolutionOption
import com.example.model.SubtitleColorOverride
import com.example.model.SubtitlePosition
import com.example.model.SubtitleSegment
import com.example.model.SubtitleSizeOption
import com.example.model.SubtitleStylePreset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed interface SavedVideoDraft {
    data class PhotoFx(
        val photoFilePath: String?,
        val photoUriString: String?,
        val photoName: String,
        val audioUriString: String?,
        val audioName: String?,
        val config: PhotoAnimationConfig,
    ) : SavedVideoDraft

    data class AutoCaption(
        val videoUriString: String,
        val videoTitle: String,
        val preset: SubtitleStylePreset,
        val position: SubtitlePosition,
        val size: SubtitleSizeOption,
        val color: SubtitleColorOverride,
        val segments: List<SubtitleSegment>,
    ) : SavedVideoDraft

    data class CreateAi(
        val mode: GenerationMode,
        val model: AiModel,
        val prompt: String,
        val ratio: AspectRatioOption,
        val resolution: ResolutionOption,
        val duration: DurationOption,
        val audioEnabled: Boolean,
    ) : SavedVideoDraft
}

object VideoDraftStore {
    private fun draftsDir(context: Context): File =
        File(context.filesDir, "video_drafts").apply { mkdirs() }

    private fun draftFile(context: Context, historyId: String): File =
        File(draftsDir(context), "${historyId}.json")

    fun hasDraft(context: Context, historyId: String): Boolean =
        draftFile(context, historyId).exists()

    fun savePhotoFxDraft(
        context: Context,
        historyId: String,
        photoUri: Uri?,
        photoName: String,
        audioUri: Uri?,
        audioName: String?,
        config: PhotoAnimationConfig,
    ) {
        runCatching {
            var copiedPhotoPath: String? = null
            if (photoUri != null) {
                val targetPhoto = File(draftsDir(context), "${historyId}_photo.jpg")
                context.contentResolver.openInputStream(photoUri)?.use { input ->
                    targetPhoto.outputStream().use { output -> input.copyTo(output) }
                    copiedPhotoPath = targetPhoto.absolutePath
                } ?: run {
                    if (photoUri.scheme == "file" && photoUri.path != null) {
                        val src = File(photoUri.path!!)
                        if (src.exists()) {
                            src.copyTo(targetPhoto, overwrite = true)
                            copiedPhotoPath = targetPhoto.absolutePath
                        }
                    }
                }
            }

            val effectsArr = JSONArray().apply {
                config.selectedEffects.forEach { put(it.name) }
            }
            val textsArr = JSONArray().apply {
                config.textOverlays.forEach { t ->
                    put(
                        JSONObject().apply {
                            put("id", t.id)
                            put("text", t.text)
                            put("font", t.font.name)
                            put("sizeFraction", t.sizeFraction.toDouble())
                            put("yFraction", t.yFraction.toDouble())
                            put("color", t.color.name)
                            put("style", t.style.name)
                            put("motion", t.motion.name)
                        }
                    )
                }
            }
            val subArr = JSONArray().apply {
                config.timedSubtitles.forEach { s ->
                    put(
                        JSONObject().apply {
                            put("id", s.id)
                            put("startMs", s.startMs)
                            put("endMs", s.endMs)
                            put("text", s.text)
                        }
                    )
                }
            }
            val json = JSONObject().apply {
                put("type", "PHOTO_FX")
                put("photoFilePath", copiedPhotoPath ?: "")
                put("photoUriString", photoUri?.toString() ?: "")
                put("photoName", photoName)
                put("audioUriString", audioUri?.toString() ?: "")
                put("audioName", audioName ?: "")
                put("selectedEffects", effectsArr)
                put("density", config.density.name)
                put("speed", config.speed.name)
                put("colorTheme", config.colorTheme.name)
                put("ratio", config.ratio.name)
                put("durationSeconds", config.durationSeconds.toDouble())
                put("durationSourceLabel", config.durationSourceLabel ?: "")
                put("textOverlays", textsArr)
                put("timedSubtitles", subArr)
                put("subtitlePreset", config.subtitlePreset.name)
                put("subtitlePosition", config.subtitlePosition.name)
                put("subtitleSize", config.subtitleSize.name)
                put("subtitleColor", config.subtitleColor.name)
            }
            draftFile(context, historyId).writeText(json.toString())
        }
    }

    fun saveAutoCaptionDraft(
        context: Context,
        historyId: String,
        videoUri: Uri,
        videoTitle: String,
        preset: SubtitleStylePreset,
        position: SubtitlePosition,
        size: SubtitleSizeOption,
        color: SubtitleColorOverride,
        segments: List<SubtitleSegment>,
    ) {
        runCatching {
            val segArr = JSONArray().apply {
                segments.forEach { s ->
                    put(
                        JSONObject().apply {
                            put("id", s.id)
                            put("startMs", s.startMs)
                            put("endMs", s.endMs)
                            put("text", s.text)
                        }
                    )
                }
            }
            val json = JSONObject().apply {
                put("type", "AUTO_CAPTION")
                put("videoUriString", videoUri.toString())
                put("videoTitle", videoTitle)
                put("preset", preset.name)
                put("position", position.name)
                put("size", size.name)
                put("color", color.name)
                put("segments", segArr)
            }
            draftFile(context, historyId).writeText(json.toString())
        }
    }

    fun saveCreateAiDraft(
        context: Context,
        historyId: String,
        mode: GenerationMode,
        model: AiModel,
        prompt: String,
        ratio: AspectRatioOption,
        resolution: ResolutionOption,
        duration: DurationOption,
        audioEnabled: Boolean,
    ) {
        runCatching {
            val json = JSONObject().apply {
                put("type", "CREATE_AI")
                put("mode", mode.name)
                put("model", model.name)
                put("prompt", prompt)
                put("ratio", ratio.name)
                put("resolution", resolution.name)
                put("duration", duration.name)
                put("audioEnabled", audioEnabled)
            }
            draftFile(context, historyId).writeText(json.toString())
        }
    }

    fun loadDraft(context: Context, historyId: String): SavedVideoDraft? {
        val file = draftFile(context, historyId)
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            when (json.optString("type")) {
                "PHOTO_FX" -> {
                    val fxArr = json.optJSONArray("selectedEffects") ?: JSONArray()
                    val effects = buildSet {
                        for (i in 0 until fxArr.length()) {
                            runCatching { PhotoEffect.valueOf(fxArr.getString(i)) }
                                .getOrNull()?.let { add(it) }
                        }
                    }
                    val textsArr = json.optJSONArray("textOverlays") ?: JSONArray()
                    val overlays = buildList {
                        for (i in 0 until textsArr.length()) {
                            val o = textsArr.optJSONObject(i) ?: continue
                            add(
                                PhotoTextOverlay(
                                    id = o.optString("id", "txt-$i"),
                                    text = o.optString("text", ""),
                                    font = enumOrDefault(o.optString("font"), AnimationFontOption.PLAYFAIR),
                                    sizeFraction = o.optDouble("sizeFraction", 0.068).toFloat(),
                                    yFraction = o.optDouble("yFraction", 0.80).toFloat(),
                                    color = enumOrDefault(o.optString("color"), AnimationTextColor.WHITE),
                                    style = enumOrDefault(o.optString("style"), AnimationTextStyle.OUTLINE),
                                    motion = enumOrDefault(o.optString("motion"), AnimationTextMotion.FLOAT),
                                )
                            )
                        }
                    }
                    val subArr = json.optJSONArray("timedSubtitles") ?: JSONArray()
                    val timedSubs = buildList {
                        for (i in 0 until subArr.length()) {
                            val o = subArr.optJSONObject(i) ?: continue
                            add(
                                SubtitleSegment(
                                    id = o.optString("id", "sub-$i"),
                                    startMs = o.optLong("startMs", 0L),
                                    endMs = o.optLong("endMs", 2000L),
                                    text = o.optString("text", ""),
                                )
                            )
                        }
                    }
                    val srcLabel = json.optString("durationSourceLabel").ifBlank { null }
                    SavedVideoDraft.PhotoFx(
                        photoFilePath = json.optString("photoFilePath").ifBlank { null },
                        photoUriString = json.optString("photoUriString").ifBlank { null },
                        photoName = json.optString("photoName", "foto-animasi.jpg"),
                        audioUriString = json.optString("audioUriString").ifBlank { null },
                        audioName = json.optString("audioName").ifBlank { null },
                        config = PhotoAnimationConfig(
                            selectedEffects = effects,
                            density = enumOrDefault(json.optString("density"), EffectDensity.MEDIUM),
                            speed = enumOrDefault(json.optString("speed"), EffectSpeed.NORMAL),
                            colorTheme = enumOrDefault(json.optString("colorTheme"), EffectColorTheme.NATURAL),
                            ratio = enumOrDefault(json.optString("ratio"), PhotoVideoRatio.LANDSCAPE_16_9),
                            durationSeconds = json.optDouble("durationSeconds", 10.0).toFloat(),
                            durationSourceLabel = srcLabel,
                            textOverlays = overlays,
                            timedSubtitles = timedSubs,
                            subtitlePreset = enumOrDefault(json.optString("subtitlePreset"), SubtitleStylePreset.TIKTOK_POP),
                            subtitlePosition = enumOrDefault(json.optString("subtitlePosition"), SubtitlePosition.BOTTOM),
                            subtitleSize = enumOrDefault(json.optString("subtitleSize"), SubtitleSizeOption.MEDIUM),
                            subtitleColor = enumOrDefault(json.optString("subtitleColor"), SubtitleColorOverride.PRESET),
                        ),
                    )
                }

                "AUTO_CAPTION" -> {
                    val segArr = json.optJSONArray("segments") ?: JSONArray()
                    val segments = buildList {
                        for (i in 0 until segArr.length()) {
                            val o = segArr.optJSONObject(i) ?: continue
                            add(
                                SubtitleSegment(
                                    id = o.optString("id", "seg-$i"),
                                    startMs = o.optLong("startMs", 0L),
                                    endMs = o.optLong("endMs", 2000L),
                                    text = o.optString("text", ""),
                                )
                            )
                        }
                    }
                    SavedVideoDraft.AutoCaption(
                        videoUriString = json.optString("videoUriString", ""),
                        videoTitle = json.optString("videoTitle", "video.mp4"),
                        preset = enumOrDefault(json.optString("preset"), SubtitleStylePreset.TIKTOK_POP),
                        position = enumOrDefault(json.optString("position"), SubtitlePosition.BOTTOM),
                        size = enumOrDefault(json.optString("size"), SubtitleSizeOption.MEDIUM),
                        color = enumOrDefault(json.optString("color"), SubtitleColorOverride.PRESET),
                        segments = segments,
                    )
                }

                "CREATE_AI" -> SavedVideoDraft.CreateAi(
                    mode = enumOrDefault(json.optString("mode"), GenerationMode.TEXT),
                    model = enumOrDefault(json.optString("model"), AiModel.SEEDANCE_V1_LITE_TEXT),
                    prompt = json.optString("prompt", ""),
                    ratio = enumOrDefault(json.optString("ratio"), AspectRatioOption.LANDSCAPE),
                    resolution = enumOrDefault(json.optString("resolution"), ResolutionOption.R720),
                    duration = enumOrDefault(json.optString("duration"), DurationOption.D15),
                    audioEnabled = json.optBoolean("audioEnabled", false),
                )

                else -> null
            }
        }.getOrNull()
    }

    fun readRawDraftJson(context: Context, historyId: String): String? =
        runCatching {
            val file = draftFile(context, historyId)
            if (file.exists()) file.readText().take(32_000) else null
        }.getOrNull()

    fun saveRawDraftJsonIfAbsent(context: Context, historyId: String, rawJson: String?) {
        if (rawJson.isNullOrBlank()) return
        runCatching {
            val file = draftFile(context, historyId)
            if (!file.exists()) {
                file.writeText(rawJson)
            }
        }
    }

    fun deleteDraft(context: Context, historyId: String) {
        runCatching {
            draftFile(context, historyId).delete()
            File(draftsDir(context), "${historyId}_photo.jpg").delete()
        }
    }

    fun clearAllDrafts(context: Context) {
        runCatching {
            draftsDir(context).listFiles()?.forEach { it.delete() }
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, fallback: T): T {
        if (name.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
    }
}
