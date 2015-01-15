package org.wordpress.android.ui.caredear;

import android.database.Cursor;
import android.util.Log;

import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CaredearUtils {
    public static String getCategoryNameById(int categoryId){
        return CATEGORIY_NAME_CHN[categoryId];
    }

    public interface CATEGORY_ID_ON_SERVER {
        public final static int XWBG = 1;
        public final static int ZSQ = 2;
        public final static int YMGX = 3;
        public final static int MSCP = 4;
        public final static int YSZD = 5;
        public final static int GCJS = 6;
        public final static int ESBK = 7;
        public final static int PSJM = 8;
        public final static int HH = 9;
        public final static int CW = 10;
        public final static int HHCW = 11;
        public final static int TWDY = 12;
    }

    //Category names for DB query, should match server side category names
    public static final String[] CATEGORIES = {
            "",
            "新闻八卦",
            "最省钱",
            "幽默搞笑",
            "美食菜谱",
            "养生之道",
            "广场健身",
            "儿孙百科",
            "骗术揭秘",
            "花卉",
            "宠物",
            "花卉宠物",
            "图文电影"
    };

    //Category names for UI title display 
    private static final String[] CATEGORIY_NAME_CHN = {
            "",
            "新闻八卦",
            "最省钱",
            "幽默搞笑",
            "美食菜谱",
            "养生之道",
            "广场健身",
            "儿孙百科",
            "防骗揭秘",
            "花卉",
            "宠物",
            "宠物花卉",
            "图文电影"
    };

}
