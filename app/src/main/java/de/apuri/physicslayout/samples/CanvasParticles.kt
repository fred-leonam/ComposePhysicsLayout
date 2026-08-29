package de.apuri.physicslayout.samples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.apuri.physicslayout.GravitySensor
import de.apuri.physicslayout.lib.BodyConfig
import de.apuri.physicslayout.lib.PhysicsCanvas
import de.apuri.physicslayout.lib.PhysicsCanvasBody
import de.apuri.physicslayout.lib.drag.DragConfig
import de.apuri.physicslayout.lib.physicsBody
import de.apuri.physicslayout.lib.simulation.rememberSimulation
import kotlin.random.Random

@Composable
fun CanvasParticlesScreen() {
    val simulation = rememberSimulation()
    val particles = remember {
        val random = Random(7)
        val palette = listOf(
            Color(0xFF42A5F5),
            Color(0xFF66BB6A),
            Color(0xFFAB47BC),
            Color(0xFFFFCA28),
        )
        List(300) { index ->
            PhysicsCanvasBody(
                id = "particle-$index",
                size = DpSize(8.dp, 8.dp),
                initialOffset = DpOffset(
                    x = random.nextInt(-140, 141).dp,
                    y = random.nextInt(-240, 241).dp,
                ),
                bodyConfig = BodyConfig(
                    density = 0.5f,
                    friction = 0.1f,
                    restitution = 0.6f,
                ),
                color = palette[index % palette.size],
            )
        }
    }

    GravitySensor { (x, y) ->
        simulation.setGravity(Offset(-x, y).times(3f))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        PhysicsCanvas(
            bodies = particles,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            scale = 8.dp,
            simulation = simulation,
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .physicsBody(
                        id = "rich-composable",
                        shape = CircleShape,
                        bodyConfig = BodyConfig(density = 5f),
                        dragConfig = DragConfig(),
                    ),
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier.size(88.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Compose")
                }
            }
        }
    }
}
