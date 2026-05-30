package com.example.scrolltrek.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    val nextStep = {
        val nextPage = pagerState.currentPage + 1
        if (nextPage < 5) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
            Unit
        } else {
            Unit
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> OnboardingStep1ValueProp(onNext = { nextStep() })
                    1 -> OnboardingStep2Accessibility(onNext = { nextStep() })
                    2 -> OnboardingStep3Notifications(onNext = { nextStep() })
                    3 -> OnboardingStep4Battery(onNext = { nextStep() })
                    4 -> OnboardingStep5Complete(onComplete = onComplete)
                }
            }
        }
    }
}
