package com.dp.jumpster.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.dp.jumpster.R

/**
 * Material 风格图标工具类
 * 用于统一应用中的图标风格和颜色
 */
object MaterialIconUtils {

    /**
     * 获取带有主题色的图标
     * @param context 上下文
     * @param iconResId 图标资源 ID
     * @param colorResId 颜色资源 ID，默认为主题色
     * @return 着色后的 Drawable
     */
    fun getTintedIcon(
        context: Context,
        iconResId: Int,
        colorResId: Int = R.color.sport_primary
    ): Drawable? {
        val drawable = AppCompatResources.getDrawable(context, iconResId) ?: return null
        val wrappedDrawable = DrawableCompat.wrap(drawable).mutate()
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, colorResId))
        return wrappedDrawable
    }

    /**
     * 获取适应当前主题的图标
     * 在深色模式和浅色模式下自动选择合适的颜色
     * @param context 上下文
     * @param iconResId 图标资源 ID
     * @return 适应主题的 Drawable
     */
    fun getThemedIcon(context: Context, iconResId: Int): Drawable? {
        val drawable = AppCompatResources.getDrawable(context, iconResId) ?: return null
        val wrappedDrawable = DrawableCompat.wrap(drawable).mutate()
        val colorResId = R.color.sport_on_primary
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, colorResId))
        return wrappedDrawable
    }
}
