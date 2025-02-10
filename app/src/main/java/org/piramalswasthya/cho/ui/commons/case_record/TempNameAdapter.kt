package org.piramalswasthya.cho.ui.commons.case_record

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import org.piramalswasthya.cho.model.PrescriptionTemplateDB

class TempNameAdapter(
                      context: Context,
                      resource: Int,
                      private val dataList: List<PrescriptionTemplateDB?>,
                      autoCompleteTextView: AutoCompleteTextView
) : ArrayAdapter<PrescriptionTemplateDB>(context, resource, dataList) {


    init {
        // Set the custom filter to the AutoCompleteTextView
        autoCompleteTextView.setAdapter(this)
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val formData = dataList[position]
        (view as? TextView)?.text = formData?.templateName
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val formData = dataList[position]
        (view as? TextView)?.text = formData?.templateName
        return view
    }
}
