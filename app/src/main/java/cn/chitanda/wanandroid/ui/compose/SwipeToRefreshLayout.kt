package cn.chitanda.wanandroid.ui.compose

import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * @Author:       Chen
 * @Date:         2021/3/16 15:28
 * @Description:  https://github.com/android/compose-samples/blob/92f2f16e4e63fa0e4418f660dde9e9558674cee5/JetNews/app/src/main/java/com/example/jetnews/ui/SwipeToRefresh.kt
 */

@ExperimentalMaterialApi
@Composable
fun SwipeToRefreshLayout(
    refreshingState: Boolean,
    onRefresh: () -> Unit,
    refreshDistance: Dp = 80.dp,
    refreshIndicator: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val swipeDistance = with(LocalDensity.current) {
        refreshDistance.toPx()
    }

    val state = rememberSwipeableState(refreshingState) { newValue ->
        // compare both copies of the swipe state before calling onRefresh(). This is a workaround.
        if (newValue && !refreshingState) onRefresh()
        true
    }

    Box(
        modifier = Modifier
            .nestedScroll(state.PreUpPostDownNestedScrollConnection)
            .swipeable(
                state = state,
                anchors = mapOf(
                    -swipeDistance to false,
                    swipeDistance to true
                ),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Vertical
            )
    ) {
        content()
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, state.offset.value.roundToInt()) }) {
            if (state.offset.value != -swipeDistance) {
                refreshIndicator()
            }
        }

        // TODO (https://issuetracker.google.com/issues/164113834): This state->event trampoline is a
        //  workaround for a bug in the SwipableState API. Currently, state.value is a duplicated
        //  source of truth of refreshingState.
        LaunchedEffect(refreshingState) { state.animateTo(refreshingState) }
    }
}

/**
 * Temporary workaround for nested scrolling behavior. There is no default implementation for
 * pull to refresh yet, this nested scroll connection mimics the behavior.
 */
@ExperimentalMaterialApi
private val <T> SwipeableState<T>.PreUpPostDownNestedScrollConnection: NestedScrollConnection
    get() = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.Drag) {
                performDrag(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            return if (source == NestedScrollSource.Drag) {
                performDrag(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = Offset(available.x, available.y).toFloat()
            return if (toFling < 0) {
                performFling(velocity = toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                Velocity.Zero
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity
        ): Velocity {
            performFling(velocity = Offset(available.x, available.y).toFloat())
            return available
        }

        private fun Float.toOffset(): Offset = Offset(0f, this)

        private fun Offset.toFloat(): Float = this.y
    }