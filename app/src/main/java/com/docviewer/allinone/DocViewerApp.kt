package com.docviewer.allinone

import android.app.Application

class DocViewerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Native Android PdfRenderer is used, no external init needed
    }
}
