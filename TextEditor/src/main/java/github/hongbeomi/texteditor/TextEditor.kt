package github.hongbeomi.texteditor

import android.content.Context
import android.graphics.*
import android.text.Spannable
import android.text.Spanned
import android.text.style.*
import android.view.*
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.ListPopupWindow.WRAP_CONTENT
import androidx.core.content.ContextCompat
import github.hongbeomi.texteditor.databinding.ViewActionModePopupWindowBinding
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class TextEditor private constructor(private val context: Context) {

    companion object {
        const val DEFAULT_WIDTH = -1
        const val DEFAULT_HEIGHT = -1

        enum class ButtonType {
            HighLight, Bold, Italic, UnderLine, Strike
        }
    }

    private val currentLocation = Point()
    private val startLocation = Point()

    private val currentBounds = Rect()
    private val popupWindow: PopupWindow = PopupWindow(context)

    private var targetTextView: TextView? = null
    private var targetTextViewTextSize: Float = 12f
    private var span: Spannable? = null
    private var startIndex: Int? = null
    private var endIndex: Int? = null
    lateinit var binding: ViewActionModePopupWindowBinding

    @ColorRes
    private var highLightColor: Int = R.color.default_highlight

    init {
        setUpPopupWindow()
    }

    private fun setUpTextSpan(span: Spannable, startIndex: Int, endIndex: Int) {
        this.span = span
        this.startIndex = startIndex
        this.endIndex = endIndex
    }

    private fun setUpPopupWindow() {
        popupWindow.apply {
            binding = ViewActionModePopupWindowBinding.inflate(LayoutInflater.from(context))
            width = context.resources.getDimensionPixelSize(R.dimen.popup_window_width)
            height = WRAP_CONTENT
            contentView = binding.root
            animationStyle = R.style.PopupWindowAnimationBottomSlide
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_action_mode_popup_window))

            binding.apply {
                imageButtonActionModePopupWindowClear.setOnClickListener {
                    onClear()
                }
                imageButtonActionModePopupWindowHighlight.setOnClickListener {
                    onClickImageButton(ButtonType.HighLight)
                }
                imageButtonActionModePopupWindowBold.setOnClickListener {
                    onClickImageButton(ButtonType.Bold)
                }
                imageButtonActionModePopupWindowItalic.setOnClickListener {
                    onClickImageButton(ButtonType.Italic)
                }
                imageButtonActionModePopupWindowUnderline.setOnClickListener {
                    onClickImageButton(ButtonType.UnderLine)
                }
                imageButtonActionModePopupWindowStrikeThrough.setOnClickListener {
                    onClickImageButton(ButtonType.Strike)
                }
            }
        }
    }

    private fun onClickImageButton(type: ButtonType) {
        targetTextView?.post {
            val styleSpan = when (type) {
                ButtonType.HighLight -> BackgroundColorSpan(ContextCompat.getColor(context, highLightColor))
                ButtonType.Bold -> StyleSpan(Typeface.BOLD)
                ButtonType.Italic -> StyleSpan(Typeface.ITALIC)
                ButtonType.UnderLine -> UnderlineSpan()
                ButtonType.Strike -> StrikethroughSpan()
            }
            span?.setSpan(
                styleSpan,
                startIndex ?: return@post,
                endIndex ?: return@post,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun onClear() {
        val spansToRemove = span?.getSpans(
            startIndex ?: return,
            endIndex ?: return,
            CharacterStyle::class.java
        ) ?: return
        for (s in spansToRemove) {
            if (s.underlying is CharacterStyle) {
                span?.removeSpan(s)
            }
        }
    }

    private fun showPopupWindow() {
        val popupContent = popupWindow.contentView
        if (popupWindow.isShowing) {
            val (point, cX, cY) = getLocationAndCoordinate() ?: return
            val x = (cX + (point.x + cX))
            val y = (cY + (point.y - cY - (targetTextView?.scrollY ?: 0)))
            popupWindow.update(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT)
        } else {
            targetTextView?.apply {
                popupWindow.showAtLocation(this, Gravity.TOP, 0, 0)
                val cFrame = Rect()
                val cLocation = IntArray(2)
                popupContent.let {
                    it.getLocationOnScreen(cLocation)
                    it.getLocalVisibleRect(currentBounds)

                    if (currentBounds.right == 0 && currentBounds.bottom == 0) {
                        currentBounds.right = 976
                        currentBounds.bottom = 192
                    }
                    it.getWindowVisibleDisplayFrame(cFrame)

                    val scrollY = (this.parent as? View)?.scrollY ?: 0
                    val textLocation = IntArray(2)
                    this.getLocationInWindow(textLocation)

                    val startX = cLocation[0] + currentBounds.centerX()
                    val startY = cLocation[1] + currentBounds.centerY() -
                            (textLocation[1] - cFrame.top) - scrollY
                    startLocation.set(startX, startY)

                    val (point, cX, cY) = getLocationAndCoordinate() ?: return
                    updatePopupWindow(point, cX, cY)
                }
            }
        }
    }

    private fun getLocationAndCoordinate(): Triple<Point, Int, Int>? {
        val popLocation = calculatePopupLocation()
        popLocation ?: return null
        val currentX = currentLocation.x
        val currentY = currentLocation.y
        currentLocation.set(popLocation.x, popLocation.y)
        return Triple(popLocation, currentX, currentY)
    }

    private fun updatePopupWindow(point: Point, cX: Int, cY: Int) {
        val x = (cX + (point.x + cX))
        val y = (cY + (point.y - cY - (targetTextView?.scrollY ?: 0)))
        popupWindow.update(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT)
    }

    private fun calculatePopupLocation(): Point? {
        val scrollView: ScrollView? = targetTextView?.parent as? ScrollView

        val selectionStart: Int = targetTextView?.selectionStart ?: 0
        val selectionEnd: Int = targetTextView?.selectionEnd ?: 0
        val min = 0.coerceAtLeast(min(selectionStart, selectionEnd))
        val max = 0.coerceAtLeast(max(selectionStart, selectionEnd))

        val selectionBounds = RectF()
        val selection = Path()
        targetTextView?.layout?.getSelectionPath(min, max, selection)
        selection.computeBounds(selectionBounds, true)

        val centerX: Int = startLocation.x
        val centerY: Int = startLocation.y

        val popupHeight: Int = currentBounds.height()
        val textPadding: Int = targetTextView?.paddingLeft ?: 0
        val topOffset: Int =
            (selectionBounds.top - centerY - (targetTextViewTextSize + 20)).roundToInt()
        val bottomOffset: Int =
            (selectionBounds.bottom - (centerY - popupHeight) - (targetTextViewTextSize + 20) - (targetTextView?.scrollY
                ?: 0)).roundToInt()

        val scrollY: Int = scrollView?.scrollY ?: 0
        val x: Int = (selectionBounds.centerX() + textPadding - centerX).roundToInt()
        val y: Int = if (selectionBounds.top - scrollY < centerY) bottomOffset else topOffset
        currentLocation.set(x, y - scrollY)
        return currentLocation
    }

    private fun dismissPopupWindow() {
        popupWindow.dismiss()
    }

    fun setTextView(textView: TextView) {
        this.targetTextView = textView
        this.targetTextViewTextSize = textView.textSize
        this.targetTextView?.apply {
            setTextIsSelectable(true)
            customSelectionActionModeCallback = OnSelectedCallback()
            setOnScrollChangeListener { _, _, _, _, _ ->
                if (textView.hasSelection() && popupWindow.isShowing) {
                    val (point, cX, cY) = getLocationAndCoordinate()
                        ?: return@setOnScrollChangeListener
                    val x = (cX + (point.x + cX))
                    val y = (cY + (point.y - cY - (targetTextView?.scrollY ?: 0)))
                    popupWindow.update(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT)
                }
            }
            setSelectionChangedListener(
                onSelectionChangedListener = { spannable, startIndex, endIndex ->
                    // for configuration change
                    (textView.parent as View).post {
                        setUpTextSpan(spannable, startIndex, endIndex)
                        showPopupWindow()
                    }
                },
                onUnSelectionChangedListener = { dismissPopupWindow() }
            )
        }
    }

    fun setHighLightColor(@ColorRes colorRes: Int) {
        this.highLightColor = colorRes
    }

    class Builder(context: Context) {
        private val editor = TextEditor(context)

        fun setTextView(textView: TextView) = apply {
            editor.setTextView(textView)
        }

        fun setHighLightColor(@ColorRes colorRes: Int) = apply {
            editor.setHighLightColor(colorRes)
        }

        fun build() = editor
    }

}