package com.blanke.mdwechat.hookers

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.blanke.mdwechat.Classes.ActionBarContainer
import com.blanke.mdwechat.WeChatHelper.colorPrimaryDrawable
import com.blanke.mdwechat.hookers.base.Hooker
import com.blanke.mdwechat.hookers.base.HookerProvider
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod

object ActionBarHooker : HookerProvider {

    override fun provideStaticHookers(): List<Hooker>? {
        return listOf(actionBarHooker)
    }

    private val actionBarHooker = Hooker {
        findAndHookMethod(ActionBarContainer, "setPrimaryBackground", Drawable::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val drawable = param.args[0]
                var needHook = true
                if (drawable is ColorDrawable) {
                    if (drawable.color == Color.parseColor("#F2F2F2")
                            || drawable.color == Color.TRANSPARENT) {
                        needHook = false
                    }
                }
                if (needHook) {
                    param.args[0] = colorPrimaryDrawable
                }
                val actionBar = param.thisObject as ViewGroup
                actionBar.elevation = 5F
            }
        })
    }
}