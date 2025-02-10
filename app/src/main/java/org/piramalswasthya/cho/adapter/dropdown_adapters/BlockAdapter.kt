package org.piramalswasthya.cho.adapter.dropdown_adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.TextView
import org.piramalswasthya.cho.network.DistrictBlock


class BlockAdapter(
    context: Context,
    resource: Int,
    private val dataList: List<DistrictBlock>,
    private val autoCompleteTextView: AutoCompleteTextView
) : ArrayAdapter<DistrictBlock>(context, resource, dataList) {

    private val filterList = ArrayList<DistrictBlock>(dataList)

    init {
        // Set the custom filter to the AutoCompleteTextView
        autoCompleteTextView.setAdapter(this)
    }
    fun updateData(newData: List<DistrictBlock>) {
        filterList.clear()
        filterList.addAll(newData)
        notifyDataSetChanged()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val block = dataList[position]
        (view as? TextView)?.text = block.blockName
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val block = dataList[position]
        (view as? TextView)?.text = block.blockName
        return view
    }


}