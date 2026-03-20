package dev.cavatz.keybindertv.data.export

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import kotlinx.serialization.json.Json

/**
 * Encode/décode la configuration KeyBinderTv en QR code via ZXing.
 *
 * Limite QR code : ~2953 bytes en mode binaire niveau de correction L.
 * Si la config dépasse cette limite, [encode] retourne null.
 *
 * @param json Instance Json sans prettyPrint pour minimiser la taille
 */
class QrCodeCodec(
    private val json: Json = Json { encodeDefaults = true },
    private val importer: ConfigImporter = ConfigImporter()
) {

    companion object {
        /** Taille maximum en bytes pour un QR code de niveau L */
        const val MAX_QR_BYTES: Int = 2953

        /** Largeur et hauteur par défaut du QR code généré */
        private const val DEFAULT_QR_SIZE: Int = 512
    }

    /**
     * Encode la configuration en Bitmap QR code.
     * Retourne null si la configuration sérialisée dépasse [MAX_QR_BYTES].
     *
     * @param config Configuration à encoder
     * @param width  Largeur du QR en pixels (défaut 512)
     * @param height Hauteur du QR en pixels (défaut 512)
     * @return Bitmap si la config tient dans le QR, null sinon
     */
    fun encode(
        config: KeyBinderConfig,
        width: Int = DEFAULT_QR_SIZE,
        height: Int = DEFAULT_QR_SIZE
    ): Bitmap? {
        val jsonString: String = json.encodeToString(KeyBinderConfig.serializer(), config)
        val byteCount: Int = jsonString.toByteArray(Charsets.UTF_8).size
        if (byteCount > MAX_QR_BYTES) {
            return null
        }
        return try {
            BarcodeEncoder().encodeBitmap(jsonString, BarcodeFormat.QR_CODE, width, height)
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Décode un Bitmap QR code en [ConfigImporter.ImportResult].
     * Utilise ZXing pour lire le contenu puis l'importe via [ConfigImporter].
     *
     * @param bitmap Image contenant le QR code à décoder
     * @return [ConfigImporter.ImportResult.Success] ou [ConfigImporter.ImportResult.Error]
     */
    fun decode(bitmap: Bitmap): ConfigImporter.ImportResult {
        return try {
            val pixels: IntArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            importer.importFromJson(result.text)
        } catch (exception: Exception) {
            ConfigImporter.ImportResult.Error("Échec du décodage QR : ${exception.message}")
        }
    }
}
