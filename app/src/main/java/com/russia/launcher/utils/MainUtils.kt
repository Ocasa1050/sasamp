package com.russia.launcher.utils

import com.russia.launcher.async.dto.response.FileInfo
import com.russia.launcher.async.dto.response.LatestVersionInfoDto
import com.russia.launcher.async.dto.response.News
import com.russia.launcher.async.dto.response.Servers
import com.russia.launcher.domain.enums.DownloadType

class MainUtils {
    companion object {
        @JvmStatic
        var usselesTex = mutableListOf<String>()

        @JvmStatic
        var preferredTextureExtension: String? = null

        @JvmStatic
        fun configureTextureSupport(
            glExtensions: String?,
            glVersion: String? = null,
            glRenderer: String? = null
        ) {
            val extensions = glExtensions.orEmpty().lowercase()
            val version = glVersion.orEmpty().lowercase()
            preferredTextureExtension = when {
                "gl_img_texture_compression_pvrtc" in extensions ||
                    "gl_img_texture_compression_pvrtc2" in extensions -> ".pvr"
                "gl_oes_compressed_etc1_rgb8_texture" in extensions ||
                    "gl_oes_compressed_etc2_rgb8_texture" in extensions ||
                    "gl_arb_es3_compatibility" in extensions ||
                    version.contains("opengl es 3") -> ".etc"
                "gl_ext_texture_compression_s3tc" in extensions ||
                    "gl_ext_texture_compression_dxt1" in extensions ||
                    "gl_amd_compressed_atc_texture" in extensions ||
                    "gl_angle_texture_compression_dxt1" in extensions ||
                    "gl_angle_texture_compression_dxt3" in extensions ||
                    "gl_angle_texture_compression_dxt5" in extensions -> ".dxt"
                else -> null
            }

            updateUnusedTextureExtensions()

            android.util.Log.i(
                "TextureSupport",
                "selected=$preferredTextureExtension " +
                    "version=${glVersion.orEmpty()} renderer=${glRenderer.orEmpty()} " +
                    "extensions=${extensions.take(512)}"
            )
        }

        /**
         * Selects the best format that is actually published by the cache.
         *
         * GPU detection happens before the remote files list is downloaded, so
         * an ETC-capable device must still fall back to DXT when the server
         * only publishes DXT. The native loader must use this final value too.
         */
        @JvmStatic
        fun selectAvailableTextureExtension(files: Collection<FileInfo>): String? {
            val available = files.mapNotNull { textureFormat(it) }.toSet()
            val supportedFormats = listOf(".pvr", ".etc", ".dxt")
            val selected = supportedFormats.firstOrNull { extension ->
                extension == preferredTextureExtension &&
                    extension in available
            } ?: supportedFormats.firstOrNull { extension ->
                extension in available
            }

            preferredTextureExtension = selected
            updateUnusedTextureExtensions()
            return selected
        }

        @JvmStatic
        fun textureFormat(file: FileInfo): String? {
            val explicitFormat = file.contentFormat
                ?.trim()
                ?.lowercase()
                ?.let { if (it.startsWith(".")) it else ".$it" }
                ?.takeIf { it in setOf(".pvr", ".etc", ".dxt") }

            return explicitFormat ?: listOf(".pvr", ".etc", ".dxt")
                .firstOrNull { file.path.lowercase().contains(it) }
        }

        private fun updateUnusedTextureExtensions() {
            usselesTex = when (preferredTextureExtension) {
                ".pvr" -> mutableListOf(".dxt", ".etc")
                ".etc" -> mutableListOf(".pvr", ".dxt")
                ".dxt" -> mutableListOf(".pvr", ".etc")
                else -> mutableListOf()
            }
        }

        @JvmStatic
        var type = DownloadType.RELOAD_GAME_FILES

        @JvmField
        var FILES_TO_RELOAD: MutableList<FileInfo> = mutableListOf()

        @JvmField
        var LATEST_APK_INFO: LatestVersionInfoDto? = null
    }
}