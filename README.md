# Physics Layout
![Maven Central](https://img.shields.io/maven-central/v/io.github.klassenkonstantin/physics-layout?style=flat-square&versionPrefix=0.4)

This library offers a [dyn4j](https://www.dyn4j.org) wrapper for [Jetpack Compose](https://developer.android.com/jetpack/compose).

## About this fork

This repository is a fork of [KlassenKonstantin/ComposePhysicsLayout](https://github.com/KlassenKonstantin/ComposePhysicsLayout), originally created by Konstantin Klassen. I forked the project to explore how its Compose and dyn4j integration could support many lightweight physics objects without requiring one Composable for every object.

The work that followed has focused on:

- Adding `PhysicsCanvas`, which simulates many dyn4j bodies while drawing them through a single Compose `Canvas`.
- Keeping the original `physicsBody` API for rich, interactive Composables and allowing both rendering approaches to share one simulation.
- Publishing transformations as one observable frame and pacing simulation updates with the display frame clock.
- Cleaning up removed transformations and drag joints, fixing border caching, and improving body/effect synchronization.
- Adding a 300-particle Canvas demonstration and a gravity-sensor-controlled circular maze with false paths and dead ends.

The goal is not to replace the original Composable-based API. It is to provide a hybrid approach: use `physicsBody` for complex UI elements and `PhysicsCanvasBody` for large collections of simple particles, sprites, walls, or game objects.

## 🚧 Experimental 🚧
Before reaching version 1.0, this library is considered experimental, which means that there is no guaranteed backwards compatibility between versions. Signatures, interfaces, names, etc. may and will most likely change.

## Sample App
https://user-images.githubusercontent.com/1836066/206856910-d2172e7e-64da-454e-99b9-8171cf5f5eeb.mov

## Download
```
dependencies {
    implementation 'io.github.klassenkonstantin:physics-layout:<version>'
}
```

> The Maven Central coordinate above belongs to the original upstream project. The fork-only
> `PhysicsCanvas` API and samples must currently be built from this repository or published under
> a separate artifact coordinate.

# How to use
To get started, create a `PhysicsLayout` and add arbitrary content to it. Add the `physicsBody` modifier to Composables that should be part of the physics simulation.

## PhysicsLayout
```kotlin
@Composable
fun PhysicsLayout(
    modifier: Modifier = Modifier,
    shape: Shape? = RectangleShape,
    scale: Dp = DEFAULT_SCALE,
    simulation: Simulation = rememberSimulation(),
    content: @Composable BoxScope.() -> Unit
)
```
- `shape`: The shape of the outer border of the `PhysicsLayout`
- `scale`: How many Dps should be considered one meter. Bodies shouldn't be smaller than one meter
- `simulation`: Does the mapping between layout and physics engine
- `content`: The arbitrary layout

## physicsBody modifier
```kotlin
fun Modifier.physicsBody(
    id: String? = null,
    shape: Shape = RectangleShape,
    bodyConfig: BodyConfig = BodyConfig(),
    dragConfig: DragConfig? = null,
)
```
- `id`: The id the body should have in the simulation. Useful for operations that act directly on bodies (not yet supported).
- `shape`: Describes the outer bounds of the body. Supported shapes are:
  - [RectangleShape](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/package-summary#RectangleShape())
  - [CircleShape](https://developer.android.com/reference/kotlin/androidx/compose/foundation/shape/package-summary#CircleShape())
  - [RoundedCornerShape](https://developer.android.com/reference/kotlin/androidx/compose/foundation/shape/RoundedCornerShape)
  - [CutCornerShape](https://developer.android.com/reference/kotlin/androidx/compose/foundation/shape/CutCornerShape)
- `bodyConfig`: Configures properties of the body
- `dragConfig`: Set a `DragConfig` to enable drag support, or `null` to disable dragging

## Clock
By default `Simulation` uses a default `Clock` which automatically starts running. To pause and resume a `Clock`, create an instance with `rememberClock()` and pass that to the `Simulation`.

### Example usage
```kotlin
@Composable
fun SimpleScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        PhysicsLayout {
            Card(
                modifier = Modifier.physicsBody(
                    shape = CircleShape,
                ).align(Alignment.Center),
                shape = CircleShape,
            ) {
                Icon(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    tint = Color.White
                )
            }
        }
    }
}
```
This example adds a ball with a star in the center of the layout, which then starts falling to the ground.

> Note: The `shape` must be set on both the body modifier and the `Card`.

## PhysicsCanvas

Use `PhysicsCanvas` for hundreds of simple physics sprites. It registers normal dyn4j bodies but
draws the whole collection through one Compose `Canvas`, avoiding a composable, graphics layer, and
pointer-input modifier for every sprite. Its optional content block can still contain rich bodies
using `physicsBody`, so both rendering paths can share one simulation:

```kotlin
val simulation = rememberSimulation()
val particles = remember {
    List(300) { index ->
        PhysicsCanvasBody(
            id = "particle-$index",
            size = DpSize(8.dp, 8.dp),
            initialOffset = DpOffset((index % 20 * 10).dp, (index / 20 * 10).dp),
            color = Color.Cyan,
        )
    }
}

PhysicsCanvas(
    bodies = particles,
    modifier = Modifier.fillMaxSize(),
    scale = 8.dp,
    simulation = simulation,
) {
    Card(
        Modifier
            .align(Alignment.Center)
            .physicsBody(shape = CircleShape)
    ) {
        Text("Rich composable")
    }
}
```

Pass `drawBody` when sprites need custom Canvas rendering. Per-body composable semantics and input
belong on `physicsBody`; high-count Canvas sprites should use centralized interaction handling.
Because `scale` also converts physical acceleration to screen distance, very small scale values may
need proportionally stronger gravity for a fast visual particle effect.

The sample app also contains a small tilt-controlled **Canvas Maze** game. Its player and static
maze walls are `PhysicsCanvasBody` values, the green goal is drawn with `drawBackground`, and
`onBodyState` handles goal detection without creating a Composable for every physics object.

### Change gravity
If you need to change the gravity of the simulated world, use `Simulation.setGravity`

## Caveats, notes, missing features
- Use `PhysicsCanvas` instead of one Composable per body for high-count particle-like effects.
- In general, what is true for all of Compose is especially true for this Layout: **Release builds perform way better than debug builds**.
- State is not restored on config changes 😱.
- Canvas body transforms can be observed with `onBodyState`; collision callbacks and equivalent observation for Composable bodies are not yet exposed.
- Not tested with scrolling containers
