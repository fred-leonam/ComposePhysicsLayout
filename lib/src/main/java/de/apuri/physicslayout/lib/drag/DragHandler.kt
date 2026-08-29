package de.apuri.physicslayout.lib.drag

import de.apuri.physicslayout.lib.simulation.SimulationEntity
import de.apuri.physicslayout.lib.simulation.SimulationTouchEvent
import org.dyn4j.dynamics.joint.Joint
import org.dyn4j.dynamics.joint.PinJoint
import org.dyn4j.world.World

internal interface DragHandler {
    fun drag(
        body: SimulationEntity.Body,
        touchEvent: SimulationTouchEvent,
        dragConfig: DragConfig
    )

    fun removeBody(body: SimulationEntity.Body)
}

internal class DefaultDragHandler(
    private val world: World<SimulationEntity<*>>
) : DragHandler {
    private val joints = mutableMapOf<JointKey, PinJoint<SimulationEntity.Body>>()

    override fun drag(
        body: SimulationEntity.Body,
        touchEvent: SimulationTouchEvent,
        dragConfig: DragConfig
    ) {
        val key = JointKey(body, touchEvent.pointerId)
        when (touchEvent.type) {
            TouchType.DOWN -> {
                getOrPutJoint(key, touchEvent, dragConfig)
            }

            TouchType.MOVE -> {
                getOrPutJoint(key, touchEvent, dragConfig).apply {
                    target = body.getWorldPoint(touchEvent.offset)
                    springFrequency = dragConfig.frequency
                    springDampingRatio = dragConfig.dampingRatio
                    maximumSpringForce = dragConfig.maxForce
                }

            }

            TouchType.UP -> {
                joints.remove(key)?.let {
                    world.removeJoint(it as Joint<SimulationEntity<*>>)
                }
            }
        }
    }

    override fun removeBody(body: SimulationEntity.Body) {
        val iterator = joints.iterator()
        while (iterator.hasNext()) {
            val (key, joint) = iterator.next()
            if (key.body === body) {
                world.removeJoint(joint as Joint<SimulationEntity<*>>)
                iterator.remove()
            }
        }
    }

    private fun getOrPutJoint(
        jointKey: JointKey,
        touchEvent: SimulationTouchEvent,
        dragConfig: DragConfig
    ) = joints.getOrPut(jointKey) {
        PinJoint(
            jointKey.body,
            jointKey.body.getWorldPoint(touchEvent.offset),
        ).apply {
            isSpringEnabled = true
            springFrequency = dragConfig.frequency
            springDampingRatio = dragConfig.dampingRatio
            maximumSpringForce = dragConfig.maxForce
        }.also {
            world.addJoint(it as Joint<SimulationEntity<*>>)
        }
    }
}

private data class JointKey(
    val body: SimulationEntity.Body,
    val pointerId: Long,
)
