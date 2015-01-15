package org.wordpress.android.ui.caredear;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import org.wordpress.caredear.R;

public class CategoryAdapter extends BaseAdapter implements CaredearUtils.CATEGORY_ID_ON_SERVER {
    private static final String TAG = "CategoryAdapter";
    private static final int NUM_OF_CATEGORY = 10;

    private class ViewHolder {
        View root;
        ImageView icn;
    }
    private Context mContext;
    //private String[] CATEGORY_NAME;
    private int[] CATEGORY_ICON_RES_ID = {
            R.drawable.cd_cate_xwbg, R.drawable.cd_cate_zsq,
            R.drawable.cd_cate_ymgx, R.drawable.cd_cate_mscp,
            R.drawable.cd_cate_yszd, R.drawable.cd_cate_gcjs,
            R.drawable.cd_cate_fpjm, R.drawable.cd_cate_cwhh,
            R.drawable.cd_cate_esbk, R.drawable.cd_cate_twdy,
    };
    private int[] CATEGORY_BG_RES_ID = {
            R.drawable.cd_bg_cate_0, R.drawable.cd_bg_cate_1,
            R.drawable.cd_bg_cate_2, R.drawable.cd_bg_cate_3,
            R.drawable.cd_bg_cate_4, R.drawable.cd_bg_cate_5,
            R.drawable.cd_bg_cate_0, R.drawable.cd_bg_cate_1,
            R.drawable.cd_bg_cate_5, R.drawable.cd_bg_cate_3,
    };

    public CategoryAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        int ret = NUM_OF_CATEGORY;
        return ret;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        long id = 0L;
        switch (position) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                id = position + 1;
                break;
            case 6:
                id = PSJM;
                break;
            case 7:
                id = HHCW;
                break;
            case 8:
                id = ESBK;
                break;
            case 9:
                id = TWDY;
                break;
        }
        return id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.cd_grid_item_category, null);
            holder = new ViewHolder();
            holder.root = convertView.findViewById(R.id.category_root_view);
            holder.icn = (ImageView) convertView.findViewById(R.id.category_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.icn.setImageResource(CATEGORY_ICON_RES_ID[position]);
        holder.root.setBackgroundResource(CATEGORY_BG_RES_ID[position]);
        return convertView;
    }



}