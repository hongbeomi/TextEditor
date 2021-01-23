package github.hongbeomi.texteditor

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem

class OnSelectedCallback : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = true
    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.clear()
        return true
    }
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
    override fun onDestroyActionMode(mode: ActionMode?) {}
}