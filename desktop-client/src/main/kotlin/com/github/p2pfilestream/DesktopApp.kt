package com.github.p2pfilestream

import com.github.p2pfilestream.views.LoginView
import javafx.application.Application
import tornadofx.App

class DesktopApp : App(LoginView::class, Styles::class)

/**
 * The main method is needed to support the mvn jfx:run goal.
 */
fun main(args: Array<String>) {
    Application.launch(DesktopApp::class.java, *args)
}