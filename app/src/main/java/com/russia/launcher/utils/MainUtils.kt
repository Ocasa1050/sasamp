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
        fun configureTextureSupport(glExtensions: String?) {
            val extensions = glExtensions.orEmpty().lowercase()
            preferredTextureExtension = when {
                "gl_img_texture_compression_pvrtc" in extensions -> ".pvr"
                "gl_ext_texture_compression_s3tc" in extensions ||
                    "gl_ext_texture_compression_dxt1" in extensions ||
                    "gl_amd_compressed_atc_texture" in extensions ||
                    "gl_angle_texture_compression_dxt1" in extensions ||
                    "gl_angle_texture_compression_dxt3" in extensions ||
                    "gl_angle_texture_compression_dxt5" in extensions -> ".dxt"
                "gl_oes_compressed_etc1_rgb8_texture" in extensions ||
                    "gl_oes_compressed_etc2_rgb8_texture" in extensions ||
                    "gl_arb_es3_compatibility" in extensions -> ".etc"
                else -> null
            }

            usselesTex = when (preferredTextureExtension) {
                ".pvr" -> mutableListOf(".dxt", ".etc")
                ".dxt" -> mutableListOf(".pvr", ".etc")
                ".etc" -> mutableListOf(".pvr", ".dxt")
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