package com.russia.launcher.async.dto.response

import java.io.Serializable

class FileInfo : Serializable {
    var path: String = ""
    var size: Long = 0
    var hash: Long = 0
    var url: String = ""
    // The filename can be an alias (for example .etc/.pvr) while the
    // downloaded binary still has the original compression format.
    var contentFormat: String? = null
}