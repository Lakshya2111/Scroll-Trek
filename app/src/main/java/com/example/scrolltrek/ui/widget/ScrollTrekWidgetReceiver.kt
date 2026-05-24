package com.example.scrolltrek.ui.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.scrolltrek.MainActivity
import com.example.scrolltrek.data.repository.ScrollRepository
import com.example.scrolltrek.ui.common.DistanceFormatter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun scrollRepository(): ScrollRepository
}

class ScrollTrekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScrollTrekWidget()
}

class ScrollTrekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
        val repository = entryPoint.scrollRepository()

        // Fetch current states
        val summary = repository.todaySummary.first()
        val progress = repository.landmarkProgress.first()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        Text(
                            text = "scrollTrek",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = DistanceFormatter.format(summary.totalDistanceM),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Text(
                        text = "Today's Scroll Distance",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    LinearProgressIndicator(
                        progress = progress.progressFraction,
                        modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                        color = GlanceTheme.colors.primary,
                        backgroundColor = GlanceTheme.colors.onSurfaceVariant
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    val toNextStr = DistanceFormatter.format(progress.distanceToNextM)
                    Text(
                        text = "$toNextStr to ${progress.nextLandmark.name}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
