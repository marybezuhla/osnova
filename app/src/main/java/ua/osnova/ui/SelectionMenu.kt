package ua.osnova.ui

import android.content.ClipboardManager
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import ua.osnova.R
import ua.osnova.text.Constitution
import ua.osnova.text.Page
import ua.osnova.text.Quote

/**
 * The menu over text selected in an article: "Copy", "Share" and "Select all". "Share" opens the quote's sheet
 * with the parts the selection is in already chosen, so the quote is cited by its parts.
 */
class SelectionMenu(
    private val view: View,
    private val c: Constitution,
    private val page: Page,
    /** The indexes of the parts chosen (in [Quote.pieces]), or null for the whole page */
    private val onShare: (List<Int>?) -> Unit,
) : TextToolbar {
    private var mode: ActionMode? = null
    private var rect = Rect.Zero
    private var copy: (() -> Unit)? = null
    private var selectAll: (() -> Unit)? = null

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        this.rect = rect
        copy = onCopyRequested
        selectAll = onSelectAllRequested
        val shown = mode
        if (shown == null) {
            mode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
            status = TextToolbarStatus.Shown
        } else {
            shown.invalidate()
            shown.invalidateContentRect()
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        mode?.finish()
        mode = null
    }

    private val callback = object : ActionMode.Callback2() {
        override fun onCreateActionMode(m: ActionMode, menu: Menu): Boolean = fill(menu)
        override fun onPrepareActionMode(m: ActionMode, menu: Menu): Boolean { menu.clear(); return fill(menu) }

        private fun fill(menu: Menu): Boolean {
            if (copy != null) {
                menu.add(0, COPY, 0, android.R.string.copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                menu.add(0, SHARE, 1, view.context.getString(R.string.share_send)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            if (selectAll != null) menu.add(0, SELECT_ALL, 2, android.R.string.selectAll).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            return true
        }

        override fun onActionItemClicked(m: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                COPY -> copy?.invoke()
                SHARE -> share()
                SELECT_ALL -> selectAll?.invoke()
                else -> return false
            }
            if (item.itemId != SELECT_ALL) m.finish()
            return true
        }

        override fun onDestroyActionMode(m: ActionMode) {
            mode = null
            status = TextToolbarStatus.Hidden
        }

        override fun onGetContentRect(m: ActionMode, v: View, out: android.graphics.Rect) {
            out.set(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        }
    }

    /**
     * The selection is taken the only way the text gives it, through the clipboard, and what was in the
     * clipboard before is put back: sharing does not copy anything.
     */
    private fun share() {
        val context = view.context
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val before = try { clipboard.primaryClip } catch (_: Exception) { null }
        copy?.invoke() ?: return
        val selection = try { clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() } catch (_: Exception) { null }
        try {
            if (before != null) clipboard.setPrimaryClip(before) else clipboard.clearPrimaryClip()
        } catch (_: Exception) { /* the share still goes */ }
        // Nothing of the page's own words selected (only its heading): the whole page
        val chosen = selection?.let { Quote.selected(c, page, it) }?.second
        val all = Quote.pieces(c, page)
        onShare(chosen?.map { p -> all.indexOfFirst { it.number == p.number } }?.filter { it >= 0 })
    }

    private companion object {
        const val COPY = 1
        const val SHARE = 2
        const val SELECT_ALL = 3
    }
}
