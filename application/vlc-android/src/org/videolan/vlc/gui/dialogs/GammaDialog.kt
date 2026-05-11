/*
 * **************************************************************************
 *  GammaDialog.kt — gamma adjustment behind the main overflow menu.
 *  UX modeled after PlaybackSpeedDialog.
 * **************************************************************************
 */
package org.videolan.vlc.gui.dialogs

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.videolan.resources.VLCInstance
import org.videolan.tools.KEY_GAMMA_GLOBAL_VALUE
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.gui.helpers.restartMediaPlayer
import java.util.Locale

class GammaDialog : VLCBottomSheetDialogFragment() {

    private lateinit var settings: SharedPreferences
    private lateinit var seekBar: SeekBar
    private lateinit var valueLabel: TextView
    private var initialGamma: Float = 1.0f
    private var currentGamma: Float = 1.0f

    override fun getDefaultState(): Int = BottomSheetBehavior.STATE_EXPANDED
    override fun needToManageOrientation(): Boolean = true
    override fun initialFocusedView(): View = seekBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.dialog_gamma, container, false)
        settings = Settings.getInstance(requireActivity())
        initialGamma = settings.getFloat(KEY_GAMMA_GLOBAL_VALUE, 1.0f)
        currentGamma = initialGamma

        seekBar = root.findViewById(R.id.gamma_seek)
        valueLabel = root.findViewById(R.id.gamma_value)

        seekBar.progress = gammaToProgress(currentGamma)
        renderValue()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentGamma = progressToGamma(progress)
                    renderValue()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        root.findViewById<View>(R.id.button_gamma_minus).setOnClickListener { nudge(-0.01f) }
        root.findViewById<View>(R.id.button_gamma_plus).setOnClickListener { nudge(+0.01f) }
        valueLabel.setOnClickListener { setGamma(1.0f) }
        root.findViewById<View>(R.id.button_gamma_07).setOnClickListener { setGamma(0.7f) }
        root.findViewById<View>(R.id.button_gamma_1).setOnClickListener { setGamma(1.0f) }
        root.findViewById<View>(R.id.button_gamma_13).setOnClickListener { setGamma(1.3f) }
        root.findViewById<View>(R.id.button_gamma_16).setOnClickListener { setGamma(1.6f) }

        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(true)
        return root
    }

    private fun nudge(delta: Float) {
        setGamma((currentGamma + delta).coerceIn(0.50f, 2.00f))
    }

    private fun setGamma(value: Float) {
        currentGamma = value.coerceIn(0.50f, 2.00f)
        seekBar.progress = gammaToProgress(currentGamma)
        renderValue()
    }

    private fun renderValue() {
        valueLabel.text = String.format(Locale.US, "%.2f", currentGamma)
    }

    private fun gammaToProgress(g: Float): Int = ((g - 0.50f) * 100f).toInt().coerceIn(0, 150)
    private fun progressToGamma(p: Int): Float = 0.50f + p.toFloat() / 100f

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (currentGamma != initialGamma) {
            settings.edit(commit = true) { putFloat(KEY_GAMMA_GLOBAL_VALUE, currentGamma) }
            lifecycleScope.launch {
                VLCInstance.restart()
                restartMediaPlayer()
            }
        }
    }

    companion object {
        const val TAG = "VLC/GammaDialog"
        fun newInstance(): GammaDialog = GammaDialog()
    }
}
