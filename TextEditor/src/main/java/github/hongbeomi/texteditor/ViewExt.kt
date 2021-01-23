package github.hongbeomi.texteditor

import android.text.Spannable
import android.view.View
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

fun TextView.setSelectionChangedListener(
    onSelectionChangedListener: (Spannable, Int, Int) -> Unit,
    onUnSelectionChangedListener: () -> Unit
) {
    accessibilityDelegate = object : View.AccessibilityDelegate() {
        override fun sendAccessibilityEvent(host: View?, eventType: Int) {
            super.sendAccessibilityEvent(host, eventType)
            if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                when(this@setSelectionChangedListener.hasSelection()) {
                    true -> onSelectionChangedListener.invoke(text as Spannable, selectionStart, selectionEnd)
                    false -> onUnSelectionChangedListener.invoke()
                }

            }
        }
    }
}
