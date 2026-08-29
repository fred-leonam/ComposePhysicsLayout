package de.apuri.physicslayout.lib

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.apuri.physicslayout.lib.simulation.Simulation
import de.apuri.physicslayout.lib.simulation.SimulationBody
import de.apuri.physicslayout.lib.simulation.SimulationShape
import de.apuri.physicslayout.lib.simulation.rememberSimulation
import org.dyn4j.geometry.Vector2
import java.util.UUID
import kotlin.math.min

/**
 * A lightweight physics body rendered as part of a single [PhysicsCanvas].
 *
 * [initialOffset] is the body's initial center relative to the center of the canvas. It is only
 * applied when [id] is first added. Change [id] when a body should be respawned at a new position.
 */
@Immutable
data class PhysicsCanvasBody(
    val id: String,
    val size: DpSize,
    val initialOffset: DpOffset = DpOffset.Zero,
    val initialRotationDegrees: Float = 0f,
    val shape: PhysicsCanvasShape = PhysicsCanvasShape.Circle,
    val bodyConfig: BodyConfig = BodyConfig(),
    val color: Color = Color.White,
)

/** Shapes intentionally limited to cheap primitives suitable for high body counts. */
@Immutable
enum class PhysicsCanvasShape {
    Circle,
    Rectangle,
}

/**
 * Runs [bodies] in a physics simulation while drawing all of them in one Compose [Canvas].
 *
 * Use this for large collections of simple sprites. Use [physicsBody] instead when an individual
 * body needs rich composable content, semantics, or its own pointer-input handling. Bodies in this
 * canvas can share a [simulation] with composable physics bodies.
 *
 * [drawBody] receives the body's center in canvas pixels and its clockwise rotation in degrees.
 * Supplying a custom drawer makes it possible to render images or other lightweight sprites
 * without introducing a composable and graphics layer for every body.
 */
@Composable
fun PhysicsCanvas(
    bodies: List<PhysicsCanvasBody>,
    modifier: Modifier = Modifier,
    shape: Shape? = RectangleShape,
    scale: Dp = 32.dp,
    simulation: Simulation = rememberSimulation(),
    drawBody: DrawScope.(
        body: PhysicsCanvasBody,
        center: Offset,
        rotationDegrees: Float,
    ) -> Unit = { body, center, rotationDegrees ->
        drawDefaultBody(body, center, rotationDegrees)
    },
    content: @Composable BoxScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    val scalePx = with(density) { scale.toPx().toDouble() }
    val bodySnapshot = bodies.toList()
    val canvasInstanceId = remember(simulation, density, scalePx) { UUID.randomUUID().toString() }
    val activeBodyIds = remember(simulation, canvasInstanceId) { mutableSetOf<String>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun simulationId(bodyId: String) = "$canvasInstanceId:$bodyId"

    LaunchedEffect(simulation, bodySnapshot, canvasSize, density, scalePx) {
        if (canvasSize == IntSize.Zero) return@LaunchedEffect

        require(bodySnapshot.map { it.id }.toSet().size == bodySnapshot.size) {
            "PhysicsCanvas body ids must be unique"
        }

        val nextBodyIds = bodySnapshot.mapTo(mutableSetOf()) { simulationId(it.id) }
        (activeBodyIds - nextBodyIds).forEach { id ->
            simulation.syncSimulationBody(id, null)
        }

        bodySnapshot.forEach { body ->
            val widthPx = with(density) { body.size.width.toPx() }
            val heightPx = with(density) { body.size.height.toPx() }
            require(widthPx > 0f && heightPx > 0f) {
                "PhysicsCanvas body '${body.id}' must have a positive size"
            }

            val offsetX = with(density) { body.initialOffset.x.toPx() } / scalePx
            val offsetY = with(density) { body.initialOffset.y.toPx() } / scalePx
            val simulationShape = when (body.shape) {
                PhysicsCanvasShape.Circle -> SimulationShape.Circle(
                    min(widthPx, heightPx) / scalePx / 2.0
                )

                PhysicsCanvasShape.Rectangle -> SimulationShape.Rectangle(
                    widthPx / scalePx,
                    heightPx / scalePx,
                )
            }

            simulation.syncSimulationBody(
                simulationId(body.id),
                SimulationBody(
                    width = widthPx / scalePx,
                    height = heightPx / scalePx,
                    shape = simulationShape,
                    initialOffset = Vector2(offsetX, offsetY),
                    initialRotationDegrees = body.initialRotationDegrees.toDouble(),
                    bodyConfig = body.bodyConfig,
                )
            )
        }

        activeBodyIds.clear()
        activeBodyIds.addAll(nextBodyIds)
    }

    DisposableEffect(simulation, canvasInstanceId) {
        onDispose {
            activeBodyIds.forEach { id -> simulation.syncSimulationBody(id, null) }
            activeBodyIds.clear()
        }
    }

    PhysicsLayout(
        modifier = modifier,
        shape = shape,
        scale = scale,
        simulation = simulation,
    ) {
        Canvas(
            Modifier
                .matchParentSize()
                .onSizeChanged { canvasSize = it }
        ) {
            val canvasCenter = center
            val transformations = simulation.currentTransformations()
            bodySnapshot.forEach { body ->
                transformations[simulationId(body.id)]?.let { transformation ->
                    val bodyCenter = canvasCenter + Offset(
                        x = (transformation.translationX * scalePx).toFloat(),
                        y = (transformation.translationY * scalePx).toFloat(),
                    )
                    drawBody(body, bodyCenter, transformation.rotation.toFloat())
                }
            }
        }
        content()
    }
}

private fun DrawScope.drawDefaultBody(
    body: PhysicsCanvasBody,
    center: Offset,
    rotationDegrees: Float,
) {
    val width = body.size.width.toPx()
    val height = body.size.height.toPx()
    when (body.shape) {
        PhysicsCanvasShape.Circle -> drawCircle(
            color = body.color,
            radius = min(width, height) / 2f,
            center = center,
        )

        PhysicsCanvasShape.Rectangle -> rotate(rotationDegrees, center) {
            drawRect(
                color = body.color,
                topLeft = center - Offset(width / 2f, height / 2f),
                size = androidx.compose.ui.geometry.Size(width, height),
            )
        }
    }
}
