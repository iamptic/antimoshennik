package com.antimoshennik.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class SpeechRecognizer(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechRecognizer"
        private const val SAMPLE_RATE = 16000
        private const val MODEL_PATH = "vosk-model-small-ru"
        private const val TARGET_RMS = 3000.0
        private const val MIN_RMS = 100.0
        
        var lastMode = "Офлайн (Vosk)"
        
        @Volatile
        private var sharedModel: Model? = null
        private var modelLoading = false
        private val modelLock = Any()
    }
    
        private val corrections = mapOf(
        // Банки
        "с бербанк" to "сбербанк", "сбер банк" to "сбербанк", "сбер" to "сбербанк",
        "ветеби" to "втб", "в т б" to "втб", "вэтэбэ" to "втб",
        "тинь кофф" to "тинькофф", "тиньков" to "тинькофф",
        "альфа банк" to "альфабанк", "альфа" to "альфабанк",
        
        // Безопасность - много вариантов!
        "безопа сности" to "безопасности", "безопасность" to "безопасности",
        "безопас" to "безопасности", "опасности" to "безопасности",
        "особое" to "безопасности", "познать" to "безопасности",
        "без опасн" to "безопасности", "беза пас" to "безопасности",
        
        // Служба
        "служ ба" to "служба", "слушать" to "служба", "служить" to "служба",
        "службы" to "служба", "слу жба" to "служба",
        
        // Блокировка
        "за блокирован" to "заблокирован", "блокиров" to "заблокирован",
        "заблокиро" to "заблокирован", "блокир" to "заблокирован",
        
        // Переводы
        "пере ведите" to "переведите", "перевид" to "переведите",
        "переведи" to "переведите", "перевести" to "переведите",
        
        // Следственный/прокуратура
        "след ственный" to "следственный", "следствен" to "следственный",
        "про куратура" to "прокуратура", "прокурат" to "прокуратура",
        
        // СМС/данные
        "эс эм эс" to "смс", "эсэмэс" to "смс", "сообщение" to "смс",
        "пас порт" to "паспорт", "паспор" to "паспорт",
        
        // Мошенники
        "моше нник" to "мошенник", "мошен" to "мошенник",
        
        // Счёт
        "счёт" to "счет", "щёт" to "счет", "счета" to "счет",
        
        // Карта
        "карт" to "карта", "карту" to "карта", "карты" to "карта",
        
        // Код
        "коды" to "код", "кода" to "код",
        
        // Центральный банк
        "центральн" to "центральный", "централь" to "центральный",
        
        // Уголовное
        "уголовн" to "уголовное", "уголов" to "уголовное",
        
        // Срочно
        "срочн" to "срочно", "сроч" to "срочно",
        
        // Полиция
        "полици" to "полиция", "полис" to "полиция"
    )
    
    init {
        ensureModelLoaded()
    }
    
    private fun ensureModelLoaded() {
        synchronized(modelLock) {
            if (sharedModel != null || modelLoading) return
            modelLoading = true
        }
        
        try {
            val modelDir = File(context.filesDir, MODEL_PATH)
            
            if (!modelDir.exists()) {
                extractModelFromAssets(modelDir)
            }
            
            val fileCount = countFiles(modelDir)
            if (fileCount < 10) {
                modelDir.deleteRecursively()
                extractModelFromAssets(modelDir)
            }
            
            if (modelDir.exists()) {
                sharedModel = Model(modelDir.absolutePath)
                Log.d(TAG, "Vosk model loaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vosk init failed", e)
        } finally {
            synchronized(modelLock) { modelLoading = false }
        }
    }
    
    private fun countFiles(dir: File): Int {
        if (!dir.exists()) return 0
        var count = 0
        dir.walkTopDown().forEach { if (it.isFile) count++ }
        return count
    }
    
    private fun extractModelFromAssets(targetDir: File) {
        try {
            targetDir.mkdirs()
            copyAssetFolder(MODEL_PATH, targetDir)
        } catch (e: IOException) {
            Log.e(TAG, "Extract failed", e)
        }
    }
    
    private fun copyAssetFolder(assetPath: String, targetDir: File) {
        val assetManager = context.assets
        try {
            val entries = assetManager.list(assetPath)
            if (entries.isNullOrEmpty()) {
                targetDir.parentFile?.mkdirs()
                assetManager.open(assetPath).use { input ->
                    targetDir.outputStream().use { output -> input.copyTo(output) }
                }
            } else {
                targetDir.mkdirs()
                for (entry in entries) {
                    copyAssetFolder("$assetPath/$entry", File(targetDir, entry))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Copy error: $assetPath", e)
        }
    }
    
    suspend fun recognizeFromFile(filePath: String): String = withContext(Dispatchers.IO) {
        lastMode = "Офлайн (Vosk)"
        return@withContext recognizeWithVosk(filePath)
    }
    
    private suspend fun recognizeWithVosk(filePath: String): String = withContext(Dispatchers.IO) {
        if (sharedModel == null) ensureModelLoaded()
        val model = sharedModel ?: return@withContext ""
        
        val file = File(filePath)
        if (!file.exists()) return@withContext ""
        
        try {
            var pcmData = decodeAudioToPcm(filePath) ?: return@withContext ""
            if (pcmData.isEmpty()) return@withContext ""
            
            pcmData = applyHighPassFilter(pcmData)
            pcmData = normalizeVolume(pcmData)
            
            val recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
            val result = StringBuilder()
            
            val chunkSize = 4000
            var offset = 0
            
            while (offset < pcmData.size) {
                val end = minOf(offset + chunkSize, pcmData.size)
                val chunk = pcmData.copyOfRange(offset, end)
                if (recognizer.acceptWaveForm(chunk, chunk.size)) {
                    val text = extractText(recognizer.result)
                    if (text.isNotBlank()) result.append(text).append(" ")
                }
                offset = end
            }
            
            val finalText = extractText(recognizer.finalResult)
            if (finalText.isNotBlank()) result.append(finalText)
            
            recognizer.close()
            
            return@withContext postProcessText(result.toString().trim())
            
        } catch (e: Exception) {
            Log.e(TAG, "Recognition error", e)
            return@withContext ""
        }
    }
    
    private fun applyHighPassFilter(input: ByteArray): ByteArray {
        val samples = bytesToShorts(input)
        val output = ShortArray(samples.size)
        val alpha = 0.97f
        var prevInput = 0f
        var prevOutput = 0f
        for (i in samples.indices) {
            val currentInput = samples[i].toFloat()
            val currentOutput = alpha * (prevOutput + currentInput - prevInput)
            output[i] = currentOutput.toInt().coerceIn(-32768, 32767).toShort()
            prevInput = currentInput
            prevOutput = currentOutput
        }
        return shortsToBytes(output)
    }
    
    private fun normalizeVolume(input: ByteArray): ByteArray {
        val samples = bytesToShorts(input)
        var sumSquares = 0.0
        for (sample in samples) sumSquares += sample.toDouble() * sample.toDouble()
        val rms = sqrt(sumSquares / samples.size)
        if (rms < MIN_RMS) return input
        val gain = (TARGET_RMS / rms).coerceIn(0.5, 5.0)
        val output = ShortArray(samples.size)
        for (i in samples.indices) {
            output[i] = (samples[i] * gain).toInt().coerceIn(-32768, 32767).toShort()
        }
        return shortsToBytes(output)
    }
    
    private fun postProcessText(text: String): String {
        var result = text.lowercase()
        for ((wrong, correct) in corrections) result = result.replace(wrong, correct)
        return result.replace(Regex("\\s+"), " ").trim()
    }
    
    private fun decodeAudioToPcm(filePath: String): ByteArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) { audioTrackIndex = i; break }
            }
            if (audioTrackIndex < 0) return null
            
            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            
            val outputStream = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            
            while (!outputDone) {
                if (!inputDone) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                    val pcmData = ByteArray(bufferInfo.size)
                    outputBuffer.get(pcmData)
                    outputStream.write(pcmData)
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
            codec.stop(); codec.release(); extractor.release()
            return convertTo16kMono(outputStream.toByteArray(), sampleRate, channels)
        } catch (e: Exception) {
            Log.e(TAG, "Decode error", e)
            extractor.release()
            return null
        }
    }
    
    private fun convertTo16kMono(input: ByteArray, inputSampleRate: Int, inputChannels: Int): ByteArray {
        val samples = bytesToShorts(input)
        val monoSamples = if (inputChannels == 2) {
            ShortArray(samples.size / 2) { i -> ((samples[i * 2].toInt() + samples[i * 2 + 1].toInt()) / 2).toShort() }
        } else samples
        val ratio = inputSampleRate.toDouble() / SAMPLE_RATE
        val outputLength = (monoSamples.size / ratio).toInt()
        val resampled = ShortArray(outputLength)
        for (i in 0 until outputLength) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val fraction = srcPos - srcIndex
            val s1 = monoSamples[srcIndex.coerceIn(0, monoSamples.size - 1)]
            val s2 = monoSamples[(srcIndex + 1).coerceIn(0, monoSamples.size - 1)]
            resampled[i] = (s1 + (fraction * (s2 - s1)).toInt()).coerceIn(-32768, 32767).toShort()
        }
        return shortsToBytes(resampled)
    }
    
    private fun bytesToShorts(bytes: ByteArray): ShortArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val shorts = ShortArray(buffer.remaining())
        buffer.get(shorts)
        return shorts
    }
    
    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        shorts.forEach { buffer.putShort(it) }
        return bytes
    }
    
    private fun extractText(json: String): String {
        return try { JSONObject(json).optString("text", "") } catch (e: Exception) { "" }
    }
    
    fun release() { }
}
