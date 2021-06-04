package com.example.plusequaltoview

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.Color
import android.app.Activity
import android.content.Context

val parts : Int = 4
val scGap : Float = 0.02f / parts
val delay : Long = 20
val backColor : Int = Color.parseColor("#BDBDBD")
val colors : Array<Int> = arrayOf(
    "#f44336",
    "#673AB7",
    "#00C853",
    "#304FFE",
    "#BF360C"
).map {
    Color.parseColor(it)
}.toTypedArray()
val strokeFactor : Float = 90f
val rot : Float = 90f
val gapFactor : Float = 5.2f
val sizeFactor : Float = 4.2f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawPlus(scale : Float, size : Float, paint : Paint) {
    val updatedSize : Float = size * scale.divideScale(0, parts)
    save()
    translate(0f, -size)
    for (j in 0..1) {
        save()
        rotate(rot * j * scale.divideScale(1, parts))
        drawLine(-updatedSize / 2, 0f, updatedSize / 2, 0f, paint)
        restore()
    }
    restore()
}

fun Canvas.drawEqual(scale : Float, size : Float, paint : Paint) {
    val updatedSize : Float = size * scale.divideScale(0, parts)
    val gap : Float = size / gapFactor
    save()
    translate(0f, size)
    for (j in 0..1) {
        save()
        translate(0f, (gap / 2) * (1f  - 2 * j))
        restore()
    }
    restore()
}

fun Canvas.drawPlusEqualTo(scale : Float, w : Float, h : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / sizeFactor
    val sf : Float = scale.sinify()
    save()
    translate(w / 2,  h / 2)
    drawPlus(sf, size, paint)
    drawEqual(sf, size, paint)
    restore()
}

fun Canvas.drawPETNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawPlusEqualTo(scale, w, h, paint)
}

class PlusEqualToView(ctx : Context) :View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class PETNode(var i : Int, val state : State = State()) {

        private var next : PETNode? = null
        private var prev : PETNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = PETNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawPETNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : PETNode {
            var curr : PETNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class PlusEqualTo(var i : Int) {

        private var curr : PETNode = PETNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : PlusEqualToView) {

        private val pet : PlusEqualTo = PlusEqualTo(0)
        private val animator : Animator = Animator(view)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            pet.draw(canvas, paint)
            animator.animate {
                pet.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            pet.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : PlusEqualToView {
            val view : PlusEqualToView = PlusEqualToView(activity)
            activity.setContentView(view)
            return view
        }
    }
}