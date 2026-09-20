package com.russia.launcher.async.dto.response

import java.io.Serializable

class FileInfo : Serializable {
    var path: String = ""
    var size: Long = 0
    var hash: Long = 0
    var url: String = ""
}