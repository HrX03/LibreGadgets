package dev.hrx.libregadgets.gadgets

import android.content.Context
import android.graphics.Color
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer
import dev.hrx.libregadgets.core.storage.GlucoseMeasurement
import dev.hrx.libregadgets.core.storage.GlucoseThresholds
import dev.hrx.libregadgets.core.storage.SharedStorage

data class GlucoseWearTileState(
    val lastMeasurement: GlucoseMeasurement?, val glucoseThresholds: GlucoseThresholds?
)

@ExperimentalHorologistApi
class GlucoseWearTileService : SuspendingTileService() {
    private lateinit var renderer: GlucoseWearTileRenderer
    private lateinit var storage: SharedStorage

    override fun onCreate() {
        super.onCreate()

        renderer = GlucoseWearTileRenderer(this)
        storage = SharedStorage(this)
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        return renderer.renderTimeline(
            GlucoseWearTileState(
                storage.latestMeasurement, storage.glucoseThresholds
            ), requestParams
        )
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return renderer.produceRequestedResources(0, requestParams)
    }
}

@OptIn(ExperimentalHorologistApi::class)
class GlucoseWearTileRenderer(context: Context) :
    SingleTileLayoutRenderer<GlucoseWearTileState, Int>(context) {
    override fun renderTile(
        state: GlucoseWearTileState, deviceParameters: DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return tileLayout(state, context, deviceParameters)
    }

    override fun ResourceBuilders.Resources.Builder.produceRequestedResources(
        resourceState: Int, deviceParameters: DeviceParameters, resourceIds: List<String>
    ) {

    }
}

internal fun tileLayout(
    state: GlucoseWearTileState,
    context: Context,
    deviceParameters: DeviceParameters,
) = PrimaryLayout.Builder(deviceParameters).setContent(
    Text.Builder(context, "${state.lastMeasurement?.value}")
        .setTypography(Typography.TYPOGRAPHY_DISPLAY1).setColor(ColorBuilders.argb(Color.WHITE))
        .build()
).build()

@OptIn(ExperimentalHorologistApi::class)
fun updateWearTile(context: Context) {
    TileService.getUpdater(context).requestUpdate(GlucoseWearTileService::class.java)
}