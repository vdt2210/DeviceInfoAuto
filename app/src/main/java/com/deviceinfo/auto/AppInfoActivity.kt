package com.deviceinfo.auto

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnLayout
import java.util.Calendar

class AppInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithEdgeBarsAndUpToolbar(R.layout.activity_app_info, R.string.app_info_title)

        val pInfo = packageInfoCompat()
        val appInfo = pInfo.applicationInfo  ?: @Suppress("DEPRECATION") packageManager.getApplicationInfo(packageName, 0)

        findViewById<ImageView>(R.id.about_app_icon).setImageDrawable(appInfo.loadIcon(packageManager))
        findViewById<FrameLayout>(R.id.about_icon_frame).doOnLayout { frame ->
            frame.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            frame.clipToOutline = true
        }
        findViewById<TextView>(R.id.about_hero_title).text = appInfo.loadLabel(packageManager)
        findViewById<TextView>(R.id.about_version).text =
            pInfo.versionName?.takeIf { it.isNotBlank() } ?: "—"

        bindHtmlLink(R.id.about_link_author, AUTHOR_GITHUB_URL, R.string.link_author_profile)
        bindHtmlLink(R.id.about_link_source, SOURCE_REPO_URL, R.string.link_source_repo)
        bindHtmlLink(R.id.about_link_release, RELEASES_LATEST_URL, R.string.link_open_latest_release)

        findViewById<TextView>(R.id.about_libraries_body).text = formatLibrariesList()

        val year = Calendar.getInstance().get(Calendar.YEAR)
        findViewById<TextView>(R.id.about_copyright).text =
            getString(R.string.about_copyright, year, AUTHOR_CREDIT)
    }

    private fun formatLibrariesList(): String =
        ABOUT_LIBRARY_LINES.joinToString("\n") { "• $it" }

    private fun bindHtmlLink(textViewId: Int, url: String, labelRes: Int) {
        findViewById<TextView>(textViewId).apply {
            val label = getString(labelRes)
            text = HtmlCompat.fromHtml(
                """<a href="$url">$label</a>""",
                HtmlCompat.FROM_HTML_MODE_COMPACT,
            )
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    @Suppress("DEPRECATION")
    private fun packageInfoCompat(): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }

    override fun onSupportNavigateUp(): Boolean = defaultFinishOnNavigateUp()

    companion object {
        // Sync with app/build.gradle.kts when dependencies change.
        private val ABOUT_LIBRARY_LINES = listOf(
            "AndroidX Core KTX 1.12.0",
            "AndroidX Activity 1.8.2",
            "AndroidX AppCompat 1.6.1",
            "AndroidX Car App Library 1.4.0",
            "AndroidX RecyclerView 1.3.2",
            "AndroidX SwipeRefreshLayout 1.1.0",
        )

        private const val AUTHOR_CREDIT = "vdt2210"
        private const val AUTHOR_GITHUB_URL = "https://github.com/vdt2210"
        private const val SOURCE_REPO_URL = "https://github.com/vdt2210/DeviceInfoAuto"
        private const val RELEASES_LATEST_URL = "https://github.com/vdt2210/DeviceInfoAuto/releases/latest"
    }
}
