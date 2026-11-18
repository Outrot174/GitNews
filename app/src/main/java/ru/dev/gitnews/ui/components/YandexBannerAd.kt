package ru.dev.gitnews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import ru.dev.gitnews.BuildConfig
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder

@Composable
fun YandexBannerAd(
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    var isAdVisible by remember { mutableStateOf(false) }
    val colors = GitNewsTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isAdVisible) {
                    Modifier
                        .wrapContentHeight()
                        .background(colors.surfaceCanvas)
                        .hairlineBorder(colors.borderSubtle, RoundedCornerShape(0.dp))
                } else {
                    Modifier.height(0.dp)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                try {
                    BannerAdView(ctx).apply {
                        setAdUnitId(BuildConfig.YANDEX_BANNER_ID)
                        setAdSize(BannerAdSize.stickySize(ctx, screenWidthDp.coerceAtLeast(320)))
                        setBannerAdEventListener(object : BannerAdEventListener {
                            override fun onAdLoaded() {
                                isAdVisible = true
                            }

                            override fun onAdFailedToLoad(error: AdRequestError) {
                                isAdVisible = false
                            }

                            override fun onAdClicked() {}
                            override fun onLeftApplication() {}
                            override fun onReturnedToApplication() {}
                            override fun onImpression(impressionData: ImpressionData?) {}
                        })
                        loadAd(AdRequest.Builder().build())
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("YandexBannerAd", "BannerAdView creation failed", t)
                    isAdVisible = false
                    android.view.View(ctx)
                }
            }
        )
    }
}
