package com.blanke.mdwechat.hookers

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.TextView
import com.blanke.mdwechat.*
import com.blanke.mdwechat.WeChatHelper.defaultImageRippleDrawable
import com.blanke.mdwechat.WeChatHelper.transparentDrawable
import com.blanke.mdwechat.config.HookConfig
import com.blanke.mdwechat.hookers.base.Hooker
import com.blanke.mdwechat.hookers.base.HookerProvider
import com.blanke.mdwechat.util.NightModeUtils
import com.blanke.mdwechat.util.ViewTreeUtils
import com.blanke.mdwechat.util.ViewUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object ListViewHooker : HookerProvider {
    private val excludeContext = arrayOf("com.tencent.mm.plugin.mall.ui.MallIndexUI")

    private var headTextColor = Color.BLACK
        get() {
            return NightModeUtils.getContentTextColor()
        }

    private var titleTextColor = Color.BLACK
        get() {
            return NightModeUtils.getTitleTextColor()
        }
    private var isHookTextColor = false
        get() {
            return HookConfig.is_hook_main_textcolor || NightModeUtils.isNightMode()
        }

    override fun provideStaticHookers(): List<Hooker>? {
        return listOf(listViewHook)
    }

    private val listViewHook = Hooker {
        XposedHelpers.findAndHookMethod(AbsListView::class.java, "setSelector", Drawable::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                param?.args!![0] = transparentDrawable
            }
        })
        XposedHelpers.findAndHookMethod(AbsListView::class.java, "obtainView", CC.Int, BooleanArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val view = param?.result as View
                val context = view.context
                val tmp = excludeContext.find { context::class.java.name.contains(it) }
                if (tmp != null) {
                    return
                }
                view.background = defaultImageRippleDrawable
//                log("--------------------")
//                log("context=" + view.context)
//                LogUtil.logViewStackTraces(view)
//                log("--------------------")

                // ConversationFragment 聊天列表 item
                if (ViewTreeUtils.equals(ViewTreeRepo.ConversationListViewItem, view)) {
                    val chatNameView = ViewUtils.getChildView(view, 1, 0, 0, 0)
                    val chatTimeView = ViewUtils.getChildView(view, 1, 0, 1)
                    val recentMsgView = ViewUtils.getChildView(view, 1, 1, 0, 1)
                    val unreadCountView = ViewUtils.getChildView(view, 0, 1) as TextView
                    val unreadView = ViewUtils.getChildView(view, 0, 2) as ImageView

//                    LogUtil.log("chatNameView=$chatNameView,chatTimeView=$chatTimeView,recentMsgView=$recentMsgView")
                    if (isHookTextColor) {
                        XposedHelpers.callMethod(chatNameView, "setTextColor", titleTextColor)
                        XposedHelpers.callMethod(chatTimeView, "setTextColor", headTextColor)
                        XposedHelpers.callMethod(recentMsgView, "setTextColor", headTextColor)
                        unreadCountView.backgroundTintList = ColorStateList.valueOf(HookConfig.get_color_primary)
                        unreadView.backgroundTintList = ColorStateList.valueOf(HookConfig.get_color_primary)
                    }
                }

                // 联系人列表
                else if (ViewTreeUtils.equals(ViewTreeRepo.ContactListViewItem, view)) {
                    val headTextView = ViewUtils.getChildView(view, 0) as TextView
                    val titleView = ViewUtils.getChildView(view, 1, 0, 3)
//                    log("headTextView=$headTextView,titleView=$titleView")
                    if (isHookTextColor) {
                        headTextView.setTextColor(headTextColor)
                        XposedHelpers.callMethod(titleView, "setNickNameTextColor", ColorStateList.valueOf(titleTextColor))
                    }
                    // 修改背景
                    val contentView = ViewUtils.getChildView(view, 1) as ViewGroup
                    contentView.background = defaultImageRippleDrawable
                    val innerView = ViewUtils.getChildView(contentView, 0) as View
                    // 去掉分割线
                    innerView.background = transparentDrawable
                }

                // 发现 设置 item
                else if (ViewTreeUtils.equals(ViewTreeRepo.DiscoverViewItem, view)) {
                    val iconImageView = ViewUtils.getChildView(view, 0, 0, 0, 0) as View
                    if (iconImageView.visibility == View.VISIBLE) {
                        val titleView = ViewUtils.getChildView(view, 0, 0, 0, 1, 0, 0) as TextView
                        if (isHookTextColor) {
                            titleView.setTextColor(titleTextColor)
                        }
                    }
                }

                // 设置 头像
                else if (ViewTreeUtils.equals(ViewTreeRepo.SettingAvatarView, view)) {
                    val nickNameView = ViewUtils.getChildView(view, 0, 1, 0)
                    val wechatTextView = ViewUtils.getChildView(view, 0, 1, 1) as TextView
                    if (wechatTextView.text.startsWith("微信号") && isHookTextColor) {
                        wechatTextView.setTextColor(titleTextColor)
                        XposedHelpers.callMethod(nickNameView, "setTextColor", titleTextColor)
                    }
                }

                // 聊天消息 item
                else if (ViewTreeUtils.equals(ViewTreeRepo.ChatRightMessageItem, view)) {
                    val chatMsgRightTextColor = HookConfig.get_hook_chat_text_color_right
                    val msgView = ViewUtils.getChildView(view, 3, 1, 1, 3) as View
//                    log("msgView=$msgView")
                    XposedHelpers.callMethod(msgView, "setTextColor", chatMsgRightTextColor)
                    XposedHelpers.callMethod(msgView, "setLinkTextColor", chatMsgRightTextColor)
                    XposedHelpers.callMethod(msgView, "setHintTextColor", chatMsgRightTextColor)
//                    val mText = XposedHelpers.getObjectField(msgView, "mText")
//                    log("msg right text=$mText")
                    if (HookConfig.is_hook_bubble) {
                        val bubble = WeChatHelper.getRightBubble(msgView.resources)
                        msgView.background = bubble
                        if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                            msgView.setPadding(30, 25, 45, 25)
                        }
                    }
                } else if (ViewTreeUtils.equals(ViewTreeRepo.ChatLeftMessageItem, view)) {
                    val chatMsgLeftTextColor = HookConfig.get_hook_chat_text_color_left
                    val msgView = ViewUtils.getChildView(view, 3, 1, 1) as View
//                    log("msgView=$msgView")
                    XposedHelpers.callMethod(msgView, "setTextColor", chatMsgLeftTextColor)
                    XposedHelpers.callMethod(msgView, "setLinkTextColor", chatMsgLeftTextColor)
                    XposedHelpers.callMethod(msgView, "setHintTextColor", chatMsgLeftTextColor)
//                    val mText = XposedHelpers.getObjectField(msgView, "mText")
//                    log("msg left text=$mText")
                    if (HookConfig.is_hook_bubble) {
                        val bubble = WeChatHelper.getLeftBubble(msgView.resources)
                        msgView.background = bubble
                        if (WechatGlobal.wxVersion!! >= Version("6.7.2")) {
                            msgView.setPadding(45, 25, 30, 25)
                        }
                    }
                }

            }
        })
    }
}