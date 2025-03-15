@file:UseSerializers(OffsetSerializer::class)

package com.devoid.keysync.model

import androidx.compose.ui.geometry.Offset
import com.devoid.keysync.data.serializers.OffsetSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


@Serializable
sealed class DraggableItem {

    abstract val id: Int
    abstract var position: Offset
    abstract fun copy(id: Int=this.id,position: Offset=this.position) :DraggableItem
@Serializable
    data class VariableKey(
        override val id: Int,
        override var position: Offset,
        var keyCode:Int? = null,
        var size: Int
    ) : DraggableItem(){
        override fun copy(id: Int, position: Offset): DraggableItem =
            VariableKey(id, position, keyCode, size)

    }

    @Serializable
    data class WASDGroup(
        override val id: Int,
        override var position:Offset,
        var scale: Float=1f,
        var center: Offset = Offset.Zero,
        var w: Offset = Offset.Zero,
        var a: Offset = Offset.Zero,
        var s: Offset = Offset.Zero,
        var d: Offset = Offset.Zero,
    ) : DraggableItem() {
        override fun copy(id: Int, position: Offset): DraggableItem =
            WASDGroup(id, position,scale, center, w, a, s, d)
    }

    @Serializable
    data class FixedKey(
        override val id: Int,
        override var position: Offset,
        @SerialName("itemType")
        val type: DraggableItemType,
        var keyCode: Int,
        var size: Int
    ) : DraggableItem(){
        override fun copy(id: Int, position: Offset): DraggableItem =
            FixedKey(id, position, type, keyCode, size)
    }
    @Serializable
    data class CancelableKey(
        override val id: Int,
        override var position: Offset,
        var cancelPosition :Offset,
        @SerialName("itemType")
        val type: DraggableItemType,
        var keyCode: Int?=null,
        var size: Int
    ) : DraggableItem(){
        override fun copy(id: Int, position: Offset): DraggableItem =
            CancelableKey(id, position,cancelPosition, type, keyCode, size)
    }
}

@Serializable
data class KeyMap(
    val type: KeymapType = KeymapType.DEFAULT,
    val position: Offset,
    val center: Offset? = null,
    val end: Offset? = null,
)

enum class KeymapType {
    DEFAULT,CANCELABLE
}

@Serializable
enum class DraggableItemType {
    //do not change positions of existing items
    KEY, WASD_KEY, SHOOTING_MODE, FIRE, BAG_MAP , SCOPE
}
